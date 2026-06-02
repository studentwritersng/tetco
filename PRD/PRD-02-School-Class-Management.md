# PRD 02 — School & Class Management

**Project:** Teacher's Companion Android App  
**Version:** 1.0  
**Status:** Draft  
**Depends on:** PRD 01 — Foundation & Architecture

---

## 1. Overview

This PRD covers the full lifecycle of managing schools, classes, and subjects within the Teacher's Companion app. A teacher can belong to multiple schools simultaneously. Under each school, they assign classes from the Nigerian class standard and add subjects to each class. All data is fully isolated per teacher via RLS.

---

## 2. Goals

- Allow teachers to create and manage multiple school profiles
- Allow teachers to assign classes (from Nigerian standard) to each school
- Allow teachers to add subjects under each class per school
- Support switching between schools with context preserved
- Ensure clean UI for navigating the school → class → subject hierarchy

---

## 3. User Stories

| ID | As a teacher I want to... | So that... |
|---|---|---|
| US-01 | Add a new school with a name and optional address | I can represent the different schools I teach in |
| US-02 | Set one school as my active/primary school | My dashboard defaults to that school's context |
| US-03 | Add a class to a school by selecting from the Nigerian class list | I can organise my teaching per class level |
| US-04 | Add subjects under a class | I can track separate syllabuses per subject |
| US-05 | Edit or delete a school, class, or subject | I can keep my data accurate |
| US-06 | See a summary of how many classes and subjects each school has | I get a quick overview without drilling in |
| US-07 | See which subjects have incomplete syllabuses | I know where I still have work to do |

---

## 4. Screens & UX Flow

### 4.1 School List Screen

**Route:** `schools/list`

**Layout:**
- Top app bar: "My Schools" + FAB to add new school
- Each school shown as a card containing:
  - School name (bold)
  - Address (muted, optional)
  - Stats row: `X classes · Y subjects`
  - "Active" badge if this is the primary school
- Empty state: illustration + "Add your first school" CTA

**Actions:**
- Tap card → navigate to School Detail
- Long press card → show context menu (Edit, Set as Active, Delete)
- FAB → navigate to Add School screen

---

### 4.2 Add / Edit School Screen

**Route:** `schools/add` or `schools/{id}/edit`

**Fields:**

| Field | Type | Required | Validation |
|---|---|---|---|
| School Name | Text input | Yes | 2–100 characters |
| Address | Text input | No | Max 200 characters |
| School Logo | Image picker | No | Max 2MB, JPEG/PNG |

**Behaviour:**
- On save: upsert to `schools` table with `teacher_id = auth.uid()`
- Logo uploaded to Supabase Storage bucket `avatars/{teacher_id}/schools/{school_id}`
- On success: pop back to School List with a success snackbar
- Delete option available only on Edit screen (requires confirmation dialog)
- Deletion sets `deleted_at` (soft delete); cascades to related `school_classes` and `subjects`

---

### 4.3 School Detail Screen

**Route:** `schools/{id}`

**Layout:**
- Top app bar: school name + Edit icon
- School banner (logo if available, else gradient placeholder)
- Summary chips: `X Classes`, `Y Subjects`, `Z Topics`
- Section: "Classes" — list of assigned class levels
- Each class row shows: class name + number of subjects + chevron
- FAB to add a new class

**Actions:**
- Tap class row → navigate to Class Detail
- FAB → open Add Class bottom sheet

---

### 4.4 Add Class Bottom Sheet

**Trigger:** FAB on School Detail screen

**Layout:**
- Title: "Add Class"
- Dropdown grouped by category:

```
── Primary ──────────────
  Primary 1
  Primary 2
  Primary 3
  Primary 4
  Primary 5
  Primary 6
── Junior Secondary ─────
  JSS 1
  JSS 2
  JSS 3
── Senior Secondary ─────
  SSS 1
  SSS 2
  SSS 3
```

- Already-added classes are shown greyed out and non-selectable
- "Add" button (disabled until a valid unassigned class is selected)

**Behaviour:**
- On Add: insert into `school_classes` with `school_id` and `teacher_id`
- On success: sheet dismisses, class appears in School Detail list
- Duplicate prevention: enforced by `UNIQUE (school_id, class_level_id)` constraint

---

### 4.5 Class Detail Screen

**Route:** `schools/{school_id}/classes/{class_id}`

**Layout:**
- Top app bar: class name (e.g., "JSS 2") + school name subtitle
- Subject list — each subject shown as a card:
  - Subject name
  - Number of syllabus topics
  - Completion indicator: topics with lesson notes vs total topics (e.g., `4/10 topics covered`)
  - Warning icon if any topic has no lesson note
- Empty state: "No subjects yet. Add your first subject."
- FAB to add a subject

**Actions:**
- Tap subject card → navigate to Subject Detail (covered in PRD 03)
- FAB → open Add/Edit Subject screen
- Long press subject → Edit / Delete context menu

---

### 4.6 Add / Edit Subject Screen

**Route:** `schools/{school_id}/classes/{class_id}/subjects/add` or `.../subjects/{id}/edit`

**Fields:**

| Field | Type | Required | Validation |
|---|---|---|---|
| Subject Name | Text input | Yes | 2–80 characters |

**Behaviour:**
- On save: upsert to `subjects` table with `school_class_id` and `teacher_id`
- Duplicate subject names in the same class/school blocked by DB unique constraint
- On success: pop back to Class Detail
- Delete on Edit screen: soft deletes subject and all child syllabus topics and lesson notes via cascade

---

## 5. Data Model (Reference from PRD 01)

```
profiles (teacher)
  └── schools
        └── school_classes (links school to class_level)
              └── subjects
                    └── syllabus_topics
                          └── lesson_notes
```

### 5.1 Key Queries

**Get all schools for current teacher:**
```sql
SELECT * FROM schools
WHERE teacher_id = auth.uid()
  AND deleted_at IS NULL
ORDER BY created_at DESC;
```

**Get classes for a school with subject counts:**
```sql
SELECT
  sc.id,
  cl.name AS class_name,
  cl.category,
  cl.display_order,
  COUNT(s.id) FILTER (WHERE s.deleted_at IS NULL) AS subject_count
FROM school_classes sc
JOIN class_levels cl ON sc.class_level_id = cl.id
LEFT JOIN subjects s ON s.school_class_id = sc.id
WHERE sc.school_id = :school_id
  AND sc.teacher_id = auth.uid()
  AND sc.deleted_at IS NULL
GROUP BY sc.id, cl.name, cl.category, cl.display_order
ORDER BY cl.display_order;
```

**Get subjects for a class with lesson note coverage:**
```sql
SELECT
  s.id,
  s.name,
  COUNT(st.id) FILTER (WHERE st.deleted_at IS NULL) AS total_topics,
  COUNT(st.id) FILTER (WHERE st.has_lesson_note = TRUE AND st.deleted_at IS NULL) AS covered_topics
FROM subjects s
LEFT JOIN syllabus_topics st ON st.subject_id = s.id
WHERE s.school_class_id = :class_id
  AND s.teacher_id = auth.uid()
  AND s.deleted_at IS NULL
GROUP BY s.id, s.name;
```

---

## 6. Active School Context

The teacher can designate one school as their "active" school. This is stored in the `profiles` table:

```sql
ALTER TABLE profiles ADD COLUMN active_school_id UUID REFERENCES schools(id);
```

- Updated when teacher taps "Set as Active" on a school
- Used by the Home dashboard to show context-specific stats
- Cleared (set to NULL) if the active school is deleted

---

## 7. Validation & Error States

| Scenario | Handling |
|---|---|
| School name already exists for this teacher | Show inline error: "You already have a school with this name" |
| Class already added to this school | Greyed out in dropdown, not selectable |
| Subject name duplicate in same class | Show inline error: "This subject already exists in this class" |
| Network failure on save | Show retry snackbar: "Failed to save. Tap to retry." |
| Image upload failure | Save school without logo, show toast: "Logo could not be uploaded. You can add it later." |

---

## 8. Supabase Storage

**Bucket:** `school-assets` (private, authenticated access only)

**Path structure:**
```
school-assets/
  {teacher_id}/
    {school_id}/
      logo.jpg
```

**Access policy:**
```sql
CREATE POLICY "teacher_access_school_assets"
ON storage.objects FOR ALL
USING (bucket_id = 'school-assets' AND auth.uid()::TEXT = (storage.foldername(name))[1]);
```

---

## 9. Edge Cases

- A teacher with no schools sees the empty state and is prompted to add one on first launch
- If a school is deleted and it was the active school, `active_school_id` in profiles is set to NULL
- A class cannot be deleted if it has subjects with lesson notes — user must delete subjects first (or a warning dialog must confirm cascade deletion)
- Subject names are case-insensitive for duplicate checks (enforced with `LOWER()` on insert)

---

## 10. Acceptance Criteria

- [ ] Teacher can create up to 10 schools (limit enforced for all plans in v1.0)
- [ ] All 12 Nigerian class levels are available in the dropdown, grouped correctly
- [ ] Already-assigned classes are not selectable in the Add Class dropdown
- [ ] Subject coverage (lesson note ratio) is visible on each subject card
- [ ] Soft delete cascades correctly: deleting a school removes its classes and subjects from all queries
- [ ] Active school persists across app restarts
- [ ] All screens handle empty states gracefully
- [ ] No teacher can see or modify another teacher's schools, classes, or subjects

---

*Previous: PRD 01 — Foundation & Architecture*  
*Next: PRD 03 — Syllabus Management*

# PRD 03 — Syllabus Management

**Project:** Teacher's Companion Android App  
**Version:** 1.0  
**Status:** Draft  
**Depends on:** PRD 01 — Foundation & Architecture, PRD 02 — School & Class Management

---

## 1. Overview

This PRD covers how teachers add, organise, and track syllabus topics under each subject. Each topic represents a unit of teaching content scoped to a term and week. Topics track whether a lesson note exists, giving teachers visibility into coverage gaps. This PRD also defines the Lesson Note detail view as the terminal node of the school → class → subject → topic → note hierarchy.

---

## 2. Goals

- Allow teachers to create and order syllabus topics per subject, per school
- Organise topics by term (First, Second, Third) and week number
- Track lesson note coverage per topic (has_lesson_note flag)
- Provide a lesson note detail screen where notes can be written manually or AI-generated
- Surface uncovered topics clearly so teachers know where gaps exist

---

## 3. User Stories

| ID | As a teacher I want to... | So that... |
|---|---|---|
| US-01 | Add syllabus topics to a subject | I can map out what I need to teach |
| US-02 | Organise topics by term and week | I follow the Nigerian school calendar |
| US-03 | Reorder topics within a term | I can adjust the teaching sequence |
| US-04 | See which topics have lesson notes and which don't | I know where I have gaps |
| US-05 | Open a topic and write or view its lesson note | I can document my lessons |
| US-06 | See the syllabus for any school I manage independently | My schools don't interfere with each other |
| US-07 | Edit or delete a syllabus topic | I can correct mistakes or remove irrelevant content |

---

## 4. Screens & UX Flow

### 4.1 Subject Detail Screen (Syllabus View)

**Route:** `schools/{school_id}/classes/{class_id}/subjects/{subject_id}`

**Layout:**
- Top app bar: subject name + class name subtitle
- Filter tabs: `All` | `First Term` | `Second Term` | `Third Term`
- Topic list grouped by week number under the active term filter
- Each topic row shows:
  - Week badge (e.g., "Wk 3")
  - Topic title
  - Status indicator:
    - Green check icon → lesson note exists
    - Orange warning icon → no lesson note
  - Drag handle for reordering (within the same term)
- Progress bar at top: `X of Y topics have lesson notes`
- FAB to add a new topic

**Empty state (no topics):**
- Illustration + "No topics yet for this subject. Start building your syllabus."

**Empty state (filtered term has no topics):**
- "No topics for this term yet."

---

### 4.2 Add / Edit Syllabus Topic Screen

**Route:** `subjects/{subject_id}/topics/add` or `.../topics/{id}/edit`

**Fields:**

| Field | Type | Required | Validation |
|---|---|---|---|
| Topic Title | Text input | Yes | 3–150 characters |
| Term | Dropdown | Yes | First, Second, Third |
| Week Number | Number picker / dropdown | Yes | 1 – 14 |
| Display Order | Auto-assigned | No | Set to next available order in term |

**Behaviour:**
- On save: insert/update in `syllabus_topics` with `subject_id` and `teacher_id`
- `has_lesson_note` defaults to `FALSE` on creation
- `display_order` auto-assigned as `MAX(display_order) + 1` within the same subject and term
- On success: pop back to Subject Detail, newly added topic visible in correct term group
- Delete available on Edit screen only, requires confirmation dialog
- Deleting a topic soft-deletes it and its associated lesson notes and questions

---

### 4.3 Topic Detail / Lesson Note Screen

**Route:** `subjects/{subject_id}/topics/{topic_id}/note`

**Layout:**

```
┌────────────────────────────────────────┐
│ ← [Topic Title]          [Edit] [⋮]   │
│ Class Name · Subject Name · Term Wk N  │
├────────────────────────────────────────┤
│ LESSON NOTE                            │
│ ┌──────────────────────────────────┐   │
│ │ [Note content area — scrollable] │   │
│ └──────────────────────────────────┘   │
│                                        │
│ ┌──────────────────────────────────┐   │
│ │ ✨ Generate with AI              │   │  ← visible only if plan allows
│ └──────────────────────────────────┘   │
│                                        │
│ TEACHING GUIDE                         │
│ ┌──────────────────────────────────┐   │
│ │ [Guide content — collapsible]    │   │
│ └──────────────────────────────────┘   │
│                                        │
│ QUESTIONS                              │
│ [MCQ] [Essay]   ← tabs                │
│ ┌──────────────────────────────────┐   │
│ │ Question list                    │   │
│ └──────────────────────────────────┘   │
│ [Generate Questions with AI]           │  ← visible only if plan allows
└────────────────────────────────────────┘
```

**Sections:**

1. **Lesson Note** — Rich text area for writing or displaying the lesson note. Supports basic formatting (bold, italic, bullet lists). Manual edits saved automatically with a debounce of 1.5 seconds.

2. **AI Generate Button** — Calls the AI Edge Function to generate a lesson note from the topic title and subject context. Covered in detail in PRD 05. Hidden for Basic plan users.

3. **Teaching Guide** — Collapsible section showing the AI-generated teaching guide. A "Generate Teaching Guide" button triggers the Edge Function. Hidden for Basic plan users.

4. **Questions** — Tab view showing MCQ and Essay questions. A "Generate Questions" button at the bottom triggers generation. Hidden for Basic plan users.

**Overflow menu (⋮):**
- Edit topic details
- Delete topic
- Share lesson note (export as plain text)

---

### 4.4 Edit Lesson Note Screen

**Route:** `topics/{topic_id}/note/edit`

**Layout:**
- Full-screen text editor
- Toolbar with formatting options: Bold | Italic | Bullet list | Numbered list
- Auto-save with visual indicator ("Saved" / "Saving...")
- On back: confirm discard if unsaved changes exist

**Behaviour:**
- Content saved to `lesson_notes.content` (TEXT field)
- On first save: creates a new `lesson_notes` row and sets `syllabus_topics.has_lesson_note = TRUE`
- Subsequent saves: update the existing `lesson_notes` row
- If content is cleared entirely and saved: `has_lesson_note` is set back to `FALSE`

---

## 5. Syllabus Coverage Tracking

### 5.1 has_lesson_note Flag

The `has_lesson_note` boolean on `syllabus_topics` is the source of truth for coverage tracking. It is:

- Set to `TRUE` when a lesson note row is created for the topic
- Set back to `FALSE` if the lesson note content is deleted or the lesson note row is soft-deleted
- Updated via a Supabase database trigger:

```sql
CREATE OR REPLACE FUNCTION sync_has_lesson_note()
RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND NEW.deleted_at IS NULL) THEN
    UPDATE syllabus_topics
    SET has_lesson_note = TRUE
    WHERE id = NEW.syllabus_topic_id;
  ELSIF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE' AND NEW.deleted_at IS NOT NULL) THEN
    UPDATE syllabus_topics
    SET has_lesson_note = FALSE
    WHERE id = NEW.syllabus_topic_id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER sync_lesson_note_flag
AFTER INSERT OR UPDATE OR DELETE ON lesson_notes
FOR EACH ROW EXECUTE FUNCTION sync_has_lesson_note();
```

### 5.2 Coverage Summary Query

Used on the Subject Detail screen progress bar:

```sql
SELECT
  COUNT(*) FILTER (WHERE has_lesson_note = TRUE AND deleted_at IS NULL) AS covered,
  COUNT(*) FILTER (WHERE deleted_at IS NULL) AS total
FROM syllabus_topics
WHERE subject_id = :subject_id;
```

### 5.3 Gap Detection Query

Used by the Alarm system (PRD 04) to find topics with no lesson notes:

```sql
SELECT
  st.id,
  st.title,
  st.term,
  st.week_number,
  s.name AS subject_name,
  cl.name AS class_name,
  sc.name AS school_name
FROM syllabus_topics st
JOIN subjects s ON st.subject_id = s.id
JOIN school_classes scc ON s.school_class_id = scc.id
JOIN class_levels cl ON scc.class_level_id = cl.id
JOIN schools sc ON scc.school_id = sc.id
WHERE st.teacher_id = auth.uid()
  AND st.has_lesson_note = FALSE
  AND st.deleted_at IS NULL
  AND s.deleted_at IS NULL
ORDER BY sc.name, cl.display_order, st.term, st.week_number;
```

---

## 6. Topic Ordering & Reordering

- Topics are sorted by `display_order` within each term group
- Teacher can drag-and-drop to reorder topics within the same term
- On drag-and-drop completion, a batch update is sent to Supabase:

```sql
UPDATE syllabus_topics
SET display_order = :new_order
WHERE id = :topic_id AND teacher_id = auth.uid();
```

- Reordering across terms is not supported — to move a topic to another term, teacher must edit the topic and change its term field

---

## 7. Data Model

```sql
-- Reference from PRD 01
syllabus_topics (
  id UUID,
  subject_id UUID → subjects.id,
  teacher_id UUID → auth.users.id,
  title TEXT,
  term TEXT ('First' | 'Second' | 'Third'),
  week_number INT (1–14),
  display_order INT,
  has_lesson_note BOOLEAN,
  created_at, updated_at, deleted_at
)

lesson_notes (
  id UUID,
  syllabus_topic_id UUID → syllabus_topics.id,
  teacher_id UUID → auth.users.id,
  content TEXT,
  ai_generated BOOLEAN,
  teaching_guide TEXT,
  created_at, updated_at, deleted_at
)
```

---

## 8. RLS Policies

```sql
-- syllabus_topics
ALTER TABLE syllabus_topics ENABLE ROW LEVEL SECURITY;

CREATE POLICY "teacher_select_topics" ON syllabus_topics FOR SELECT
USING (teacher_id = auth.uid() AND deleted_at IS NULL);

CREATE POLICY "teacher_insert_topics" ON syllabus_topics FOR INSERT
WITH CHECK (teacher_id = auth.uid());

CREATE POLICY "teacher_update_topics" ON syllabus_topics FOR UPDATE
USING (teacher_id = auth.uid());

-- lesson_notes
ALTER TABLE lesson_notes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "teacher_select_notes" ON lesson_notes FOR SELECT
USING (teacher_id = auth.uid() AND deleted_at IS NULL);

CREATE POLICY "teacher_insert_notes" ON lesson_notes FOR INSERT
WITH CHECK (teacher_id = auth.uid());

CREATE POLICY "teacher_update_notes" ON lesson_notes FOR UPDATE
USING (teacher_id = auth.uid());
```

---

## 9. Validation & Error States

| Scenario | Handling |
|---|---|
| Topic title is empty | Inline error: "Please enter a topic title" |
| Week number out of range | Inline error: "Week must be between 1 and 14" |
| Auto-save fails | Persistent error bar: "Could not save. Check your connection." with retry button |
| Lesson note content exceeds storage limit | Toast: "Note is too long. Please reduce the content." (limit: 50,000 characters) |
| Topic deleted while lesson note is open | Pop back to Subject Detail, show snackbar: "This topic was deleted." |

---

## 10. Export (v1.0 Scope)

- Lesson notes can be shared as plain text via Android's native share sheet
- No PDF or formatted export in v1.0
- Export formats (PDF, Word) are planned for v1.1

---

## 11. Acceptance Criteria

- [ ] Topics can be added, edited, and soft-deleted under any subject
- [ ] Term and week number fields are always required and validated
- [ ] Topic list is grouped by term with week numbers visible
- [ ] Progress bar shows correct lesson note coverage ratio
- [ ] `has_lesson_note` updates automatically when a lesson note is created or deleted
- [ ] Gap detection query returns only the current teacher's uncovered topics
- [ ] Drag-and-drop reordering works within a term group
- [ ] Lesson note auto-saves with a visible "Saved" indicator
- [ ] Lesson notes from different schools do not intermix
- [ ] All screens gracefully handle empty states

---

*Previous: PRD 02 — School & Class Management*  
*Next: PRD 04 — Alarm & Notification System*

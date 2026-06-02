# PRD 05 — AI Features

**Project:** Teacher's Companion Android App
**Version:** 1.0
**Status:** Draft
**Depends on:** PRD 01 — Foundation & Architecture, PRD 03 — Syllabus Management, PRD 06 — Plans, Pricing & Subscription

---

## 1. Overview

This PRD covers all AI-powered features in the Teacher's Companion app. AI capabilities are gated by the teacher's subscription plan (defined in PRD 06) and delivered via Supabase Edge Functions that call the Anthropic Claude API. The three AI features are: lesson note generation from a syllabus topic, MCQ and essay question generation from an existing lesson note, and an extended teaching guide generation per lesson note.

---

## 2. Goals

- Generate high-quality, Nigeria-curriculum-aligned lesson notes from a topic title and subject context
- Generate MCQ and essay questions from a lesson note for teacher use in assessments
- Generate a detailed teaching guide for each lesson note (pedagogical strategy, resources, timing)
- Track and enforce per-teacher AI usage quotas based on subscription plan
- Provide clear feedback when quota is reached and prompt upgrade
- Ensure all AI calls pass through server-side Edge Functions (no API key exposure on device)

---

## 3. User Stories

| ID | As a teacher I want to... | So that... |
|---|---|---|
| US-01 | Generate a lesson note for a syllabus topic with one tap | I save time on lesson preparation |
| US-02 | Edit the AI-generated lesson note before saving | I can personalise the content |
| US-03 | Generate MCQ questions from my lesson note | I have ready-made assessment material |
| US-04 | Generate essay questions from my lesson note | I can set longer-form assessments |
| US-05 | Generate a teaching guide for my lesson note | I get pedagogical strategies and timing advice |
| US-06 | See how many AI generations I have left this month | I can manage my quota |
| US-07 | Be prompted to upgrade when my quota runs out | I can continue using AI features |
| US-08 | Regenerate AI content if I am not satisfied | I can get a different output |

---

## 4. AI Features Detail

### 4.1 Lesson Note Generation

**Trigger:** "✨ Generate with AI" button on the Topic Detail / Lesson Note screen (PRD 03, Section 4.3). Hidden for Basic plan users.

**Input context sent to Edge Function:**
- `topic_title` — the syllabus topic title (e.g., "Photosynthesis")
- `subject_name` — (e.g., "Biology")
- `class_name` — (e.g., "JSS 2")
- `term` — (e.g., "Second Term")
- `week_number` — (e.g., 5)
- `school_name` — optional, for context

**Prompt template (server-side):**

```
You are an experienced Nigerian secondary school teacher writing a lesson note for the Nigerian curriculum.

Subject: {subject_name}
Class: {class_name}
Term: {term}
Week: {week_number}
Topic: {topic_title}

Write a complete, well-structured lesson note that includes:
1. Topic / Subject
2. Class and Term
3. Duration (40 minutes)
4. Learning Objectives (3–5 bullet points)
5. Previous Knowledge (1–2 sentences)
6. Instructional Materials
7. Introduction (engage students, 2–3 sentences)
8. Lesson Development (step-by-step explanation, examples)
9. Evaluation (2–3 questions to check understanding)
10. Conclusion / Summary
11. Assignment

Write in clear, simple English appropriate for {class_name} students. Do not include markdown headers — use plain section labels only.
```

**Behaviour:**
- Loading state shown on button with spinner; note area disabled during generation
- On success: content populates the lesson note text area; `ai_generated = TRUE` set on the `lesson_notes` row
- If a lesson note already exists: show confirmation dialog — "Replace existing note with AI-generated content?"
- Teacher can edit the generated content; edits are saved normally per PRD 03 auto-save logic
- "Regenerate" option available in overflow menu after first generation

**Usage tracking:** Increments `ai_usage.lesson_notes_generated` for the teacher this month.

---

### 4.2 Question Generation (MCQ & Essay)

**Trigger:** "Generate Questions with AI" button on the Questions section of the Topic Detail screen (PRD 03, Section 4.3). Hidden for Basic plan users.

**Tabs:** MCQ | Essay (user selects which type to generate before tapping the button)

#### MCQ Generation

**Input context:**
- `lesson_note_content` — the full lesson note text
- `topic_title`, `subject_name`, `class_name`
- `question_count` — default 5 (Advanced), up to 10 (Premium)

**Prompt template (server-side):**

```
You are a Nigerian school teacher creating a multiple choice quiz.

Subject: {subject_name}
Class: {class_name}
Topic: {topic_title}

Based on the lesson note below, generate {question_count} multiple choice questions.
Each question must have exactly 4 options (A, B, C, D) with one clearly correct answer.

Lesson Note:
{lesson_note_content}

Return your response as a JSON array with this structure:
[
  {
    "question": "...",
    "options": { "A": "...", "B": "...", "C": "...", "D": "..." },
    "answer": "A"
  }
]
Return only the JSON array, no other text.
```

**Response handling:** Parse JSON array; render each item as a question card in the MCQ tab.

#### Essay Generation

**Input context:** Same as MCQ plus `question_count` (default 3 for Advanced, up to 5 for Premium).

**Prompt template (server-side):**

```
You are a Nigerian school teacher creating essay questions.

Subject: {subject_name}
Class: {class_name}
Topic: {topic_title}

Based on the lesson note below, generate {question_count} essay questions suitable for {class_name} students.
Questions should require critical thinking and understanding, not just recall.

Lesson Note:
{lesson_note_content}

Return your response as a JSON array:
[
  {
    "question": "...",
    "marks": 10,
    "guide": "Key points the answer should cover..."
  }
]
Return only the JSON array, no other text.
```

**Behaviour:**
- Questions stored in `questions` table linked to `lesson_note_id`
- Teacher can delete individual questions or regenerate the entire set
- Questions are visible in the MCQ / Essay tabs on the Topic Detail screen

**Usage tracking:** Increments `ai_usage.questions_generated` for the teacher this month.

---

### 4.3 Teaching Guide Generation

**Trigger:** "Generate Teaching Guide" button in the Teaching Guide section of the Topic Detail screen (PRD 03, Section 4.3). Hidden for Basic plan users.

**Input context:**
- `lesson_note_content`
- `topic_title`, `subject_name`, `class_name`, `term`, `week_number`

**Prompt template (server-side):**

```
You are a master Nigerian school teacher and pedagogy expert.

Subject: {subject_name}
Class: {class_name}
Topic: {topic_title}

Based on the lesson note below, write a detailed teaching guide for this lesson. Include:
1. Lesson Timing Breakdown (how to spend each 10-minute block of a 40-minute period)
2. Pedagogical Strategy (which teaching method is best and why)
3. Differentiation Tips (how to support weaker students and challenge stronger ones)
4. Likely Student Misconceptions (and how to address them)
5. Discussion Questions (to spark student engagement)
6. Suggested Resources (textbooks, diagrams, real-world examples)
7. Assessment Strategy (how to check understanding during the lesson)

Lesson Note:
{lesson_note_content}

Write in flowing prose paragraphs. Be practical and specific to the Nigerian classroom context.
```

**Behaviour:**
- Teaching guide stored in `lesson_notes.teaching_guide` (TEXT column)
- Displayed in the collapsible Teaching Guide section
- "Regenerate" option available

**Usage tracking:** Increments `ai_usage.teaching_guides_generated` for the teacher this month.

---

## 5. Quota Enforcement

### 5.1 Plan Limits (Defaults — configurable from Admin Panel)

| Feature | Basic | Advanced | Premium |
|---|---|---|---|
| Lesson Note Generation | 0 | 20 / month | Unlimited |
| MCQ Generation | 0 | 15 / month | Unlimited |
| Essay Generation | 0 | 15 / month | Unlimited |
| Teaching Guide Generation | 0 | 10 / month | Unlimited |
| MCQ count per generation | — | 5 | 10 |
| Essay count per generation | — | 3 | 5 |

### 5.2 Quota Check Flow

Before every AI Edge Function call:

```typescript
// Edge Function: check-quota
const usage = await supabase
  .from('ai_usage')
  .select('*')
  .eq('teacher_id', teacher_id)
  .eq('month', currentMonth())
  .single();

const plan = await getPlanLimits(teacher.plan_id);

if (usage[feature] >= plan.limits[feature]) {
  return { error: 'QUOTA_EXCEEDED', limit: plan.limits[feature] };
}
```

### 5.3 Quota Exceeded UX

- Button shows "Quota reached" state (disabled, warning colour)
- Tapping shows a bottom sheet:
  - "You've used all your {X} {feature} generations this month."
  - Resets on: {1st of next month}
  - CTA: "Upgrade Plan" → navigates to Plan Selection screen (PRD 06)

### 5.4 Usage Counter Update

After successful generation, increment usage atomically:

```sql
INSERT INTO ai_usage (teacher_id, month, {feature_column})
VALUES (:teacher_id, :month, 1)
ON CONFLICT (teacher_id, month)
DO UPDATE SET {feature_column} = ai_usage.{feature_column} + 1,
              updated_at = now();
```

---

## 6. Edge Function Architecture

All AI calls go through Supabase Edge Functions. No Anthropic API key is exposed to the Android client.

### 6.1 Function: `generate-lesson-note`

**Endpoint:** `POST /functions/v1/generate-lesson-note`

**Auth:** Supabase JWT (teacher's session token)

**Request body:**
```json
{
  "topic_id": "uuid",
  "subject_name": "Biology",
  "class_name": "JSS 2",
  "term": "Second",
  "week_number": 5,
  "topic_title": "Photosynthesis"
}
```

**Response:**
```json
{
  "content": "...",
  "tokens_used": 820
}
```

### 6.2 Function: `generate-questions`

**Endpoint:** `POST /functions/v1/generate-questions`

**Request body:**
```json
{
  "lesson_note_id": "uuid",
  "type": "mcq" | "essay",
  "question_count": 5
}
```

**Response:**
```json
{
  "questions": [ ... ]
}
```

### 6.3 Function: `generate-teaching-guide`

**Endpoint:** `POST /functions/v1/generate-teaching-guide`

**Request body:**
```json
{
  "lesson_note_id": "uuid"
}
```

**Response:**
```json
{
  "teaching_guide": "..."
}
```

### 6.4 Anthropic API Configuration

```typescript
// Shared config used by all Edge Functions
const anthropic = new Anthropic({
  apiKey: Deno.env.get('ANTHROPIC_API_KEY'),
});

const response = await anthropic.messages.create({
  model: 'claude-opus-4-6',
  max_tokens: 2048,
  messages: [{ role: 'user', content: prompt }]
});
```

- Model: `claude-opus-4-6` (balances quality and cost)
- Temperature: not set (uses default — appropriate for instructional content)
- Timeout: 30 seconds; if exceeded, return `{ error: 'TIMEOUT' }` and surface retry in app

---

## 7. Data Model

```sql
-- Questions table
CREATE TABLE questions (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  lesson_note_id   UUID REFERENCES lesson_notes(id),
  teacher_id       UUID REFERENCES auth.users(id),
  type             TEXT CHECK (type IN ('mcq', 'essay')),
  question_text    TEXT NOT NULL,
  options          JSONB,         -- MCQ only: { "A": "...", "B": "...", ... }
  correct_answer   TEXT,          -- MCQ only: "A" | "B" | "C" | "D"
  answer_guide     TEXT,          -- Essay only: marking guide
  marks            INT,           -- Essay only
  display_order    INT,
  created_at       TIMESTAMPTZ DEFAULT now(),
  updated_at       TIMESTAMPTZ DEFAULT now(),
  deleted_at       TIMESTAMPTZ
);

-- AI usage tracking table
CREATE TABLE ai_usage (
  id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id                  UUID REFERENCES auth.users(id),
  month                       TEXT NOT NULL, -- 'YYYY-MM'
  lesson_notes_generated      INT DEFAULT 0,
  questions_generated         INT DEFAULT 0,
  teaching_guides_generated   INT DEFAULT 0,
  updated_at                  TIMESTAMPTZ DEFAULT now(),
  UNIQUE (teacher_id, month)
);
```

---

## 8. RLS Policies

```sql
ALTER TABLE questions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "teacher_manage_questions" ON questions FOR ALL
USING (teacher_id = auth.uid());

ALTER TABLE ai_usage ENABLE ROW LEVEL SECURITY;

CREATE POLICY "teacher_view_own_usage" ON ai_usage FOR SELECT
USING (teacher_id = auth.uid());

-- Edge Functions use service role to write ai_usage rows
```

---

## 9. AI Usage Widget (In-App)

Displayed on the Profile screen and on the Topic Detail screen when near quota:

```
AI Usage — May 2025
Lesson Notes    ████████░░  16/20
Questions       ███████░░░  11/15
Teaching Guides ████░░░░░░   4/10

Resets June 1 · Upgrade for more
```

- Percentage bar fills based on `ai_usage` vs plan limits
- Warning colour (orange) when usage exceeds 80%
- Error colour (red) when at 100%

---

## 10. Validation & Error States

| Scenario | Handling |
|---|---|
| Quota exceeded | Disabled button + bottom sheet with upgrade CTA |
| Basic plan user taps hidden AI button | Button not rendered; no action possible |
| Anthropic API returns error | Toast: "AI generation failed. Please try again." with retry button |
| Edge Function timeout (>30s) | Toast: "This is taking longer than expected. Try again." |
| Lesson note empty when generating questions | Inline message: "Write or generate a lesson note first." |
| JSON parse failure on question response | Log error, show: "Could not process AI response. Please retry." |
| Network unavailable | Toast: "No internet connection. AI features require connectivity." |

---

## 11. Acceptance Criteria

- [ ] Lesson note generation produces contextually accurate, Nigeria-aligned content
- [ ] Generated lesson note is editable and auto-saves per PRD 03 behaviour
- [ ] MCQ questions are rendered with 4 options and a highlighted correct answer
- [ ] Essay questions are rendered with marks and a marking guide
- [ ] Teaching guide is stored and displayed in the collapsible section
- [ ] AI buttons are hidden for Basic plan users
- [ ] Quota is correctly enforced per feature per month
- [ ] Quota resets on the 1st of each month (verified by `month` field in `ai_usage`)
- [ ] "Quota exceeded" bottom sheet includes correct remaining reset date
- [ ] No Anthropic API key is present in any Android client code or APK

---

*Previous: PRD 04 — Alarm & Notification System*
*Next: PRD 06 — Plans, Pricing & Subscription*

# PRD 01 — Foundation & Architecture

**Project:** Teacher's Companion Android App  
**Version:** 1.0  
**Status:** Draft  
**Backend:** Supabase (PostgreSQL + Auth + Storage + Edge Functions + Realtime)

---

## 1. Overview

Teacher's Companion is an Android application that helps Nigerian teachers manage multiple schools, classes, syllabuses, lesson notes, and AI-assisted teaching workflows. This PRD defines the foundational architecture — database schema, authentication, security policies, navigation structure, and the core data model — that every other PRD builds upon.

---

## 2. Goals

- Establish a scalable, multi-tenant Supabase backend where each teacher's data is fully isolated
- Define the Nigerian education class hierarchy as a reusable system-level reference
- Set up Row Level Security (RLS) so no teacher can access another teacher's data
- Define the app's top-level navigation and screen architecture
- Establish shared conventions (naming, IDs, timestamps, soft deletes) used across all PRDs

---

## 3. Tech Stack

| Layer | Technology |
|---|---|
| Mobile App | Android (Kotlin + Jetpack Compose) |
| Backend | Supabase (PostgreSQL 15) |
| Auth | Supabase Auth (email/password + Google OAuth) |
| Storage | Supabase Storage (avatars, attachments) |
| Serverless | Supabase Edge Functions (Deno) |
| AI | Anthropic Claude API (via Edge Functions) |
| Push Notifications | Firebase Cloud Messaging (FCM) via Edge Functions |
| Payments | Paystack |
| Admin Panel | Separate web app (Next.js) on same Supabase project |

---

## 4. Nigerian Education Class Standard

This is a system-level reference table, seeded once and never modified by users.

### 4.1 Class Levels

| ID | Category | Class Name | Display Order |
|---|---|---|---|
| 1 | Primary | Primary 1 | 1 |
| 2 | Primary | Primary 2 | 2 |
| 3 | Primary | Primary 3 | 3 |
| 4 | Primary | Primary 4 | 4 |
| 5 | Primary | Primary 5 | 5 |
| 6 | Primary | Primary 6 | 6 |
| 7 | Junior Secondary | JSS 1 | 7 |
| 8 | Junior Secondary | JSS 2 | 8 |
| 9 | Junior Secondary | JSS 3 | 9 |
| 10 | Senior Secondary | SSS 1 | 10 |
| 11 | Senior Secondary | SSS 2 | 11 |
| 12 | Senior Secondary | SSS 3 | 12 |

### 4.2 SQL Seed

```sql
CREATE TABLE class_levels (
  id SERIAL PRIMARY KEY,
  category TEXT NOT NULL,
  name TEXT NOT NULL UNIQUE,
  display_order INT NOT NULL
);

INSERT INTO class_levels (category, name, display_order) VALUES
  ('Primary', 'Primary 1', 1),
  ('Primary', 'Primary 2', 2),
  ('Primary', 'Primary 3', 3),
  ('Primary', 'Primary 4', 4),
  ('Primary', 'Primary 5', 5),
  ('Primary', 'Primary 6', 6),
  ('Junior Secondary', 'JSS 1', 7),
  ('Junior Secondary', 'JSS 2', 8),
  ('Junior Secondary', 'JSS 3', 9),
  ('Senior Secondary', 'SSS 1', 10),
  ('Senior Secondary', 'SSS 2', 11),
  ('Senior Secondary', 'SSS 3', 12);
```

> `class_levels` is a public read-only table. No RLS write access is granted to any user role.

---

## 5. Database Schema

### 5.1 Global Conventions

- All primary keys use `UUID` generated with `gen_random_uuid()`
- All tables include `created_at TIMESTAMPTZ DEFAULT NOW()` and `updated_at TIMESTAMPTZ DEFAULT NOW()`
- Soft deletes via `deleted_at TIMESTAMPTZ DEFAULT NULL` — records with a non-null `deleted_at` are excluded from all queries via RLS or views
- `teacher_id UUID REFERENCES auth.users(id)` is the ownership anchor on all user-owned tables
- All foreign keys use `ON DELETE CASCADE` unless specified otherwise

### 5.2 Profiles Table

Extends Supabase's `auth.users` with app-specific fields.

```sql
CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name TEXT,
  phone TEXT,
  avatar_url TEXT,
  referral_code TEXT UNIQUE NOT NULL,
  referred_by UUID REFERENCES profiles(id),
  plan TEXT NOT NULL DEFAULT 'basic' CHECK (plan IN ('basic', 'advanced', 'premium')),
  plan_expires_at TIMESTAMPTZ,
  ai_credits_used INT NOT NULL DEFAULT 0,
  fcm_token TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);
```

### 5.3 Schools Table

```sql
CREATE TABLE schools (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  address TEXT,
  logo_url TEXT,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);
```

### 5.4 School Classes Table

Links a school to a Nigerian class level.

```sql
CREATE TABLE school_classes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  class_level_id INT NOT NULL REFERENCES class_levels(id),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ,
  UNIQUE (school_id, class_level_id)
);
```

### 5.5 Subjects Table

```sql
CREATE TABLE subjects (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE CASCADE,
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ,
  UNIQUE (school_class_id, name)
);
```

### 5.6 Syllabus Topics Table

```sql
CREATE TABLE syllabus_topics (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  term TEXT CHECK (term IN ('First', 'Second', 'Third')),
  week_number INT CHECK (week_number BETWEEN 1 AND 14),
  display_order INT DEFAULT 0,
  has_lesson_note BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);
```

### 5.7 Lesson Notes Table

```sql
CREATE TABLE lesson_notes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  syllabus_topic_id UUID NOT NULL REFERENCES syllabus_topics(id) ON DELETE CASCADE,
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  content TEXT,
  ai_generated BOOLEAN DEFAULT FALSE,
  teaching_guide TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);
```

### 5.8 Questions Table

```sql
CREATE TABLE questions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  lesson_note_id UUID NOT NULL REFERENCES lesson_notes(id) ON DELETE CASCADE,
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('mcq', 'essay')),
  content JSONB NOT NULL,
  ai_generated BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);
```

> `content` JSONB structure for MCQ: `{ "question": "", "options": ["A","B","C","D"], "answer": "A" }`  
> `content` JSONB structure for Essay: `{ "question": "", "sample_answer": "" }`

### 5.9 Alarms Table

```sql
CREATE TABLE alarms (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('wake_up', 'period', 'syllabus_gap', 'custom')),
  label TEXT,
  time TIME NOT NULL,
  days_of_week INT[] DEFAULT '{1,2,3,4,5}',
  is_active BOOLEAN DEFAULT TRUE,
  school_id UUID REFERENCES schools(id),
  metadata JSONB,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);
```

### 5.10 Plans Config Table

Managed via admin panel. Stores plan limits.

```sql
CREATE TABLE plan_configs (
  id SERIAL PRIMARY KEY,
  plan TEXT NOT NULL UNIQUE CHECK (plan IN ('basic', 'advanced', 'premium')),
  monthly_price_kobo INT NOT NULL DEFAULT 0,
  ai_lesson_notes_limit INT NOT NULL DEFAULT 0,
  ai_questions_limit INT NOT NULL DEFAULT 0,
  ai_teaching_guide_limit INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO plan_configs (plan, monthly_price_kobo, ai_lesson_notes_limit, ai_questions_limit, ai_teaching_guide_limit)
VALUES
  ('basic', 0, 0, 0, 0),
  ('advanced', 150000, 20, 50, 10),
  ('premium', 350000, 999999, 999999, 999999);
```

> Prices stored in kobo (Nigerian currency subunit, 100 kobo = ₦1).

### 5.11 AI Usage Log Table

```sql
CREATE TABLE ai_usage_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  feature TEXT NOT NULL CHECK (feature IN ('lesson_note', 'mcq', 'essay', 'teaching_guide')),
  tokens_used INT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### 5.12 Referrals Table

```sql
CREATE TABLE referrals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  referrer_id UUID NOT NULL REFERENCES profiles(id),
  referred_id UUID NOT NULL REFERENCES profiles(id),
  reward_granted BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (referred_id)
);
```

---

## 6. Row Level Security (RLS)

RLS must be enabled on every user-owned table. The pattern is consistent across all tables.

### 6.1 Enable RLS

```sql
ALTER TABLE schools ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE syllabus_topics ENABLE ROW LEVEL SECURITY;
ALTER TABLE lesson_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE alarms ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_usage_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE referrals ENABLE ROW LEVEL SECURITY;
```

### 6.2 Standard RLS Policy Pattern

Applied to each user-owned table (example for `schools`):

```sql
-- Select: teacher sees only their own non-deleted records
CREATE POLICY "teacher_select_schools"
ON schools FOR SELECT
USING (teacher_id = auth.uid() AND deleted_at IS NULL);

-- Insert: teacher can only insert with their own teacher_id
CREATE POLICY "teacher_insert_schools"
ON schools FOR INSERT
WITH CHECK (teacher_id = auth.uid());

-- Update: teacher can only update their own records
CREATE POLICY "teacher_update_schools"
ON schools FOR UPDATE
USING (teacher_id = auth.uid());

-- Delete: soft delete enforced at application layer; hard delete blocked
-- No DELETE policy granted to authenticated users
```

### 6.3 Public Read Tables

```sql
-- class_levels is public read, no auth required
CREATE POLICY "public_read_class_levels"
ON class_levels FOR SELECT
USING (TRUE);

-- plan_configs is public read
CREATE POLICY "public_read_plan_configs"
ON plan_configs FOR SELECT
USING (TRUE);
```

### 6.4 Admin Role

Admin access is handled via the Supabase `service_role` key, used exclusively by the admin web panel. The service role bypasses RLS entirely. It must never be embedded in the Android app.

---

## 7. Authentication

### 7.1 Supported Auth Methods

| Method | Provider |
|---|---|
| Email + Password | Supabase Auth |
| Google OAuth | Supabase Auth (Google provider) |

### 7.2 Auth Flow

1. User opens app → checks for existing Supabase session token in local storage
2. If session valid → navigate to Home
3. If session expired → attempt silent refresh via `supabase.auth.refreshSession()`
4. If refresh fails → navigate to Login screen
5. On successful login/signup → upsert row in `profiles` table via database trigger

### 7.3 Auto Profile Creation Trigger

```sql
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO profiles (id, full_name, referral_code)
  VALUES (
    NEW.id,
    NEW.raw_user_meta_data->>'full_name',
    UPPER(SUBSTRING(REPLACE(NEW.id::TEXT, '-', ''), 1, 8))
  )
  ON CONFLICT (id) DO NOTHING;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION handle_new_user();
```

### 7.4 Session Management on Android

- Use the Supabase Kotlin SDK
- Store session tokens using Android `EncryptedSharedPreferences`
- Refresh tokens automatically using the SDK's built-in session listener
- On logout: call `supabase.auth.signOut()` and clear local encrypted storage

---

## 8. App Navigation Architecture

### 8.1 Top-Level Navigation

The app uses a bottom navigation bar with 4 primary destinations:

| Tab | Icon | Description |
|---|---|---|
| Home | Home | Dashboard with quick stats and alerts |
| Schools | School building | Manage schools, classes, subjects |
| Alarms | Bell | Manage all alarms and reminders |
| Profile | Person | Profile, plan, settings, referral, help |

### 8.2 Screen Map

```
App
├── Auth
│   ├── Splash / Onboarding
│   ├── Login
│   ├── Sign Up
│   └── Forgot Password
│
├── Home (Tab 1)
│   └── Dashboard
│
├── Schools (Tab 2)
│   ├── School List
│   ├── Add / Edit School
│   ├── School Detail
│   │   ├── Class List
│   │   ├── Add Class (dropdown: Nigerian class standard)
│   │   └── Class Detail
│   │       ├── Subject List
│   │       ├── Add / Edit Subject
│   │       └── Subject Detail
│   │           ├── Syllabus Topic List
│   │           ├── Add / Edit Syllabus Topic
│   │           └── Lesson Note Detail
│   │               ├── View / Edit Note
│   │               ├── AI Generate Note
│   │               ├── Generate Questions (MCQ / Essay)
│   │               └── Teaching Guide
│
├── Alarms (Tab 3)
│   ├── Alarm List
│   └── Add / Edit Alarm
│
└── Profile (Tab 4)
    ├── Profile Detail & Edit
    ├── My Plan & Upgrade
    ├── Referral
    ├── Help & FAQ
    └── Logout
```

### 8.3 Navigation Implementation

- Use Jetpack Compose Navigation with a `NavHost`
- Bottom nav managed by `NavigationBar` composable
- Deep links supported for notification taps (e.g., open specific alarm or lesson note)
- Back stack follows Material 3 navigation guidelines

---

## 9. Shared Conventions

### 9.1 Timestamps

- All timestamps stored as `TIMESTAMPTZ` in UTC
- Displayed in the app converted to the device's local timezone

### 9.2 Soft Deletes

All user-initiated deletes set `deleted_at = NOW()`. Records are never hard-deleted from user-facing operations. RLS policies filter out soft-deleted records automatically. A scheduled Supabase Edge Function can hard-delete records older than 90 days if required.

### 9.3 Updated At Trigger

```sql
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to all tables with updated_at
CREATE TRIGGER set_updated_at_profiles
BEFORE UPDATE ON profiles
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Repeat for: schools, school_classes, subjects, syllabus_topics, lesson_notes, alarms
```

### 9.4 Error Handling

- All Supabase calls wrapped in try/catch on the Android side
- Network errors surface a generic "Connection error. Please try again." toast
- Auth errors (401) trigger a session refresh attempt before surfacing to the user
- All errors logged to a local crash log (consider Firebase Crashlytics integration)

### 9.5 Offline Behaviour

- The app requires an internet connection for all operations in v1.0
- A persistent offline banner is shown when no connection is detected
- Future versions may introduce local caching via Room database for read-only data

---

## 10. Security Checklist

- [ ] RLS enabled on all user-owned tables
- [ ] `service_role` key never shipped in the Android app
- [ ] Only `anon` and `authenticated` Supabase keys used in the app
- [ ] All AI and payment calls made server-side via Edge Functions
- [ ] FCM tokens stored in `profiles` and rotated on each app launch
- [ ] Paystack secret key stored only in Edge Function environment variables
- [ ] Referral codes validated server-side before reward is granted
- [ ] Session tokens stored in `EncryptedSharedPreferences`
- [ ] Certificate pinning considered for Supabase API calls in production

---

## 11. Dependencies & Third-Party Services

| Service | Purpose | Key Location |
|---|---|---|
| Supabase | Database, auth, storage, edge functions | Supabase dashboard |
| Anthropic Claude API | AI features | Edge Function env vars |
| Firebase Cloud Messaging | Push notifications | Firebase console |
| Paystack | Subscription payments | Edge Function env vars |
| Google OAuth | Social login | Supabase Auth providers |

---

## 12. Open Questions

1. Should the app support multiple teacher accounts on one device (account switching)?
2. Is phone number auth required in addition to email and Google?
3. Should offline read access to lesson notes be supported in v1.0?
4. What is the data retention policy for AI usage logs?
5. Is biometric login (fingerprint/face) required?

---

*Next: PRD 02 — School & Class Management*

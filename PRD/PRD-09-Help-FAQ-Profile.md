# PRD 09 — Help, FAQ & Profile

**Project:** Teacher's Companion Android App
**Version:** 1.0
**Status:** Draft
**Depends on:** PRD 01 — Foundation & Architecture, PRD 06 — Plans, Pricing & Subscription, PRD 08 — Admin Web Panel

---

## 1. Overview

This PRD covers two distinct but related areas of the app: the teacher's Profile management (personal details, account settings, avatar, password, and account deletion) and the Help & FAQ section (searchable FAQ content, in-app support contact, and app information). Both are accessible from the main Profile screen, which also surfaces subscription status and AI usage at a glance.

---

## 2. Goals

- Let teachers view and update their personal details and profile picture
- Allow password changes and account deletion with appropriate safeguards
- Provide a searchable, offline-capable FAQ section driven by admin-managed content
- Surface app version, privacy policy, terms, and support contact clearly
- Show plan status and AI usage summary directly on the Profile screen
- Ensure profile data changes propagate to the app's active context immediately

---

## 3. User Stories

| ID | As a teacher I want to... | So that... |
|---|---|---|
| US-01 | View and edit my name and profile photo | My account reflects my identity |
| US-02 | Change my password securely | I can keep my account safe |
| US-03 | See my current plan and usage at a glance | I understand what I have access to |
| US-04 | Navigate to the referral screen from my profile | I can invite colleagues easily |
| US-05 | Search the FAQ for answers to my questions | I can self-serve without contacting support |
| US-06 | Browse FAQ items by category | I can find help even without a specific query |
| US-07 | Contact support directly from the app | I can get help when FAQ doesn't answer my question |
| US-08 | View the app's privacy policy and terms of service | I understand how my data is used |
| US-09 | Delete my account and all associated data | I can leave the platform if I choose |
| US-10 | Log out of the app | I can secure my account on shared devices |

---

## 4. Screens & UX Flow

### 4.1 Profile Screen

**Route:** `profile`

**Access:** Bottom navigation bar — Profile tab (rightmost icon)

**Layout:**

```
┌────────────────────────────────────────┐
│                                        │
│         [Avatar]  Edit ✏️              │
│      Full Name                         │
│      email@example.com                 │
│                                        │
├────────────────────────────────────────┤
│  PLAN & USAGE                          │
│  ┌──────────────────────────────────┐  │
│  │  Advanced Plan   Renews Jun 1    │  │
│  │  AI Usage — May 2025             │  │
│  │  Lesson Notes ████████░░  16/20  │  │
│  │  Questions    ███████░░░  11/15  │  │
│  │  Guides       ████░░░░░░   4/10  │  │
│  │  [Upgrade Plan]                  │  │
│  └──────────────────────────────────┘  │
│                                        │
├────────────────────────────────────────┤
│  ACCOUNT                               │
│  👤  Edit Profile                  ›   │
│  🔒  Change Password               ›   │
│  🎁  Refer a Friend                ›   │
│  💳  Manage Subscription           ›   │
│                                        │
├────────────────────────────────────────┤
│  SUPPORT                               │
│  ❓  Help & FAQ                    ›   │
│  ✉️  Contact Support               ›   │
│                                        │
├────────────────────────────────────────┤
│  APP INFO                              │
│  📄  Privacy Policy                ›   │
│  📋  Terms of Service              ›   │
│  ℹ️  App Version: 1.0.0               │
│                                        │
├────────────────────────────────────────┤
│  [Log Out]                             │
│  [Delete Account]  (destructive, red)  │
└────────────────────────────────────────┘
```

**Plan & Usage widget:**
- Shows current plan name, renewal date (or "Free plan — no renewal")
- AI usage progress bars per feature (mirrors the widget defined in PRD 05, Section 9)
- "Upgrade Plan" CTA only shown for Basic and Advanced plans
- Basic plan users see "No AI features on your current plan. Upgrade to access AI."
- Widget data loaded from `ai_usage` + `subscriptions` + `plans` tables on screen resume

---

### 4.2 Edit Profile Screen

**Route:** `profile/edit`

**Fields:**

| Field | Type | Required | Validation |
|---|---|---|---|
| Full Name | Text input | Yes | 2–80 characters |
| Profile Photo | Image picker | No | Max 2MB, JPEG/PNG |
| Email | Display only (not editable) | — | — |

**Behaviour:**
- Photo uploaded to Supabase Storage: `avatars/{teacher_id}/profile.jpg`
- On save: update `profiles.full_name` and `profiles.avatar_url`
- Email address is not editable in v1.0 (requires support contact if change needed)
- On success: pop back to Profile screen; displayed name and avatar update immediately
- If photo upload fails: save other fields, show toast: "Profile saved. Photo could not be uploaded."

---

### 4.3 Change Password Screen

**Route:** `profile/change-password`

**Fields:**

| Field | Type | Required |
|---|---|---|
| Current Password | Password input | Yes |
| New Password | Password input | Yes |
| Confirm New Password | Password input | Yes |

**Validation:**
- New password: minimum 8 characters, at least one number
- Confirm password must match new password
- Current password is verified via `supabase.auth.updateUser` — if wrong, Supabase returns an error and inline message: "Current password is incorrect."

**Behaviour:**
- On success: snackbar "Password updated successfully." + pop back to Profile
- Teachers who signed up with a social provider (if added in future) see a message: "Your account uses Google sign-in. Password change is not available."

---

### 4.4 Log Out

**Trigger:** "Log Out" button on Profile screen

**Behaviour:**
- Confirmation dialog: "Log out of Teacher's Companion?"
- On confirm: call `supabase.auth.signOut()`
- Clear all local Room DB data
- Navigate to Auth screen (login/signup)
- FCM token is not deregistered at logout (notifications continue until token is replaced or app is uninstalled)

---

### 4.5 Delete Account Screen

**Route:** `profile/delete-account`

**Access:** "Delete Account" button on Profile screen

**Layout:**
- Warning card listing what will be deleted:
  - All schools, classes, subjects, syllabus topics
  - All lesson notes and questions
  - All AI usage history
  - Subscription (access ends immediately)
- Warning: "This action cannot be undone."
- Checkbox: "I understand that all my data will be permanently deleted."
- Password confirmation field: "Enter your password to confirm"
- "Delete My Account" button (enabled only when checkbox is ticked and password entered)

**Behaviour:**
- Calls Edge Function `delete-account` with the teacher's JWT
- Edge Function:
  1. Verifies password
  2. Cancels Paystack subscription if active (via Paystack API)
  3. Sets `deleted_at` on all teacher-owned rows (schools, classes, subjects, topics, notes, alarms)
  4. Calls `supabase.auth.admin.deleteUser(teacher_id)` to delete the `auth.users` row
  5. Returns success
- On success: clear local DB, navigate to Auth screen with snackbar: "Your account has been deleted."
- If subscription cancellation fails: still delete the account, log the failure for admin review

---

### 4.6 Help & FAQ Screen

**Route:** `help`

**Access:** Profile screen → "Help & FAQ"

**Layout:**

```
┌────────────────────────────────────────┐
│ ← Help & FAQ              [🔍 Search] │
├────────────────────────────────────────┤
│  Categories                            │
│  ┌───────────┐ ┌───────────┐          │
│  │ 🚀 Getting│ │ 🤖 AI     │          │
│  │  Started  │ │ Features  │          │
│  └───────────┘ └───────────┘          │
│  ┌───────────┐ ┌───────────┐          │
│  │ 💳 Billing│ │ 🔔 Alarms │          │
│  └───────────┘ └───────────┘          │
│  ┌───────────┐                        │
│  │ 👤 Account│                        │
│  └───────────┘                        │
│                                        │
│  Popular Questions                     │
│  ─────────────────────────────────     │
│  How do I generate a lesson note? ›    │
│  What's included in the free plan? ›   │
│  How does the referral system work? ›  │
│  How do I add a school? ›              │
└────────────────────────────────────────┘
```

**Data source:**
- FAQ categories and items fetched from `faq_categories` and `faq_items` tables on first load
- Cached in local Room DB for offline access
- Cache refreshed on every app launch (silent background fetch)
- Only `is_visible = TRUE` records are returned (filtered by RLS policy)

---

### 4.7 FAQ Category Screen

**Route:** `help/category/{id}`

**Layout:**
- Top app bar: category name
- Accordion list of FAQ items (question visible; answer expands on tap)
- Each answer rendered as HTML (supporting bold, italic, bullet lists from admin editor)

---

### 4.8 FAQ Search Screen

**Route:** `help/search`

**Trigger:** Search icon on Help screen

**Layout:**
- Search text field (autofocused on open)
- Results list: matching questions with category label
- "No results for '{query}'" empty state with suggestion: "Try different keywords, or contact support."

**Behaviour:**
- Search is performed client-side against the locally cached FAQ dataset
- Matches on question text and answer text (case-insensitive substring match)
- Results update in real-time as user types

---

### 4.9 FAQ Item Detail Screen

**Route:** `help/item/{id}`

**Layout:**
- Top app bar: category name
- Question as a bold heading
- Answer rendered as formatted HTML
- "Was this helpful?" thumbs up / thumbs down widget (logs to `faq_feedback` table — analytics only, not surfaced in admin panel v1.0)
- "Still need help? Contact Support" button at the bottom

---

### 4.10 Contact Support Screen

**Route:** `help/contact`

**Access:** Profile screen → "Contact Support" or FAQ item screen → "Contact Support"

**Layout:**
- Pre-populated fields:
  - Name: from `profiles.full_name`
  - Email: from auth session
  - App Version: auto-filled
  - Plan: auto-filled
- Editable fields:
  - Subject: text input
  - Message: multiline text input (max 1,000 characters)
- "Send" button

**Behaviour:**
- Sends an email to the support address stored in `admin_config.support_email` via a Supabase Edge Function `send-support-email`
- Uses a transactional email provider (e.g., Resend or SendGrid) configured in the Edge Function
- On success: snackbar "Your message has been sent. We'll get back to you within 24 hours."
- On failure: toast "Failed to send. Please try emailing us directly at {support_email}."

---

### 4.11 Privacy Policy & Terms of Service

**Trigger:** Respective list items on Profile screen

**Behaviour:**
- URL fetched from `admin_config` keys `privacy_policy_url` and `terms_url`
- Opens in Android Chrome Custom Tab
- No in-app web view — always opens in system browser

---

## 5. Backend Architecture

### 5.1 Edge Function: `delete-account`

```typescript
// POST /functions/v1/delete-account
// Auth: teacher's Supabase JWT

const teacher_id = jwt.sub;

// 1. Verify password
const { error: authError } = await supabase.auth.signInWithPassword({
  email: teacher.email,
  password: req.body.password
});
if (authError) return { error: 'INVALID_PASSWORD' };

// 2. Cancel Paystack subscription if active
const { data: sub } = await supabase
  .from('subscriptions')
  .select('paystack_subscription_code')
  .eq('teacher_id', teacher_id)
  .eq('status', 'active')
  .single();

if (sub?.paystack_subscription_code) {
  await paystackDisableSubscription(sub.paystack_subscription_code);
}

// 3. Soft-delete all teacher data
const tables = ['schools', 'school_classes', 'subjects', 'syllabus_topics',
                 'lesson_notes', 'questions', 'alarms', 'period_reminders'];
for (const table of tables) {
  await supabase.from(table)
    .update({ deleted_at: new Date().toISOString() })
    .eq('teacher_id', teacher_id);
}

// 4. Hard-delete auth user
await supabase.auth.admin.deleteUser(teacher_id);
```

### 5.2 Edge Function: `send-support-email`

```typescript
// POST /functions/v1/send-support-email
// Auth: teacher's Supabase JWT

const { subject, message, app_version, plan } = req.body;
const support_email = await getAdminConfig('support_email');

await resend.emails.send({
  from: 'noreply@teacherscompanion.app',
  to: support_email,
  subject: `[Support] ${subject}`,
  html: `
    <p><strong>From:</strong> ${teacher.full_name} (${teacher.email})</p>
    <p><strong>Plan:</strong> ${plan}</p>
    <p><strong>App Version:</strong> ${app_version}</p>
    <hr/>
    <p>${message}</p>
  `
});
```

### 5.3 FAQ Caching (Android)

```kotlin
// Room DB entity for offline FAQ
@Entity(tableName = "faq_items_cache")
data class FaqItemCache(
    @PrimaryKey val id: String,
    val categoryId: String,
    val categoryName: String,
    val question: String,
    val answer: String,     // HTML string
    val displayOrder: Int,
    val cachedAt: Long
)

// Refresh on app launch — silent, non-blocking
suspend fun refreshFaqCache() {
    val items = supabase.from("faq_items")
        .select("*, faq_categories(name)")
        .eq("is_visible", true)
        .execute()
    faqDao.replaceAll(items.data.map { it.toCache() })
}
```

### 5.4 FAQ Feedback Table

```sql
CREATE TABLE faq_feedback (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  item_id     UUID REFERENCES faq_items(id),
  teacher_id  UUID REFERENCES auth.users(id),
  helpful     BOOLEAN NOT NULL,
  created_at  TIMESTAMPTZ DEFAULT now()
);
```

---

## 6. Data Model

```sql
-- Additions to profiles table (beyond PRD 01 baseline)
ALTER TABLE profiles ADD COLUMN full_name TEXT;
ALTER TABLE profiles ADD COLUMN avatar_url TEXT;

-- Supabase Storage
-- Bucket: avatars (private, authenticated access)
-- Path: avatars/{teacher_id}/profile.jpg
```

**Storage access policy:**
```sql
CREATE POLICY "teacher_access_own_avatar"
ON storage.objects FOR ALL
USING (
  bucket_id = 'avatars'
  AND auth.uid()::TEXT = (storage.foldername(name))[1]
);
```

---

## 7. RLS Policies

```sql
-- FAQ content readable by all authenticated teachers
-- (defined in PRD 08, repeated here for completeness)
ALTER TABLE faq_categories ENABLE ROW LEVEL SECURITY;
CREATE POLICY "authenticated_read_faq_categories" ON faq_categories FOR SELECT
USING (auth.role() = 'authenticated' AND is_visible = TRUE);

ALTER TABLE faq_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY "authenticated_read_faq_items" ON faq_items FOR SELECT
USING (auth.role() = 'authenticated' AND is_visible = TRUE);

-- FAQ feedback writable by teacher (for their own rows)
ALTER TABLE faq_feedback ENABLE ROW LEVEL SECURITY;
CREATE POLICY "teacher_insert_faq_feedback" ON faq_feedback FOR INSERT
WITH CHECK (teacher_id = auth.uid());

-- Profiles: each teacher reads/updates only their own row
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "teacher_read_own_profile" ON profiles FOR SELECT
USING (id = auth.uid());

CREATE POLICY "teacher_update_own_profile" ON profiles FOR UPDATE
USING (id = auth.uid());
```

---

## 8. Validation & Error States

| Scenario | Handling |
|---|---|
| Profile save fails (network) | Toast: "Could not save. Check your connection and try again." |
| Avatar too large (>2MB) | Inline error before upload: "Image must be under 2MB." |
| Current password wrong on change password | Inline error: "Current password is incorrect." |
| New passwords don't match | Inline error under confirm field: "Passwords do not match." |
| Account deletion password wrong | Inline error: "Incorrect password. Please try again." |
| Support email send failure | Toast with fallback email address shown |
| FAQ fetch fails (no internet) | Show cached content; banner: "Showing offline content. Connect to refresh." |
| FAQ cache empty + no internet | Empty state: "Help content unavailable offline. Please connect to load." |
| Deep link to Privacy Policy URL not configured | Open a fallback static URL bundled in the app |

---

## 9. Acceptance Criteria

- [ ] Teacher can update their full name and profile photo from the Edit Profile screen
- [ ] Profile photo is uploaded to Supabase Storage and displayed correctly after update
- [ ] Password change correctly validates current password before allowing update
- [ ] Account deletion requires password confirmation and a checkbox acknowledgement
- [ ] All teacher data is soft-deleted and auth user is removed on account deletion
- [ ] FAQ content loads from cache when offline
- [ ] FAQ search returns results matching question or answer text
- [ ] FAQ items render bold, italic, and list formatting correctly from the admin HTML
- [ ] "Was this helpful?" feedback is recorded in `faq_feedback` without interrupting UX
- [ ] Support email is sent with teacher name, email, plan, and app version pre-filled
- [ ] Privacy Policy and Terms of Service open in Chrome Custom Tab
- [ ] Plan & Usage widget on Profile screen reflects current month's usage accurately
- [ ] Log out clears local Room DB and returns teacher to the auth screen
- [ ] Email address is displayed but not editable on the Edit Profile screen

---

*Previous: PRD 08 — Admin Web Panel*
*End of PRD Series — v1.0*

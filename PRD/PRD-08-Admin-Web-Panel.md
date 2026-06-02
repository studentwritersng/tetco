# PRD 08 — Admin Web Panel

**Project:** Teacher's Companion Android App
**Version:** 1.0
**Status:** Draft
**Depends on:** PRD 01 — Foundation & Architecture, PRD 05 — AI Features, PRD 06 — Plans, Pricing & Subscription, PRD 07 — Referral System

---

## 1. Overview

This PRD covers the Admin Web Panel — a separate web application that shares the same Supabase project as the Android app but operates with elevated (service-role) access. Admins can configure plan pricing and AI usage limits, manage users, monitor referral activity, view AI usage analytics, broadcast push notifications, and manage FAQ/Help content — all without requiring an app update. The panel is built with React and communicates exclusively with Supabase over the admin service-role key stored server-side.

---

## 2. Goals

- Allow non-technical admins to adjust plan pricing and AI limits without a code deployment
- Provide a full user management view with search, filtering, and manual plan overrides
- Monitor AI usage and referral activity through clear dashboards
- Send broadcast FCM push notifications to all users or filtered segments
- Manage FAQ and Help content served to the Android app
- Require secure, role-gated authentication — no Android teacher credentials work here

---

## 3. User Stories

| ID | As an admin I want to... | So that... |
|---|---|---|
| US-01 | Log in securely to the admin panel | Only authorised admins can access it |
| US-02 | Edit plan prices and AI usage limits | I can adjust the product without a code release |
| US-03 | Search and view any teacher's account | I can provide support and investigate issues |
| US-04 | Manually override a teacher's plan | I can apply goodwill upgrades or fix payment errors |
| US-05 | See a dashboard of AI usage across all teachers | I can monitor costs and usage trends |
| US-06 | See referral statistics and top referrers | I can track the growth program's performance |
| US-07 | Configure referral reward amounts and qualifying actions | I can adjust incentives without a code release |
| US-08 | Send a push notification to all or selected teachers | I can communicate important updates |
| US-09 | Add, edit, and delete FAQ items and categories | I can keep the in-app Help section current |
| US-10 | View and export a list of all registered teachers | I can produce reports for stakeholders |

---

## 4. Authentication & Access Control

### 4.1 Admin Authentication

- Admins authenticate via a dedicated `admin_users` table, not `auth.users`
- Login: email + password, validated server-side against `admin_users`
- Session managed with a short-lived JWT signed with a separate admin secret (`ADMIN_JWT_SECRET`)
- No social login — email/password only
- Brute-force protection: account locked after 5 failed attempts for 15 minutes

```sql
CREATE TABLE admin_users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email           TEXT UNIQUE NOT NULL,
  password_hash   TEXT NOT NULL,         -- bcrypt, min 12 rounds
  role            TEXT CHECK (role IN ('super_admin', 'support')) DEFAULT 'support',
  last_login_at   TIMESTAMPTZ,
  locked_until    TIMESTAMPTZ,
  failed_attempts INT DEFAULT 0,
  created_at      TIMESTAMPTZ DEFAULT now(),
  updated_at      TIMESTAMPTZ DEFAULT now()
);
```

**Roles:**

| Role | Permissions |
|---|---|
| `super_admin` | Full access — all modules including plan config, user deletion, notification broadcast |
| `support` | Read-only on most modules; can view users and respond to feedback; cannot edit plans or send broadcasts |

### 4.2 Admin Panel Architecture

- **Frontend:** React (Vite) + TailwindCSS, deployed to Vercel or Netlify
- **Backend:** Supabase with service-role key stored in server-side Next.js API routes (or Supabase Edge Functions acting as a proxy)
- The Android app's RLS policies do not apply to admin operations — service-role bypasses RLS
- Admin panel is hosted on a separate subdomain: `admin.teacherscompanion.app`

---

## 5. Screens & UX Flow

### 5.1 Login Screen

**Route:** `/login`

**Layout:**
- Centered card: "Teacher's Companion — Admin"
- Email + password fields
- "Sign In" button
- Error messages inline (invalid credentials, account locked)

---

### 5.2 Dashboard (Home)

**Route:** `/dashboard`

**Layout — summary cards row:**
- Total Teachers (all time)
- Active Subscriptions (Advanced + Premium)
- AI Generations Today
- New Registrations (last 7 days)

**Charts section:**
- Line chart: daily new teacher registrations (last 30 days)
- Bar chart: AI feature usage by type (Lesson Notes, MCQ, Essay, Teaching Guide) for current month
- Pie chart: plan distribution (Basic / Advanced / Premium)

**Recent Activity feed:**
- Last 10 teacher registrations with name, email, plan, and timestamp
- Last 10 successful payments (amount, plan, teacher email)

---

### 5.3 Plan & Pricing Management

**Route:** `/plans`

**Layout:**
- Table of all plans (Basic, Advanced, Premium) with editable inline rows:

| Column | Editable |
|---|---|
| Plan Name | No |
| Price (NGN) | Yes |
| Lesson Note Limit | Yes |
| MCQ Limit | Yes |
| Essay Limit | Yes |
| Teaching Guide Limit | Yes |
| MCQ per generation | Yes |
| Essay per generation | Yes |
| Paystack Plan Code | Yes |
| Is Active | Yes (toggle) |

- Each row has an "Edit" button that opens an Edit Plan modal
- Changes saved immediately to the `plans` table via service-role
- Confirmation dialog: "Updating this plan will affect all active subscribers on this plan immediately."

**Edit Plan Modal:**
- Form pre-populated with current values
- Input validation: limits must be positive integers; price must be ≥ 0
- Save / Cancel buttons

---

### 5.4 User Management

**Route:** `/users`

**Layout:**
- Search bar (search by name, email)
- Filter dropdowns: Plan | Registration Date Range | Status (active, cancelled, past_due)
- Results table:

| Column | Description |
|---|---|
| Name | From `profiles.full_name` |
| Email | From `auth.users.email` |
| Plan | From `profiles.plan_name` |
| Joined | `profiles.created_at` |
| Last Active | `profiles.updated_at` |
| Referrals | Count of qualified referrals |
| Status | Subscription status badge |
| Actions | View / Edit |

- Pagination: 50 rows per page
- Export CSV button (exports current filter results)

**User Detail Page:**

**Route:** `/users/{id}`

**Sections:**
- **Profile:** name, email, FCM token status, active school, plan, join date
- **Subscription:** current plan, status, renewal date, Paystack subscription code, cancel_at_period_end flag
  - "Override Plan" button → modal to manually set plan and period end date (super_admin only)
  - "Cancel Subscription" button (super_admin only)
- **AI Usage:** table of monthly usage rows (lesson notes, questions, teaching guides) for past 6 months
- **Referrals:** table showing referrals made (qualified / pending) and referral credits earned this month
- **Schools:** list of schools the teacher has added (names only, no drill-down)
- **Notes:** internal admin notes field (free text, saved to `admin_notes` table, not visible to the teacher)

---

### 5.5 AI Usage Analytics

**Route:** `/analytics/ai`

**Layout:**
- Date range picker (default: current month)
- Summary row: Total Lesson Notes Generated | Total MCQs | Total Essays | Total Teaching Guides
- Bar chart: daily AI usage volume across all teachers, broken down by feature type
- Table: Top 20 teachers by AI usage this month (name, email, plan, usage per feature)
- Export CSV button

**Cost estimation widget:**
- Estimated Anthropic API cost for the selected period based on average token counts per generation type (configurable constants in admin settings)

---

### 5.6 Referral Management

**Route:** `/referrals`

**Layout:**
- Summary row: Total Referrals Made | Total Qualified | Conversion Rate (%) | Credits Issued This Month
- Bar chart: referrals made per day (last 30 days)

**Referral Settings card:**
- Editable fields (super_admin only):
  - Qualifying action: dropdown (`First Topic Added`, `Email Verified`) — stored in `admin_config` table
  - Referrer reward: lesson note credits, MCQ credits, essay credits, guide credits (integer inputs)
  - Referee reward: same fields
  - Monthly credit cap (max referral credits per teacher per month)
- Save button — updates `admin_config` table; Edge Functions read this on every reward issuance

**Referral Leaderboard table:**
- Top 20 referrers by qualified referrals (name/email redacted as per PRD 07, full email shown to admin)
- Columns: Teacher, Email, Qualified Referrals, Pending, Credits Earned This Month

---

### 5.7 Push Notification Broadcast

**Route:** `/notifications`

**Layout:**
- Audience selector:
  - All teachers
  - By plan (Basic / Advanced / Premium)
  - Custom (paste comma-separated emails)
- Notification form:
  - Title (max 65 characters)
  - Body (max 240 characters)
  - Deep link target (optional dropdown): Home, Syllabus Gaps, Referral Screen, Plan Upgrade
- Preview card showing how the notification will appear on an Android device
- "Send" button (super_admin only) — requires confirmation modal: "Send to {X} devices?"

**Sent notification history table:**
- Date | Title | Audience | Recipients | Sent by

**Backend:**
- Broadcast calls the `broadcast-notification` Edge Function with service-role auth
- Edge Function fetches FCM tokens for the target audience in batches of 500
- Uses Firebase Admin SDK to send via `sendMulticast`

---

### 5.8 FAQ & Help Content Management

**Route:** `/help`

**Layout:**
- Left sidebar: list of FAQ categories (drag to reorder)
- Main area: list of FAQ items in the selected category
- "+ Add Category" button
- "+ Add Item" button (within selected category)

**Category:**
- Name (e.g., "Getting Started", "AI Features", "Billing")
- Display order (drag-and-drop)
- Is visible toggle

**FAQ Item:**
- Question (text)
- Answer (rich text — bold, italic, bullet lists via a simple editor)
- Display order (drag-and-drop within category)
- Is visible toggle

**Behaviour:**
- Saves to `faq_categories` and `faq_items` tables
- Android app fetches FAQ content at app launch (cached locally for offline use)
- Unpublished (is_visible = FALSE) items are not returned to the Android app

---

### 5.9 App Configuration (Admin Settings)

**Route:** `/settings`

**Sections:**

**General Config** (stored in `admin_config` table as key-value pairs):
- App version minimum required (force-update threshold)
- Maintenance mode toggle (shows maintenance banner in the Android app)
- Support email address (shown on Help screen)
- Privacy Policy URL
- Terms of Service URL

**AI Cost Constants:**
- Estimated tokens per lesson note generation
- Estimated tokens per MCQ generation
- Estimated tokens per essay generation
- Estimated tokens per teaching guide generation
- Cost per 1,000 tokens (for the cost estimation widget in Analytics)

**Notification Schedule:**
- Gap digest cron expression (default: `0 6 * * 1-5`)
- Immediate gap alert inactivity threshold (hours)

---

## 6. Backend Architecture

### 6.1 Admin Config Table

```sql
CREATE TABLE admin_config (
  key         TEXT PRIMARY KEY,
  value       TEXT NOT NULL,
  updated_by  UUID REFERENCES admin_users(id),
  updated_at  TIMESTAMPTZ DEFAULT now()
);

-- Seed default values
INSERT INTO admin_config (key, value) VALUES
  ('referral_qualifying_action', 'first_topic'),
  ('referral_referrer_lesson_notes', '5'),
  ('referral_referrer_mcq', '3'),
  ('referral_referrer_essay', '3'),
  ('referral_referrer_guide', '2'),
  ('referral_referee_lesson_notes', '3'),
  ('referral_referee_mcq', '2'),
  ('referral_referee_essay', '2'),
  ('referral_referee_guide', '1'),
  ('referral_monthly_credit_cap', '3'),
  ('maintenance_mode', 'false'),
  ('min_app_version', '1.0.0'),
  ('support_email', 'support@teacherscompanion.app'),
  ('gap_digest_cron', '0 6 * * 1-5'),
  ('gap_alert_inactivity_hours', '2');
```

### 6.2 FAQ Tables

```sql
CREATE TABLE faq_categories (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name          TEXT NOT NULL,
  display_order INT DEFAULT 0,
  is_visible    BOOLEAN DEFAULT TRUE,
  created_at    TIMESTAMPTZ DEFAULT now(),
  updated_at    TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE faq_items (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  category_id   UUID REFERENCES faq_categories(id) ON DELETE CASCADE,
  question      TEXT NOT NULL,
  answer        TEXT NOT NULL,   -- stored as HTML from rich text editor
  display_order INT DEFAULT 0,
  is_visible    BOOLEAN DEFAULT TRUE,
  created_at    TIMESTAMPTZ DEFAULT now(),
  updated_at    TIMESTAMPTZ DEFAULT now()
);
```

### 6.3 Admin Notes Table

```sql
CREATE TABLE admin_notes (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id  UUID REFERENCES auth.users(id),
  admin_id    UUID REFERENCES admin_users(id),
  note        TEXT NOT NULL,
  created_at  TIMESTAMPTZ DEFAULT now()
);
```

### 6.4 Broadcast Notification Log Table

```sql
CREATE TABLE notification_broadcasts (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title           TEXT NOT NULL,
  body            TEXT NOT NULL,
  audience        TEXT NOT NULL,   -- 'all', 'basic', 'advanced', 'premium', 'custom'
  recipient_count INT DEFAULT 0,
  sent_by         UUID REFERENCES admin_users(id),
  sent_at         TIMESTAMPTZ DEFAULT now()
);
```

### 6.5 Edge Function: `broadcast-notification`

```typescript
// Called from Admin Panel with service-role auth
const { audience, title, body, deep_link } = req.body;

let query = supabase.from('profiles').select('fcm_token');

if (audience === 'basic') query = query.eq('plan_name', 'Basic');
if (audience === 'advanced') query = query.eq('plan_name', 'Advanced');
if (audience === 'premium') query = query.eq('plan_name', 'Premium');

const { data: teachers } = await query.not('fcm_token', 'is', null);
const tokens = teachers.map(t => t.fcm_token);

// Send in batches of 500 (FCM multicast limit)
for (let i = 0; i < tokens.length; i += 500) {
  await sendFCMMulticast({
    tokens: tokens.slice(i, i + 500),
    notification: { title, body },
    data: { deep_link: deep_link ?? '' }
  });
}

await supabase.from('notification_broadcasts').insert({
  title, body, audience,
  recipient_count: tokens.length,
  sent_by: admin.id
});
```

### 6.6 Edge Function: `admin-config`

```typescript
// GET: fetch all config keys
// PATCH: update one or more config keys (super_admin only)

// Android app calls this at startup to get:
// - maintenance_mode
// - min_app_version
// - support_email
// - privacy_policy_url
// - terms_url
// Returns only public keys — not referral reward amounts or cost constants
```

---

## 7. RLS & Security

- The Admin Web Panel communicates using the **service-role key**, which bypasses all RLS policies
- The service-role key is **never** exposed to the browser — all admin API calls are proxied through server-side API routes (Next.js `/api/*`) or Edge Functions
- Admin JWT is validated on every server-side request via middleware
- `admin_users`, `admin_config`, `admin_notes`, and `notification_broadcasts` tables are not accessible via the Supabase anon or authenticated keys — accessible only via service-role

```sql
-- Deny all access via anon/authenticated role
ALTER TABLE admin_users ENABLE ROW LEVEL SECURITY;
CREATE POLICY "no_access" ON admin_users FOR ALL USING (false);

ALTER TABLE admin_config ENABLE ROW LEVEL SECURITY;
-- Public read for non-sensitive keys only
CREATE POLICY "public_read_config" ON admin_config FOR SELECT
USING (key IN ('maintenance_mode', 'min_app_version', 'support_email'));

-- FAQ content readable by authenticated teachers
ALTER TABLE faq_categories ENABLE ROW LEVEL SECURITY;
CREATE POLICY "authenticated_read_faq_categories" ON faq_categories FOR SELECT
USING (auth.role() = 'authenticated' AND is_visible = TRUE);

ALTER TABLE faq_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY "authenticated_read_faq_items" ON faq_items FOR SELECT
USING (auth.role() = 'authenticated' AND is_visible = TRUE);
```

---

## 8. Validation & Error States

| Scenario | Handling |
|---|---|
| Admin enters wrong password | Inline error: "Invalid email or password." Increment `failed_attempts` |
| Account locked | Error: "Account locked due to too many failed attempts. Try again in 15 minutes." |
| Plan limit set to a non-integer | Inline validation: "Must be a whole number." |
| Plan price set to 0 for a paid plan | Warning dialog: "Setting price to ₦0 will make this plan free. Are you sure?" |
| Broadcast sent to 0 devices (no FCM tokens) | Warning: "No devices matched this audience. Notification not sent." |
| FAQ item answer is empty | Inline error: "Answer cannot be empty." |
| Admin config save fails | Toast: "Failed to save. Please try again." with retry |
| CSV export with 0 results | Toast: "No records match the current filter." |

---

## 9. Acceptance Criteria

- [ ] Admin login is blocked after 5 failed attempts
- [ ] Only super_admin role can edit plans, send broadcasts, or delete users
- [ ] Plan price and limit changes reflect in the Android app within 60 seconds (app re-fetches plans on resume)
- [ ] AI usage analytics correctly aggregate across all teachers for the selected date range
- [ ] Referral reward configuration changes are picked up by the Edge Function on next referral event
- [ ] FCM broadcast reaches the correct audience segment
- [ ] FAQ items created or hidden in the admin panel are reflected in the Android app on next launch
- [ ] Service-role key is never exposed in any browser network request
- [ ] Maintenance mode toggle shows a maintenance banner in the Android app within 60 seconds
- [ ] User CSV export includes name, email, plan, join date, and AI usage summary

---

*Previous: PRD 07 — Referral System*
*Next: PRD 09 — Help, FAQ & Profile*

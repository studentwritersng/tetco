# PRD 06 — Plans, Pricing & Subscription

**Project:** Teacher's Companion Android App
**Version:** 1.0
**Status:** Draft
**Depends on:** PRD 01 — Foundation & Architecture, PRD 05 — AI Features

---

## 1. Overview

This PRD covers the three-tier subscription model for the Teacher's Companion app: Basic (free), Advanced, and Premium. It defines how plans are selected, how payments are processed via Paystack (Nigeria's primary payment gateway), how AI usage quotas are enforced per plan, and how teachers move between plans. Plan pricing and AI limits are configurable from the Admin Panel (PRD 08) without requiring an app update.

---

## 2. Goals

- Offer a compelling free tier that drives adoption with no barriers
- Gate AI features behind paid plans with clearly communicated value
- Enable smooth upgrades and downgrades with prorated handling
- Process Nigerian Naira (NGN) payments via Paystack
- Allow admins to adjust plan pricing and AI limits without an app release
- Surface plan status and usage clearly within the app

---

## 3. Plan Definitions

### 3.1 Basic Plan (Free)

- **Price:** ₦0 / month (forever free)
- **Schools:** Up to 10
- **Classes & Subjects:** Unlimited
- **Syllabus Topics:** Unlimited
- **Lesson Notes (manual):** Unlimited
- **Alarms & Reminders:** Unlimited
- **AI Features:** None
- **Referral Rewards:** Eligible (can earn AI credits for referrals)

### 3.2 Advanced Plan

- **Price:** ₦X / month (default ₦2,500 — configurable from Admin Panel)
- **Everything in Basic, plus:**
- **Lesson Note Generation:** 20 / month
- **MCQ Generation:** 15 / month
- **Essay Generation:** 15 / month
- **Teaching Guide Generation:** 10 / month
- **Questions per MCQ generation:** 5
- **Questions per Essay generation:** 3

### 3.3 Premium Plan

- **Price:** ₦Y / month (default ₦5,000 — configurable from Admin Panel)
- **Everything in Advanced, plus:**
- **Lesson Note Generation:** Unlimited
- **MCQ Generation:** Unlimited
- **Essay Generation:** Unlimited
- **Teaching Guide Generation:** Unlimited
- **Questions per MCQ generation:** 10
- **Questions per Essay generation:** 5
- **Priority support** (badge on help screen)

> Plan limits and prices are stored in the `plans` table and fetched by the app at launch. No hardcoded values exist in the Android codebase.

---

## 4. Screens & UX Flow

### 4.1 Plan Selection Screen

**Route:** `settings/plans`

**Access points:**
- Settings menu → "My Plan"
- Quota exceeded bottom sheet → "Upgrade Plan" CTA
- Onboarding flow (after first school is created)

**Layout:**
- Top app bar: "Choose Your Plan"
- Current plan highlighted with "Current Plan" badge
- Three plan cards, stacked vertically:
  - Plan name + price per month
  - Feature list (checkmarks for included, greyed out for excluded)
  - AI usage limits shown as numbers (e.g., "20 lesson note generations/month")
  - CTA button: "Get Started" (Basic) / "Upgrade" or "Subscribe" (paid)
- Toggle at top: Monthly (default) — Annual billing may be added in v1.1
- Footer note: "Prices in Nigerian Naira (NGN). Billed monthly."

**Plan card CTA states:**
- `Current Plan` → button is disabled, labelled "Current Plan"
- Lower plan → button labelled "Downgrade"
- Higher plan → button labelled "Upgrade"
- Basic (not current) → button labelled "Switch to Free"

---

### 4.2 Checkout / Payment Screen

**Triggered by:** Tapping "Upgrade" or "Subscribe" on a paid plan card.

**Flow:**
1. App calls Edge Function to create a Paystack payment session
2. Edge Function returns a `payment_url` (Paystack hosted checkout link)
3. App opens the Paystack payment page in a Chrome Custom Tab
4. Teacher completes payment on Paystack's page
5. Paystack sends a webhook to the Supabase Edge Function `paystack-webhook`
6. Webhook verifies the payment and updates `subscriptions` table
7. App polls or listens via Supabase Realtime for subscription status change
8. On confirmation: app navigates back to Plan Selection screen with updated plan badge and success snackbar: "You're now on the {Plan Name} plan 🎉"

**Paystack configuration:**
- Currency: NGN
- Channels: Card, Bank Transfer, USSD (all Paystack defaults)
- Customer email: teacher's registered email
- Reference: `TC-{teacher_id}-{timestamp}`
- Metadata: `{ plan_id, teacher_id }`

---

### 4.3 Subscription Management Screen

**Route:** `settings/subscription`

**Layout:**
- Current plan name + badge
- Renewal date: "Renews {date}" or "No renewal — Free plan"
- AI usage summary widget (from PRD 05, Section 9)
- "Manage Subscription" button → links to Paystack customer portal (v1.1) or shows cancellation flow
- "Cancel Subscription" option (confirmation required)
- "Upgrade / Change Plan" → navigates to Plan Selection screen

**Cancellation flow:**
- Confirmation dialog: "Cancelling will switch you to the Basic plan at the end of your current billing period. Your data is never deleted."
- On confirm: sets `subscriptions.cancel_at_period_end = TRUE`
- Teacher retains paid plan access until `current_period_end`
- At period end: Supabase Edge Function downgrades plan to Basic

---

### 4.4 Plan Enforcement (Inline)

Throughout the app, plan-gated features are handled as follows:

| Context | Basic behaviour |
|---|---|
| AI buttons on Lesson Note screen | Hidden (not visible, not disabled) |
| AI usage widget | Not shown |
| Quota exceeded | Bottom sheet with upgrade CTA |
| Referral reward (AI credits) earned on Basic | Stored; activates if user upgrades |

---

## 5. Backend Architecture

### 5.1 Plans Table (Admin-Configurable)

```sql
CREATE TABLE plans (
  id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name                        TEXT NOT NULL,       -- 'Basic', 'Advanced', 'Premium'
  price_ngn                   INT NOT NULL,        -- Monthly price in Naira (kobo optional)
  is_free                     BOOLEAN DEFAULT FALSE,
  lesson_note_limit           INT,                 -- NULL = unlimited
  mcq_limit                   INT,
  essay_limit                 INT,
  teaching_guide_limit        INT,
  mcq_per_generation          INT DEFAULT 5,
  essay_per_generation        INT DEFAULT 3,
  paystack_plan_code          TEXT,                -- Paystack plan ID for recurring billing
  is_active                   BOOLEAN DEFAULT TRUE,
  created_at                  TIMESTAMPTZ DEFAULT now(),
  updated_at                  TIMESTAMPTZ DEFAULT now()
);
```

### 5.2 Subscriptions Table

```sql
CREATE TABLE subscriptions (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id            UUID REFERENCES auth.users(id) UNIQUE,
  plan_id               UUID REFERENCES plans(id),
  status                TEXT CHECK (status IN ('active', 'cancelled', 'past_due', 'trialing')),
  paystack_customer_id  TEXT,
  paystack_subscription_code TEXT,
  current_period_start  TIMESTAMPTZ,
  current_period_end    TIMESTAMPTZ,
  cancel_at_period_end  BOOLEAN DEFAULT FALSE,
  created_at            TIMESTAMPTZ DEFAULT now(),
  updated_at            TIMESTAMPTZ DEFAULT now()
);
```

### 5.3 Edge Function: `paystack-webhook`

Handles all Paystack webhook events:

```typescript
const event = req.body;

switch (event.event) {
  case 'charge.success':
    await activateSubscription(event.data);
    break;

  case 'subscription.disable':
    await cancelSubscription(event.data.customer.email);
    break;

  case 'invoice.payment_failed':
    await markSubscriptionPastDue(event.data);
    break;
}
```

**Webhook verification:** Paystack signs all events with HMAC SHA512. Edge Function validates `x-paystack-signature` header before processing.

### 5.4 Edge Function: `create-payment-session`

```typescript
// Called by Android app to initiate checkout
const response = await paystack.transaction.initialize({
  email: teacher.email,
  amount: plan.price_ngn * 100,  // Paystack uses kobo
  plan: plan.paystack_plan_code,
  reference: `TC-${teacher.id}-${Date.now()}`,
  metadata: { plan_id: plan.id, teacher_id: teacher.id },
  callback_url: 'https://teacherscompanion.app/payment-success'
});

return { payment_url: response.data.authorization_url };
```

### 5.5 Plan Limit Resolution (used by PRD 05 quota check)

```typescript
// Called by AI Edge Functions before generation
async function getPlanLimits(teacher_id: string) {
  const { data } = await supabase
    .from('subscriptions')
    .select('plans(*)')
    .eq('teacher_id', teacher_id)
    .eq('status', 'active')
    .single();

  return data?.plans ?? getFreePlanLimits();
}
```

---

## 6. Referral Integration with Plans

Referral rewards (defined in full in PRD 07) can grant temporary AI credits that top up a teacher's monthly quota. These are stored separately in `referral_credits` and added to the effective quota limit when computing usage:

```typescript
const effectiveLimit = planLimit + referralCredits.lesson_notes;
```

---

## 7. Plan Downgrade Logic

When a teacher's subscription ends or is cancelled:

1. Supabase Edge Function `process-expired-subscriptions` runs daily at midnight via `pg_cron`
2. Finds all `subscriptions` where `current_period_end < now()` and `status = 'active'`
3. Sets `subscriptions.plan_id` to the Basic plan
4. Sets `subscriptions.status = 'cancelled'`
5. Teacher retains all their data (schools, classes, subjects, syllabus, lesson notes — nothing is deleted)
6. AI buttons hidden on next app launch (plan re-fetched at startup)

---

## 8. Data Model: Full Reference

```sql
-- Add plan reference to profiles for fast access
ALTER TABLE profiles ADD COLUMN plan_id UUID REFERENCES plans(id);
ALTER TABLE profiles ADD COLUMN plan_name TEXT DEFAULT 'Basic';

-- Updated via trigger when subscriptions row changes
CREATE OR REPLACE FUNCTION sync_profile_plan()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE profiles
  SET plan_id = NEW.plan_id,
      plan_name = (SELECT name FROM plans WHERE id = NEW.plan_id)
  WHERE id = NEW.teacher_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_subscription_change
AFTER INSERT OR UPDATE ON subscriptions
FOR EACH ROW EXECUTE FUNCTION sync_profile_plan();
```

---

## 9. RLS Policies

```sql
-- Plans are readable by all authenticated users (needed at app start)
ALTER TABLE plans ENABLE ROW LEVEL SECURITY;
CREATE POLICY "authenticated_read_plans" ON plans FOR SELECT
USING (auth.role() = 'authenticated' AND is_active = TRUE);

-- Subscriptions readable only by the owning teacher
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "teacher_read_own_subscription" ON subscriptions FOR SELECT
USING (teacher_id = auth.uid());

-- Writes only via service role (Edge Functions)
```

---

## 10. Validation & Error States

| Scenario | Handling |
|---|---|
| Payment failed on Paystack | Chrome Custom Tab returns to app; show toast: "Payment unsuccessful. Please try again." |
| Webhook received out of order | Edge Function is idempotent — checks `reference` for duplicates before processing |
| Teacher on past_due plan tries to use AI | Treat as Basic; show banner: "Your payment failed. Update your payment method to restore access." |
| Plans table unavailable at app start | Fallback to locally cached plan; retry silently in background |
| Teacher tries to downgrade mid-period | Show dialog: "You'll switch to {Plan} at the end of your billing period on {date}." |

---

## 11. Acceptance Criteria

- [ ] All three plans display correctly on the Plan Selection screen with accurate feature lists
- [ ] Paystack checkout opens in Chrome Custom Tab and returns correctly on success or failure
- [ ] Subscription status updates in real-time (or within 10 seconds via Realtime listener) after payment
- [ ] AI buttons are hidden for Basic plan users throughout the app
- [ ] Quota limits are read from the `plans` table, not hardcoded in the app
- [ ] Plan downgrade retains all teacher data with no deletions
- [ ] Cancelled subscriptions degrade to Basic at `current_period_end`, not immediately
- [ ] Paystack webhook signature is validated before any DB writes
- [ ] Referral credits correctly top up the effective AI quota

---

*Previous: PRD 05 — AI Features*
*Next: PRD 07 — Referral System*

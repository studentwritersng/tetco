# PRD 07 — Referral System

**Project:** Teacher's Companion Android App
**Version:** 1.0
**Status:** Draft
**Depends on:** PRD 01 — Foundation & Architecture, PRD 06 — Plans, Pricing & Subscription

---

## 1. Overview

This PRD covers the referral system that incentivises existing teachers to invite colleagues to the Teacher's Companion app. Each teacher receives a unique referral code at registration. When a new teacher signs up using that code and completes a qualifying action (defined below), both the referrer and the referee receive a reward. Rewards are in the form of AI generation credits that supplement the teacher's monthly plan quota. The referral system is accessible to all plan tiers, including Basic.

---

## 2. Goals

- Drive organic user acquisition through teacher peer networks
- Reward both referrers and referees to create a two-sided incentive
- Make sharing a referral code frictionless (one tap via native share sheet)
- Track referral status clearly so teachers know when they've earned a reward
- Allow admins to configure reward amounts and qualifying actions from the Admin Panel
- Cap rewards to prevent abuse (max referral credits per month)

---

## 3. User Stories

| ID | As a teacher I want to... | So that... |
|---|---|---|
| US-01 | See my unique referral code and sharing link | I can easily invite colleagues |
| US-02 | Share my referral code via WhatsApp, SMS, or other apps | I can reach colleagues quickly |
| US-03 | See how many people I've referred and their status | I know how many rewards I've earned |
| US-04 | Receive AI credits when someone I refer signs up and qualifies | I get value for inviting others |
| US-05 | Know that the person I referred also gets a reward | I feel good about sharing |
| US-06 | See my total referral credits and how to use them | I understand what I've earned |
| US-07 | Be notified when a referral reward is credited to my account | I'm informed of my progress |

---

## 4. Referral Mechanics

### 4.1 Referral Code Generation

- Each teacher is assigned a unique 8-character alphanumeric code at registration (e.g., `TCH-X7K2`)
- Stored in `profiles.referral_code` (UNIQUE constraint)
- Deep link format: `https://teacherscompanion.app/join?ref=TCH-X7K2`
- Short code entry also supported on the Sign Up screen (teacher types code manually)

### 4.2 Qualifying Action

A referral is considered complete and rewards are issued when the referee:

1. Signs up using the referral code (or link)
2. Completes onboarding (adds at least one school)
3. Adds at least one syllabus topic

> The qualifying threshold is configurable from the Admin Panel (default: "Adds first topic"). In v1.0, email verification alone does not trigger a reward.

### 4.3 Reward Structure (Default — Admin Configurable)

| Reward | Referrer | Referee |
|---|---|---|
| Lesson Note Credits | +5 | +3 |
| MCQ Credits | +3 | +2 |
| Essay Credits | +3 | +2 |
| Teaching Guide Credits | +2 | +1 |

Credits are added to the `referral_credits` table and applied on top of the teacher's monthly plan quota (see PRD 06, Section 6).

### 4.4 Credit Limits (Anti-Abuse)

- Maximum referral credits earned per calendar month: configurable in Admin Panel (default: 3 referrals worth of credits per month)
- Credits do not roll over to the next month — they expire at month end alongside `ai_usage`
- A teacher cannot refer themselves (email + device fingerprint checked)
- Each email address can only be referred once

---

## 5. Screens & UX Flow

### 5.1 Referral Screen

**Route:** `profile/referral`

**Access:** Profile screen → "Refer a Friend"

**Layout:**

```
┌────────────────────────────────────────┐
│ ← Refer & Earn                         │
├────────────────────────────────────────┤
│  🎁 Invite teachers, earn AI credits   │
│                                        │
│  Your referral code                    │
│  ┌──────────────────────────────────┐  │
│  │   TCH-X7K2          [Copy]       │  │
│  └──────────────────────────────────┘  │
│                                        │
│  [📤 Share Your Link]                  │
│                                        │
│  ─────────── How it works ──────────   │
│  1. Share your code with a colleague   │
│  2. They sign up and add their first   │
│     syllabus topic                     │
│  3. You both get free AI credits! 🎉   │
│                                        │
│  ─────── Your Referrals ────────────   │
│  Total referred:    7                  │
│  Qualified:         5                  │
│  Pending:           2                  │
│                                        │
│  ─── Credits Earned This Month ─────   │
│  Lesson Notes     ████░░   +10 extra   │
│  Questions        ██░░░░    +6 extra   │
│  Teaching Guides  ████░░    +4 extra   │
│                                        │
│  ─── Referral History ──────────────   │
│  [Name/email redacted] · Qualified ✓   │
│  [Name/email redacted] · Pending…      │
│  [Name/email redacted] · Qualified ✓   │
└────────────────────────────────────────┘
```

**Actions:**
- "Copy" → copies code to clipboard; shows toast "Code copied!"
- "Share Your Link" → triggers Android native share sheet with pre-composed message (see Section 5.2)
- Referral history rows show redacted email (first 3 characters + `***`) and status badge

---

### 5.2 Pre-Composed Share Message

```
Hey! I've been using Teacher's Companion to plan my lessons and generate notes with AI — 
it's been a huge help. Sign up free with my referral code {code} and we both get bonus 
AI credits:

👉 https://teacherscompanion.app/join?ref={code}
```

Shared via Android's `ACTION_SEND` intent — teacher can choose WhatsApp, SMS, Gmail, etc.

---

### 5.3 Sign Up Screen — Referral Code Entry

**Route:** `auth/signup`

**Field added:** "Referral Code (optional)" — text input below the main sign-up form

**Behaviour:**
- If the deep link is opened, the code is pre-populated and the field is read-only
- If entered manually, validated in real-time:
  - Valid code → green checkmark + "Referral by {first name of referrer}"
  - Invalid code → inline error: "This code doesn't exist. Check and try again."
  - Own code entered → inline error: "You can't use your own referral code."
- Code stored against the new teacher's profile at signup: `profiles.referred_by_code`

---

### 5.4 Reward Notification

When a referral qualifies:

**Referrer notification:**
- FCM push: "A teacher you referred has joined! You've earned bonus AI credits."
- In-app notification in the Referral screen history

**Referee notification (on first launch after qualifying):**
- In-app banner at top of Home screen: "Welcome bonus! You've received extra AI credits from your referral. Start generating lesson notes."

---

## 6. Backend Architecture

### 6.1 Referral Processing Trigger

```sql
-- Fires when a new syllabus topic is inserted by a newly referred teacher
CREATE OR REPLACE FUNCTION process_referral_reward()
RETURNS TRIGGER AS $$
DECLARE
  referrer_id UUID;
  referee_profile profiles%ROWTYPE;
BEGIN
  SELECT * INTO referee_profile FROM profiles WHERE id = NEW.teacher_id;

  -- Only process if not already rewarded and has a referring code
  IF referee_profile.referral_reward_issued OR referee_profile.referred_by_code IS NULL THEN
    RETURN NEW;
  END IF;

  -- Find referrer
  SELECT id INTO referrer_id FROM profiles
  WHERE referral_code = referee_profile.referred_by_code;

  IF referrer_id IS NULL THEN RETURN NEW; END IF;

  -- Check topic count for qualifying action
  IF (SELECT COUNT(*) FROM syllabus_topics WHERE teacher_id = NEW.teacher_id) >= 1 THEN

    -- Issue referrer reward
    INSERT INTO referral_credits (teacher_id, month, lesson_note_credits, mcq_credits, essay_credits, guide_credits, referral_source)
    VALUES (referrer_id, TO_CHAR(now(), 'YYYY-MM'), 5, 3, 3, 2, referee_profile.id)
    ON CONFLICT (teacher_id, month) DO UPDATE
    SET lesson_note_credits = referral_credits.lesson_note_credits + 5,
        mcq_credits = referral_credits.mcq_credits + 3,
        essay_credits = referral_credits.essay_credits + 3,
        guide_credits = referral_credits.guide_credits + 2;

    -- Issue referee reward
    INSERT INTO referral_credits (teacher_id, month, lesson_note_credits, mcq_credits, essay_credits, guide_credits, referral_source)
    VALUES (NEW.teacher_id, TO_CHAR(now(), 'YYYY-MM'), 3, 2, 2, 1, referrer_id)
    ON CONFLICT (teacher_id, month) DO UPDATE
    SET lesson_note_credits = referral_credits.lesson_note_credits + 3,
        mcq_credits = referral_credits.mcq_credits + 2,
        essay_credits = referral_credits.essay_credits + 2,
        guide_credits = referral_credits.guide_credits + 1;

    -- Mark referee as rewarded to prevent double-processing
    UPDATE profiles SET referral_reward_issued = TRUE WHERE id = NEW.teacher_id;

    -- Trigger FCM notifications via Edge Function
    PERFORM net.http_post(
      url := 'https://<project>.supabase.co/functions/v1/send-referral-notifications',
      body := json_build_object('referrer_id', referrer_id, 'referee_id', NEW.teacher_id)::text
    );

  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_first_topic_referral_check
AFTER INSERT ON syllabus_topics
FOR EACH ROW EXECUTE FUNCTION process_referral_reward();
```

### 6.2 Referral Code Generation at Signup

```sql
CREATE OR REPLACE FUNCTION generate_referral_code()
RETURNS TRIGGER AS $$
DECLARE
  code TEXT;
  exists BOOLEAN;
BEGIN
  LOOP
    code := 'TCH-' || UPPER(SUBSTRING(MD5(random()::TEXT), 1, 4));
    SELECT EXISTS(SELECT 1 FROM profiles WHERE referral_code = code) INTO exists;
    EXIT WHEN NOT exists;
  END LOOP;

  NEW.referral_code := code;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER assign_referral_code
BEFORE INSERT ON profiles
FOR EACH ROW EXECUTE FUNCTION generate_referral_code();
```

---

## 7. Data Model

```sql
-- Additions to profiles table
ALTER TABLE profiles ADD COLUMN referral_code TEXT UNIQUE;
ALTER TABLE profiles ADD COLUMN referred_by_code TEXT;
ALTER TABLE profiles ADD COLUMN referral_reward_issued BOOLEAN DEFAULT FALSE;

-- Referral credits table
CREATE TABLE referral_credits (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id           UUID REFERENCES auth.users(id),
  month                TEXT NOT NULL,          -- 'YYYY-MM'
  lesson_note_credits  INT DEFAULT 0,
  mcq_credits          INT DEFAULT 0,
  essay_credits        INT DEFAULT 0,
  guide_credits        INT DEFAULT 0,
  referral_source      UUID,                   -- teacher_id of the other party
  created_at           TIMESTAMPTZ DEFAULT now(),
  updated_at           TIMESTAMPTZ DEFAULT now(),
  UNIQUE (teacher_id, month)
);

-- Referral history view (for display on Referral screen)
CREATE VIEW referral_history AS
SELECT
  p_referrer.id AS referrer_id,
  p_referee.id AS referee_id,
  SUBSTRING(p_referee.email, 1, 3) || '***' AS redacted_email,
  p_referee.referral_reward_issued AS qualified,
  p_referee.created_at AS joined_at
FROM profiles p_referee
JOIN profiles p_referrer ON p_referrer.referral_code = p_referee.referred_by_code
WHERE p_referee.referred_by_code IS NOT NULL;
```

---

## 8. RLS Policies

```sql
ALTER TABLE referral_credits ENABLE ROW LEVEL SECURITY;

CREATE POLICY "teacher_view_own_credits" ON referral_credits FOR SELECT
USING (teacher_id = auth.uid());

-- Writes via service role only (trigger + Edge Function)

-- Referral history view — filtered by auth.uid()
CREATE POLICY "teacher_view_referral_history" ON profiles FOR SELECT
USING (referred_by_code = (SELECT referral_code FROM profiles WHERE id = auth.uid()));
```

---

## 9. Referral Leaderboard (Optional — v1.0 if time permits)

A simple leaderboard showing teachers with the most qualified referrals. Displayed on the Referral screen as a collapsible section.

```sql
SELECT
  SUBSTRING(p.email, 1, 3) || '***' AS teacher,
  COUNT(r.id) AS qualified_referrals
FROM profiles p
LEFT JOIN profiles r ON r.referred_by_code = p.referral_code AND r.referral_reward_issued = TRUE
GROUP BY p.id, p.email
ORDER BY qualified_referrals DESC
LIMIT 10;
```

- Only shown if the teacher is in the top 10 or has made at least 1 referral
- Names are redacted (first 3 chars + ***) for privacy

---

## 10. Validation & Error States

| Scenario | Handling |
|---|---|
| Teacher enters own referral code | Inline error on sign up: "You can't use your own referral code." |
| Referral code does not exist | Inline error: "This code doesn't exist. Check and try again." |
| Email already referred | `referred_by_code` is set only at signup — duplicate entry blocked by DB |
| Reward already issued for referee | `referral_reward_issued` flag prevents re-processing |
| Monthly credit cap reached | Trigger checks cap before inserting; excess silently ignored (admin can review in panel) |
| Referrer deletes account | Referee reward already issued; unaffected |

---

## 11. Acceptance Criteria

- [ ] Every teacher receives a unique referral code at registration
- [ ] Referral code is pre-populated when app is opened from a referral deep link
- [ ] Valid referral codes show referrer's first name inline during sign-up
- [ ] Reward is issued only once per referee (idempotent)
- [ ] Both referrer and referee receive correct credit amounts on qualification
- [ ] Referral screen shows accurate referred/qualified/pending counts
- [ ] FCM notification is sent to referrer when a referral qualifies
- [ ] Referral credits are visible in the AI usage widget on Profile
- [ ] Credits expire at month end alongside `ai_usage`
- [ ] Teachers cannot refer themselves

---

*Previous: PRD 06 — Plans, Pricing & Subscription*
*Next: PRD 08 — Admin Web Panel*

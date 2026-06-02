# PRD 04 — Alarm & Notification System

**Project:** Teacher's Companion Android App
**Version:** 1.0
**Status:** Draft
**Depends on:** PRD 01 — Foundation & Architecture, PRD 02 — School & Class Management, PRD 03 — Syllabus Management

---

## 1. Overview

This PRD covers all alarm and notification capabilities within the Teacher's Companion app. Teachers can set personal wake-up alarms, configure daily period reminders that follow their timetable, and receive automated alerts about syllabus gaps (topics with no lesson notes). Notifications are delivered via Android's local alarm system and Firebase Cloud Messaging (FCM) for push notifications triggered by Supabase Edge Functions.

---

## 2. Goals

- Provide a flexible wake-up alarm with repeat scheduling
- Allow teachers to configure period reminders mapped to their daily timetable
- Automatically surface syllabus coverage gaps as push notifications
- Support per-school notification contexts so alerts are correctly scoped
- Give teachers full control to enable, disable, snooze, and delete alarms

---

## 3. User Stories

| ID | As a teacher I want to... | So that... |
|---|---|---|
| US-01 | Set one or more wake-up alarms with custom labels | I never miss the start of my school day |
| US-02 | Configure period reminders for each day of the week | I get notified before each class period begins |
| US-03 | Receive a notification when a topic has no lesson note | I know exactly where my syllabus has gaps |
| US-04 | Choose how far in advance I receive period reminders | I have enough time to prepare |
| US-05 | Snooze or dismiss an alarm from the notification shade | I can manage interruptions quickly |
| US-06 | Enable or disable individual alarms without deleting them | I can pause reminders temporarily |
| US-07 | See all my active alarms and notifications in one place | I have a clear view of my schedule |
| US-08 | Configure which school's timetable drives my period reminders | My reminders match my active school |

---

## 4. Alarm Types

### 4.1 Wake-Up Alarm

A simple time-based alarm, similar to a standard clock app alarm.

**Properties:**
- Label (optional, e.g., "Morning alarm")
- Time (HH:MM)
- Repeat days (Mon – Sun, multi-select; or "Once")
- Sound (system ringtones or silent)
- Vibrate toggle
- Enabled/disabled toggle

**Behaviour:**
- Fires using Android's `AlarmManager` with `RTC_WAKEUP` to wake the device
- Alarm screen presented as a full-screen intent on wake
- Snooze duration: 5 minutes (fixed in v1.0)
- Persisted locally via Room database; synced to Supabase `alarms` table for cross-device restore

---

### 4.2 Period Reminder

A daily notification that fires before a class period starts, based on a teacher-defined timetable.

**Properties:**
- Period name (e.g., "Period 3 – JSS 2 Mathematics")
- Start time (HH:MM)
- Days of the week (multi-select)
- Advance notice (5, 10, 15, 30 minutes before period starts)
- Linked subject (optional — tapping notification opens the subject's syllabus)
- School context (which school this timetable belongs to)
- Enabled/disabled toggle

**Behaviour:**
- Fires as a high-priority Android notification (not a full-screen alarm)
- Notification action buttons: "Open Syllabus" | "Dismiss"
- Each period is scheduled as a separate `AlarmManager` intent
- If the active school changes, period reminders update to reflect the new school context

---

### 4.3 Syllabus Gap Alert

An automated notification that informs the teacher about topics that have no lesson note. This is not a manually created alarm — it is driven by the gap detection query defined in PRD 03, Section 5.3.

**Delivery schedule (configurable in admin panel, default values below):**
- Daily digest: fires every weekday at 07:00 local time
- Immediate: fires when a new topic is added with no lesson note and the teacher has been inactive for more than 2 hours

**Notification content:**
- Title: "You have X uncovered topics"
- Body: Lists up to 3 topic names with their subject and class, e.g.:
  - "Week 5 – Photosynthesis · JSS 2 Biology"
  - "Week 2 – Quadratic Equations · SSS 1 Maths"
- Action button: "Review Now" → opens the gap list screen

**Gap list screen:**
- Accessible from the notification or from Alarms & Notifications settings
- Lists all uncovered topics grouped by school → class → subject
- Each row tappable, navigates to the topic's lesson note screen

**Behaviour:**
- Triggered by Supabase Edge Function scheduled via `pg_cron` (see Section 6)
- FCM push notification payload delivered to the teacher's registered device token
- If the teacher has no uncovered topics, no notification is sent

---

## 5. Screens & UX Flow

### 5.1 Alarms & Notifications Screen

**Route:** `settings/alarms`

**Layout:**
- Top app bar: "Alarms & Reminders"
- Section: **Wake-Up Alarms**
  - List of configured alarms, each showing time, label, repeat days, and enabled toggle
  - FAB or "+ Add Alarm" button
- Section: **Period Reminders**
  - List of period reminders grouped by day
  - FAB or "+ Add Period Reminder" button
- Section: **Syllabus Gap Alerts**
  - Toggle to enable/disable daily gap digest
  - Toggle to enable/disable immediate gap alerts
  - "Review uncovered topics" shortcut link
- Section: **Notification Preferences**
  - Sound toggle
  - Vibration toggle
  - Do Not Disturb override toggle (for wake-up alarms only)

---

### 5.2 Add / Edit Wake-Up Alarm Screen

**Route:** `alarms/add` or `alarms/{id}/edit`

**Fields:**

| Field | Type | Required | Default |
|---|---|---|---|
| Time | Time picker | Yes | 06:00 |
| Label | Text input | No | "Alarm" |
| Repeat | Multi-select chip group (Mon–Sun + Once) | Yes | Once |
| Sound | Sound picker | No | System default |
| Vibrate | Toggle | No | ON |

**Actions:**
- Save → schedules `AlarmManager` intent, inserts to `alarms` table
- Delete (Edit screen only) → cancels `AlarmManager` intent, soft-deletes from `alarms` table
- Toggle on list → enables/disables without deleting; cancels or reschedules `AlarmManager` intent

---

### 5.3 Add / Edit Period Reminder Screen

**Route:** `alarms/periods/add` or `alarms/periods/{id}/edit`

**Fields:**

| Field | Type | Required | Default |
|---|---|---|---|
| Period Name | Text input | Yes | — |
| Start Time | Time picker | Yes | — |
| Days | Multi-select chip group | Yes | Mon–Fri |
| Advance Notice | Dropdown (5, 10, 15, 30 min) | Yes | 10 min |
| Linked Subject | Optional subject picker | No | None |
| School | Auto-populated from active school | Yes | Active school |

**Actions:**
- Save → schedules `AlarmManager` intents for each selected day, inserts to `period_reminders` table
- Delete → cancels all `AlarmManager` intents for this reminder

---

### 5.4 Syllabus Gap List Screen

**Route:** `alarms/gaps`

**Layout:**
- Top app bar: "Uncovered Topics"
- List grouped by: School → Class → Subject
- Each topic row shows: week badge, topic title, term label
- Tap row → navigate to Topic Detail / Lesson Note screen (PRD 03, Section 4.3)
- Empty state: "All your topics have lesson notes. Great job! 🎉"

---

## 6. Backend: Supabase Edge Functions & pg_cron

### 6.1 Daily Gap Digest (Scheduled)

A `pg_cron` job runs every weekday at 07:00 UTC (adjusted for WAT +1 = 08:00 local):

```sql
SELECT cron.schedule(
  'daily-gap-digest',
  '0 6 * * 1-5',  -- 06:00 UTC = 07:00 WAT Mon–Fri
  $$
  SELECT net.http_post(
    url := 'https://<project>.supabase.co/functions/v1/send-gap-digest',
    headers := '{"Authorization": "Bearer <service_role_key>"}'::jsonb
  );
  $$
);
```

**Edge Function: `send-gap-digest`**

```typescript
// Pseudocode
const teachers = await supabase
  .from('profiles')
  .select('id, fcm_token')
  .eq('gap_digest_enabled', true);

for (const teacher of teachers) {
  const gaps = await getUncoveredTopics(teacher.id); // PRD 03 gap query
  if (gaps.length === 0) continue;

  await sendFCMNotification({
    token: teacher.fcm_token,
    title: `You have ${gaps.length} uncovered topics`,
    body: gaps.slice(0, 3).map(g => `${g.title} · ${g.class_name}`).join('\n'),
    data: { screen: 'gaps' }
  });
}
```

### 6.2 FCM Integration

- Teacher's FCM device token stored in `profiles.fcm_token`
- Token refreshed on app launch and whenever FCM issues a new token
- Edge Function uses Firebase Admin SDK (via Deno HTTP calls) to send notifications
- Notification channel on Android: `SYLLABUS_ALERTS` (importance: DEFAULT)

### 6.3 Local AlarmManager Scheduling

All wake-up alarms and period reminders are scheduled locally via Android's `AlarmManager`:

```kotlin
val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

alarmManager.setAlarmClock(
    AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
    pendingIntent
)
```

- Alarms survive device reboots via a `BroadcastReceiver` that re-registers on `BOOT_COMPLETED`
- Period reminders use `setRepeating` for weekly recurrence per day
- All alarm IDs stored in Room DB to allow cancellation

---

## 7. Data Model

```sql
-- Alarms table (synced from Room DB)
CREATE TABLE alarms (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id  UUID REFERENCES auth.users(id),
  label       TEXT,
  time        TIME NOT NULL,
  repeat_days TEXT[], -- e.g. ['MON', 'WED', 'FRI']
  sound       TEXT,
  vibrate     BOOLEAN DEFAULT TRUE,
  enabled     BOOLEAN DEFAULT TRUE,
  created_at  TIMESTAMPTZ DEFAULT now(),
  updated_at  TIMESTAMPTZ DEFAULT now(),
  deleted_at  TIMESTAMPTZ
);

-- Period reminders table
CREATE TABLE period_reminders (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id      UUID REFERENCES auth.users(id),
  school_id       UUID REFERENCES schools(id),
  subject_id      UUID REFERENCES subjects(id),
  name            TEXT NOT NULL,
  start_time      TIME NOT NULL,
  repeat_days     TEXT[],
  advance_minutes INT DEFAULT 10,
  enabled         BOOLEAN DEFAULT TRUE,
  created_at      TIMESTAMPTZ DEFAULT now(),
  updated_at      TIMESTAMPTZ DEFAULT now(),
  deleted_at      TIMESTAMPTZ
);

-- Notification preferences (stored on profiles table)
ALTER TABLE profiles ADD COLUMN gap_digest_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE profiles ADD COLUMN gap_immediate_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE profiles ADD COLUMN fcm_token TEXT;
ALTER TABLE profiles ADD COLUMN notification_sound BOOLEAN DEFAULT TRUE;
ALTER TABLE profiles ADD COLUMN notification_vibrate BOOLEAN DEFAULT TRUE;
```

---

## 8. RLS Policies

```sql
ALTER TABLE alarms ENABLE ROW LEVEL SECURITY;

CREATE POLICY "teacher_manage_alarms" ON alarms FOR ALL
USING (teacher_id = auth.uid());

ALTER TABLE period_reminders ENABLE ROW LEVEL SECURITY;

CREATE POLICY "teacher_manage_period_reminders" ON period_reminders FOR ALL
USING (teacher_id = auth.uid());
```

---

## 9. Permissions (Android Manifest)

```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

- On Android 13+, request `POST_NOTIFICATIONS` at runtime during onboarding
- On Android 12+, request `SCHEDULE_EXACT_ALARM` via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` settings intent

---

## 10. Validation & Error States

| Scenario | Handling |
|---|---|
| Teacher denies notification permission | Show in-app banner: "Enable notifications to use alarms" with settings deep link |
| Exact alarm permission denied (Android 12+) | Show dialog explaining why it's needed; degrade to inexact alarm if denied |
| FCM token missing | Retry token registration on next app launch; log silently |
| No internet on gap digest trigger | Edge Function retries up to 3 times with exponential back-off |
| Device restarts, alarms lost | `BOOT_COMPLETED` receiver re-registers all enabled alarms from Room DB |
| Teacher disables gap_digest_enabled | Remove from `pg_cron` notification targets immediately |

---

## 11. Acceptance Criteria

- [ ] Wake-up alarm fires at the correct time even when the app is closed
- [ ] Alarms survive device reboots
- [ ] Period reminders fire the configured number of minutes before period start
- [ ] Tapping a period reminder notification opens the correct subject's syllabus
- [ ] Daily gap digest is only sent on weekdays and only when gaps exist
- [ ] Syllabus gap list is accurately populated from the PRD 03 gap query
- [ ] Enabling/disabling an alarm does not delete it
- [ ] FCM token is refreshed on each app launch
- [ ] All notification preferences persist across app restarts
- [ ] Permission denial is gracefully handled with clear user guidance

---

*Previous: PRD 03 — Syllabus Management*
*Next: PRD 05 — AI Features*

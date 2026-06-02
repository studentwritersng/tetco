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

CREATE TABLE plans (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL UNIQUE,
  price_ngn INT NOT NULL DEFAULT 0,
  is_free BOOLEAN DEFAULT FALSE,
  lesson_note_limit INT,
  mcq_limit INT,
  essay_limit INT,
  teaching_guide_limit INT,
  mcq_per_generation INT DEFAULT 5,
  essay_per_generation INT DEFAULT 3,
  paystack_plan_code TEXT,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO plans (name, price_ngn, is_free, lesson_note_limit, mcq_limit, essay_limit, teaching_guide_limit, mcq_per_generation, essay_per_generation) VALUES
  ('Basic', 0, TRUE, 0, 0, 0, 0, 0, 0),
  ('Advanced', 2500, FALSE, 20, 15, 15, 10, 5, 3),
  ('Premium', 5000, FALSE, NULL, NULL, NULL, NULL, 10, 5);

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

CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name TEXT,
  phone TEXT,
  avatar_url TEXT,
  referral_code TEXT UNIQUE NOT NULL,
  referred_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
  plan TEXT NOT NULL DEFAULT 'basic' CHECK (plan IN ('basic', 'advanced', 'premium')),
  plan_expires_at TIMESTAMPTZ,
  ai_credits_used INT NOT NULL DEFAULT 0,
  fcm_token TEXT,
  active_school_id UUID,
  gap_digest_enabled BOOLEAN DEFAULT TRUE,
  gap_immediate_enabled BOOLEAN DEFAULT TRUE,
  notification_sound BOOLEAN DEFAULT TRUE,
  notification_vibrate BOOLEAN DEFAULT TRUE,
  plan_id UUID REFERENCES plans(id) ON DELETE SET NULL,
  plan_name TEXT DEFAULT 'Basic',
  referred_by_code TEXT,
  referral_reward_issued BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

ALTER TABLE profiles
  ADD CONSTRAINT profiles_active_school_id_fkey
  FOREIGN KEY (active_school_id) REFERENCES schools(id) ON DELETE SET NULL;

CREATE TABLE school_classes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  class_level_id INT NOT NULL REFERENCES class_levels(id),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ,
  UNIQUE (school_id, class_level_id)
);

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

CREATE TABLE questions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  lesson_note_id UUID NOT NULL REFERENCES lesson_notes(id) ON DELETE CASCADE,
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('mcq', 'essay')),
  question_text TEXT NOT NULL,
  options JSONB,
  correct_answer TEXT,
  answer_guide TEXT,
  marks INT,
  display_order INT DEFAULT 0,
  ai_generated BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE alarms (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('wake_up', 'period', 'syllabus_gap', 'custom')),
  label TEXT,
  time TIME NOT NULL,
  days_of_week INT[] DEFAULT '{1,2,3,4,5}',
  is_active BOOLEAN DEFAULT TRUE,
  sound_url TEXT,
  vibrate BOOLEAN DEFAULT TRUE,
  school_id UUID REFERENCES schools(id) ON DELETE SET NULL,
  subject_id UUID REFERENCES subjects(id) ON DELETE SET NULL,
  advance_minutes INT DEFAULT 0,
  metadata JSONB,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE period_reminders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  school_id UUID REFERENCES schools(id) ON DELETE SET NULL,
  subject_id UUID REFERENCES subjects(id) ON DELETE SET NULL,
  name TEXT NOT NULL,
  start_time TIME NOT NULL,
  repeat_days TEXT[],
  advance_minutes INT DEFAULT 10,
  is_enabled BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE subscriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  plan_id UUID NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'cancelled', 'past_due', 'trialing')),
  paystack_customer_id TEXT,
  paystack_subscription_code TEXT,
  current_period_start TIMESTAMPTZ,
  current_period_end TIMESTAMPTZ,
  cancel_at_period_end BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE ai_usage (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  month TEXT NOT NULL,
  lesson_notes_generated INT DEFAULT 0,
  questions_generated INT DEFAULT 0,
  teaching_guides_generated INT DEFAULT 0,
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (teacher_id, month)
);

CREATE TABLE ai_usage_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  feature TEXT NOT NULL CHECK (feature IN ('lesson_note', 'mcq', 'essay', 'teaching_guide')),
  tokens_used INT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE referrals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  referrer_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  referred_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  reward_granted BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (referred_id)
);

CREATE TABLE referral_credits (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  month TEXT NOT NULL,
  lesson_note_credits INT DEFAULT 0,
  mcq_credits INT DEFAULT 0,
  essay_credits INT DEFAULT 0,
  guide_credits INT DEFAULT 0,
  referral_source UUID,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (teacher_id, month)
);

CREATE TABLE faq_categories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  icon TEXT,
  display_order INT DEFAULT 0,
  is_visible BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE faq_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  category_id UUID NOT NULL REFERENCES faq_categories(id) ON DELETE CASCADE,
  question TEXT NOT NULL,
  answer TEXT NOT NULL,
  display_order INT DEFAULT 0,
  is_visible BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE faq_feedback (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  item_id UUID NOT NULL REFERENCES faq_items(id) ON DELETE CASCADE,
  teacher_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  helpful BOOLEAN NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_referral_code()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.referral_code IS NULL THEN
    LOOP
      NEW.referral_code := 'TCH-' || UPPER(SUBSTRING(MD5(RANDOM()::TEXT || CLOCK_TIMESTAMP()::TEXT) FROM 1 FOR 4));
      EXIT WHEN NOT EXISTS (SELECT 1 FROM profiles WHERE referral_code = NEW.referral_code);
    END LOOP;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO profiles (id, full_name, referral_code)
  VALUES (
    NEW.id,
    NEW.raw_user_meta_data->>'full_name',
    'TCH-' || UPPER(SUBSTRING(MD5(RANDOM()::TEXT || CLOCK_TIMESTAMP()::TEXT) FROM 1 FOR 4))
  )
  ON CONFLICT (id) DO NOTHING;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

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
    WHERE id = COALESCE(NEW.syllabus_topic_id, OLD.syllabus_topic_id);
  END IF;
  IF TG_OP = 'DELETE' THEN
    RETURN OLD;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION process_referral_reward()
RETURNS TRIGGER AS $$
DECLARE
  referrer_id UUID;
  ref_code TEXT;
BEGIN
  IF NEW.referred_by_code IS NOT NULL AND NEW.referral_reward_issued = FALSE THEN
    SELECT id, referral_code INTO referrer_id, ref_code FROM profiles WHERE referral_code = NEW.referred_by_code;
    IF referrer_id IS NULL OR referrer_id = NEW.id THEN
      RETURN NEW;
    END IF;

    IF (SELECT COUNT(*) FROM syllabus_topics WHERE teacher_id = NEW.id AND deleted_at IS NULL) >= 1 THEN
      INSERT INTO referrals (referrer_id, referred_id, reward_granted) VALUES (referrer_id, NEW.id, TRUE)
      ON CONFLICT (referred_id) DO NOTHING;

      INSERT INTO referral_credits (teacher_id, month, lesson_note_credits, mcq_credits, essay_credits, guide_credits, referral_source)
      VALUES (referrer_id, TO_CHAR(now(), 'YYYY-MM'), 5, 3, 3, 2, NEW.id)
      ON CONFLICT (teacher_id, month) DO UPDATE
      SET lesson_note_credits = referral_credits.lesson_note_credits + 5,
          mcq_credits = referral_credits.mcq_credits + 3,
          essay_credits = referral_credits.essay_credits + 3,
          guide_credits = referral_credits.guide_credits + 2;

      INSERT INTO referral_credits (teacher_id, month, lesson_note_credits, mcq_credits, essay_credits, guide_credits, referral_source)
      VALUES (NEW.id, TO_CHAR(now(), 'YYYY-MM'), 3, 2, 2, 1, referrer_id)
      ON CONFLICT (teacher_id, month) DO UPDATE
      SET lesson_note_credits = referral_credits.lesson_note_credits + 3,
          mcq_credits = referral_credits.mcq_credits + 2,
          essay_credits = referral_credits.essay_credits + 2,
          guide_credits = referral_credits.guide_credits + 1;

      UPDATE profiles SET referral_reward_issued = TRUE WHERE id = NEW.id;
    END IF;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION sync_profile_plan()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE profiles
  SET plan_id = NEW.plan_id,
      plan_name = (SELECT name FROM plans WHERE id = NEW.plan_id),
      plan = LOWER((SELECT name FROM plans WHERE id = NEW.plan_id)),
      plan_expires_at = NEW.current_period_end
  WHERE id = NEW.teacher_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER set_updated_at_profiles
  BEFORE UPDATE ON profiles
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER set_updated_at_schools
  BEFORE UPDATE ON schools
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER set_updated_at_subjects
  BEFORE UPDATE ON subjects
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER set_updated_at_syllabus_topics
  BEFORE UPDATE ON syllabus_topics
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER set_updated_at_lesson_notes
  BEFORE UPDATE ON lesson_notes
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER set_updated_at_alarms
  BEFORE UPDATE ON alarms
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER set_updated_at_period_reminders
  BEFORE UPDATE ON period_reminders
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER set_updated_at_plans
  BEFORE UPDATE ON plans
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER set_updated_at_subscriptions
  BEFORE UPDATE ON subscriptions
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER set_updated_at_faq_items
  BEFORE UPDATE ON faq_items
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER set_updated_at_referral_credits
  BEFORE UPDATE ON referral_credits
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER set_updated_at_plan_configs
  BEFORE UPDATE ON plan_configs
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION handle_new_user();

CREATE TRIGGER generate_referral_code_trigger
  BEFORE INSERT ON profiles
  FOR EACH ROW EXECUTE FUNCTION generate_referral_code();

CREATE TRIGGER sync_has_lesson_note_trigger
  AFTER INSERT OR UPDATE OR DELETE ON lesson_notes
  FOR EACH ROW EXECUTE FUNCTION sync_has_lesson_note();

CREATE TRIGGER process_referral_reward_trigger
  AFTER UPDATE ON profiles
  FOR EACH ROW
  WHEN (NEW.referred_by_code IS NOT NULL AND NEW.referral_reward_issued = FALSE)
  EXECUTE FUNCTION process_referral_reward();

CREATE TRIGGER on_subscription_change
  AFTER INSERT OR UPDATE ON subscriptions
  FOR EACH ROW EXECUTE FUNCTION sync_profile_plan();

ALTER TABLE class_levels ENABLE ROW LEVEL SECURITY;
ALTER TABLE plan_configs ENABLE ROW LEVEL SECURITY;
ALTER TABLE plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE schools ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE syllabus_topics ENABLE ROW LEVEL SECURITY;
ALTER TABLE lesson_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE alarms ENABLE ROW LEVEL SECURITY;
ALTER TABLE period_reminders ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_usage ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_usage_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE referrals ENABLE ROW LEVEL SECURITY;
ALTER TABLE referral_credits ENABLE ROW LEVEL SECURITY;
ALTER TABLE faq_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE faq_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE faq_feedback ENABLE ROW LEVEL SECURITY;

CREATE POLICY "public_read_class_levels" ON class_levels FOR SELECT USING (TRUE);
CREATE POLICY "public_read_plan_configs" ON plan_configs FOR SELECT USING (TRUE);
CREATE POLICY "authenticated_read_plans" ON plans FOR SELECT USING (auth.role() = 'authenticated' AND is_active = TRUE);

CREATE POLICY "teacher_select_profiles" ON profiles FOR SELECT USING (id = auth.uid() AND deleted_at IS NULL);
CREATE POLICY "teacher_insert_profiles" ON profiles FOR INSERT WITH CHECK (id = auth.uid());
CREATE POLICY "teacher_update_profiles" ON profiles FOR UPDATE USING (id = auth.uid());

CREATE POLICY "teacher_select_schools" ON schools FOR SELECT USING (teacher_id = auth.uid() AND deleted_at IS NULL);
CREATE POLICY "teacher_insert_schools" ON schools FOR INSERT WITH CHECK (teacher_id = auth.uid());
CREATE POLICY "teacher_update_schools" ON schools FOR UPDATE USING (teacher_id = auth.uid());

CREATE POLICY "teacher_select_school_classes" ON school_classes FOR SELECT USING (teacher_id = auth.uid() AND deleted_at IS NULL);
CREATE POLICY "teacher_insert_school_classes" ON school_classes FOR INSERT WITH CHECK (teacher_id = auth.uid());
CREATE POLICY "teacher_update_school_classes" ON school_classes FOR UPDATE USING (teacher_id = auth.uid());

CREATE POLICY "teacher_select_subjects" ON subjects FOR SELECT USING (teacher_id = auth.uid() AND deleted_at IS NULL);
CREATE POLICY "teacher_insert_subjects" ON subjects FOR INSERT WITH CHECK (teacher_id = auth.uid());
CREATE POLICY "teacher_update_subjects" ON subjects FOR UPDATE USING (teacher_id = auth.uid());

CREATE POLICY "teacher_select_topics" ON syllabus_topics FOR SELECT USING (teacher_id = auth.uid() AND deleted_at IS NULL);
CREATE POLICY "teacher_insert_topics" ON syllabus_topics FOR INSERT WITH CHECK (teacher_id = auth.uid());
CREATE POLICY "teacher_update_topics" ON syllabus_topics FOR UPDATE USING (teacher_id = auth.uid());

CREATE POLICY "teacher_select_notes" ON lesson_notes FOR SELECT USING (teacher_id = auth.uid() AND deleted_at IS NULL);
CREATE POLICY "teacher_insert_notes" ON lesson_notes FOR INSERT WITH CHECK (teacher_id = auth.uid());
CREATE POLICY "teacher_update_notes" ON lesson_notes FOR UPDATE USING (teacher_id = auth.uid());

CREATE POLICY "teacher_select_questions" ON questions FOR SELECT USING (teacher_id = auth.uid() AND deleted_at IS NULL);
CREATE POLICY "teacher_insert_questions" ON questions FOR INSERT WITH CHECK (teacher_id = auth.uid());
CREATE POLICY "teacher_update_questions" ON questions FOR UPDATE USING (teacher_id = auth.uid());

CREATE POLICY "teacher_select_alarms" ON alarms FOR SELECT USING (teacher_id = auth.uid() AND deleted_at IS NULL);
CREATE POLICY "teacher_insert_alarms" ON alarms FOR INSERT WITH CHECK (teacher_id = auth.uid());
CREATE POLICY "teacher_update_alarms" ON alarms FOR UPDATE USING (teacher_id = auth.uid());

CREATE POLICY "teacher_select_period_reminders" ON period_reminders FOR SELECT USING (teacher_id = auth.uid() AND deleted_at IS NULL);
CREATE POLICY "teacher_insert_period_reminders" ON period_reminders FOR INSERT WITH CHECK (teacher_id = auth.uid());
CREATE POLICY "teacher_update_period_reminders" ON period_reminders FOR UPDATE USING (teacher_id = auth.uid());

CREATE POLICY "teacher_read_own_subscription" ON subscriptions FOR SELECT USING (teacher_id = auth.uid());

CREATE POLICY "teacher_view_own_usage" ON ai_usage FOR SELECT USING (teacher_id = auth.uid());
CREATE POLICY "teacher_view_own_logs" ON ai_usage_logs FOR SELECT USING (teacher_id = auth.uid());

CREATE POLICY "referrals_select" ON referrals FOR SELECT USING (referrer_id = auth.uid() OR referred_id = auth.uid());

CREATE POLICY "teacher_view_own_credits" ON referral_credits FOR SELECT USING (teacher_id = auth.uid());

CREATE POLICY "authenticated_read_faq_categories" ON faq_categories FOR SELECT USING (auth.role() = 'authenticated' AND is_visible = TRUE);
CREATE POLICY "authenticated_read_faq_items" ON faq_items FOR SELECT USING (auth.role() = 'authenticated' AND is_visible = TRUE);

CREATE POLICY "teacher_insert_faq_feedback" ON faq_feedback FOR INSERT WITH CHECK (teacher_id = auth.uid());
CREATE POLICY "teacher_select_faq_feedback" ON faq_feedback FOR SELECT USING (teacher_id = auth.uid());

INSERT INTO storage.buckets (id, name, public) VALUES ('school-assets', 'school-assets', FALSE);
INSERT INTO storage.buckets (id, name, public) VALUES ('avatars', 'avatars', FALSE);

CREATE POLICY "teacher_access_school_assets_insert" ON storage.objects FOR INSERT
  WITH CHECK (bucket_id = 'school-assets' AND auth.uid()::TEXT = (storage.foldername(name))[1]);

CREATE POLICY "teacher_access_school_assets_select" ON storage.objects FOR SELECT
  USING (bucket_id = 'school-assets' AND auth.uid()::TEXT = (storage.foldername(name))[1]);

CREATE POLICY "teacher_access_school_assets_update" ON storage.objects FOR UPDATE
  USING (bucket_id = 'school-assets' AND auth.uid()::TEXT = (storage.foldername(name))[1])
  WITH CHECK (bucket_id = 'school-assets' AND auth.uid()::TEXT = (storage.foldername(name))[1]);

CREATE POLICY "teacher_access_school_assets_delete" ON storage.objects FOR DELETE
  USING (bucket_id = 'school-assets' AND auth.uid()::TEXT = (storage.foldername(name))[1]);

CREATE POLICY "teacher_access_avatars_insert" ON storage.objects FOR INSERT
  WITH CHECK (bucket_id = 'avatars' AND auth.uid()::TEXT = (storage.foldername(name))[1]);

CREATE POLICY "teacher_access_avatars_select" ON storage.objects FOR SELECT
  USING (bucket_id = 'avatars' AND auth.uid()::TEXT = (storage.foldername(name))[1]);

CREATE POLICY "teacher_access_avatars_update" ON storage.objects FOR UPDATE
  USING (bucket_id = 'avatars' AND auth.uid()::TEXT = (storage.foldername(name))[1])
  WITH CHECK (bucket_id = 'avatars' AND auth.uid()::TEXT = (storage.foldername(name))[1]);

CREATE POLICY "teacher_access_avatars_delete" ON storage.objects FOR DELETE
  USING (bucket_id = 'avatars' AND auth.uid()::TEXT = (storage.foldername(name))[1]);

CREATE VIEW referral_history AS
SELECT
  p_referrer.id AS referrer_id,
  p_referee.id AS referee_id,
  SUBSTRING(u_referee.email, 1, 3) || '***' AS redacted_email,
  p_referee.referral_reward_issued AS qualified,
  p_referee.created_at AS joined_at
FROM profiles p_referee
JOIN profiles p_referrer ON p_referrer.referral_code = p_referee.referred_by_code
LEFT JOIN auth.users u_referee ON u_referee.id = p_referee.id
WHERE p_referee.referred_by_code IS NOT NULL;

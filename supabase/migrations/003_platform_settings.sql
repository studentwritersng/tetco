-- Platform Settings: fully configurable from admin dashboard

CREATE TABLE IF NOT EXISTS platform_settings (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  category TEXT NOT NULL,
  key TEXT NOT NULL UNIQUE,
  label TEXT NOT NULL,
  description TEXT,
  value JSONB NOT NULL DEFAULT '""'::jsonb,
  type TEXT NOT NULL DEFAULT 'string',
  is_public BOOLEAN DEFAULT false,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE platform_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow service_role full access" ON platform_settings
  USING (true)
  WITH CHECK (true);

-- Seed default settings
INSERT INTO platform_settings (category, key, label, description, value, type, is_public) VALUES

-- ===== REFERRAL SETTINGS =====
('referral', 'referral.max_discount_percent', 'Maximum Discount %',
 'Maximum total discount a user can earn through referrals',
 '55', 'number', false),

('referral', 'referral.discount_tiers', 'Discount Tiers',
 'Each tier: how many referrals needed and discount % added',
 '[{"referrals":1,"discount":10},{"referrals":2,"discount":9},{"referrals":3,"discount":8},{"referrals":4,"discount":7},{"referrals":5,"discount":6},{"referrals":6,"discount":5},{"referrals":7,"discount":4},{"referrals":8,"discount":3},{"referrals":9,"discount":2},{"referrals":10,"discount":1}]',
 'tiers', false),

('referral', 'referral.reward_credits', 'Reward AI Credits',
 'Number of free AI credits given when referral reward is issued',
 '5', 'number', false),

('referral', 'referral.qualification_days', 'Qualification Period (Days)',
 'Days within which the referee must subscribe for referrer to qualify',
 '30', 'number', false),

('referral', 'referral.referee_discount_percent', 'Referee Welcome Discount %',
 'Discount % the new user gets when signing up via referral',
 '10', 'number', false),

-- ===== AI / GENERATION SETTINGS =====
('ai', 'ai.default_model', 'Default AI Model',
 'Primary OpenRouter model for content generation',
 '"openrouter/free"', 'string', false),

('ai', 'ai.fallback_model', 'Fallback AI Model',
 'Model used if the primary model fails',
 '"openai/gpt-4o-mini"', 'string', false),

('ai', 'ai.default_temperature', 'Generation Temperature',
 'Creativity level 0.0 (precise) to 1.0 (creative)',
 '0.7', 'number', false),

('ai', 'ai.max_lesson_note_length', 'Max Lesson Note Length (chars)',
 'Maximum character count for generated lesson notes',
 '8000', 'number', false),

('ai', 'ai.max_teaching_guide_length', 'Max Teaching Guide Length (chars)',
 'Maximum character count for generated teaching guides',
 '4000', 'number', false),

('ai', 'ai.class_level_guidance', 'Class Level Differentiation Guidance',
 'JSON: per-class-level instructions fed to the AI model',
 '{"early_childhood":"Use very simple words, songs, pictures and hands-on activities. Keep activities under 10 minutes.","primary":"Use relatable everyday examples. Include group activities and simple exercises.","junior_secondary":"Introduce abstract concepts gradually. Use real-world applications and projects.","senior_secondary":"Focus on critical thinking, exam-style questions and detailed explanations.","tertiary":"Advanced concepts with research-based assignments and analytical exercises."}',
 'json', true),

('ai', 'ai.default_mcq_count', 'Default MCQ Count per Generation',
 'How many multiple-choice questions to generate by default',
 '5', 'number', false),

('ai', 'ai.default_essay_count', 'Default Essay Count per Generation',
 'How many essay questions to generate by default',
 '3', 'number', false),

('ai', 'ai.max_mcq_per_generation', 'Max MCQ per Generation',
 'Upper limit on MCQs per single generation request',
 '20', 'number', false),

('ai', 'ai.max_essay_per_generation', 'Max Essay per Generation',
 'Upper limit on essays per single generation request',
 '10', 'number', false),

-- ===== NOTIFICATION SETTINGS =====
('notifications', 'notifications.default_sound', 'Default Notification Sound',
 'Filename of the default notification sound (in res/raw/)',
 '"teta.wav"', 'string', false),

('notifications', 'notifications.gap_digest_frequency', 'Gap Digest Cron Schedule',
 'Cron expression for sending weekly gap digests',
 '"0 8 * * 1"', 'string', false),

('notifications', 'notifications.gap_digest_enabled', 'Gap Digest Enabled',
 'Allow weekly gap summary notifications by default for new users',
 'true', 'boolean', true),

('notifications', 'notifications.gap_immediate_enabled', 'Gap Immediate Alerts',
 'Allow real-time gap alerts by default for new users',
 'true', 'boolean', true),

('notifications', 'notifications.fcm_topic_prefix', 'FCM Topic Prefix',
 'Prefix used for Firebase Cloud Messaging topic names',
 '"tc_"', 'string', false),

-- ===== GENERAL PLATFORM SETTINGS =====
('general', 'general.app_name', 'Application Name',
 'Display name shown throughout the app',
 '"Teacher''s Companion"', 'string', false),

('general', 'general.support_email', 'Support Email',
 'Contact email for user support',
 '"support@teacherscompanion.app"', 'string', true),

('general', 'general.privacy_email', 'Privacy / DPO Email',
 'Contact for privacy-related inquiries',
 '"privacy@teacherscompanion.app"', 'string', false),

('general', 'general.terms_url', 'Terms of Service URL',
 'Link to the terms and conditions page',
 '"https://teacherscompanion.app/terms"', 'string', true),

('general', 'general.privacy_url', 'Privacy Policy URL',
 'Link to the privacy policy page',
 '"https://teacherscompanion.app/privacy"', 'string', true),

('general', 'general.max_schools_per_user', 'Max Schools per User',
 'How many schools a single teacher account can create',
 '5', 'number', false),

('general', 'general.max_classes_per_school', 'Max Classes per School',
 'How many classes a single school can have',
 '20', 'number', false),

('general', 'general.max_subjects_per_class', 'Max Subjects per Class',
 'How many subjects a single class can have',
 '15', 'number', false),

-- ===== PRICING / SUBSCRIPTION SETTINGS =====
('pricing', 'pricing.currency_symbol', 'Currency Symbol',
 'Display symbol for prices',
 '"₦"', 'string', true),

('pricing', 'pricing.currency_code', 'Currency Code',
 'ISO currency code for transactions',
 '"NGN"', 'string', false),

('pricing', 'pricing.vat_percent', 'VAT / Tax %',
 'Value Added Tax percentage applied to subscriptions',
 '7.5', 'number', false),

('pricing', 'pricing.free_trial_days', 'Free Trial Duration (Days)',
 'How many days a new user gets free access to premium features',
 '14', 'number', false),

('pricing', 'pricing.grace_period_days', 'Expiry Grace Period (Days)',
 'Days after subscription expiry before access is fully revoked',
 '3', 'number', false),

('pricing', 'pricing.min_plan_price', 'Minimum Plan Price (NGN)',
 'Lowest allowed price for any paid plan',
 '500', 'number', false),

('pricing', 'pricing.max_plan_price', 'Maximum Plan Price (NGN)',
 'Highest allowed price for any paid plan',
 '50000', 'number', false),

('pricing', 'pricing.auto_renewal_default', 'Auto-Renewal Default',
 'Whether auto-renewal is enabled by default for new subscriptions',
 'true', 'boolean', false),

('pricing', 'pricing.refund_cutoff_days', 'Refund Cutoff (Days)',
 'Days within which a cancellation qualifies for a refund',
 '7', 'number', false),

('pricing', 'pricing.display_tax_inclusive', 'Display Prices Tax-Inclusive',
 'If true, prices shown include VAT; if false, VAT is added at checkout',
 'true', 'boolean', false),

-- ===== FEATURE FLAGS =====
('feature_flags', 'feature.referral_enabled', 'Referral System',
 'Enable or disable the referral programme',
 'true', 'boolean', false),

('feature_flags', 'feature.ai_generation_enabled', 'AI Content Generation',
 'Enable or disable all AI-powered content generation',
 'true', 'boolean', false),

('feature_flags', 'feature.lesson_notes_enabled', 'Lesson Notes',
 'Enable or disable lesson note creation',
 'true', 'boolean', false),

('feature_flags', 'feature.questions_enabled', 'Question Generation',
 'Enable or disable MCQ and essay question generation',
 'true', 'boolean', false),

('feature_flags', 'feature.gap_analysis_enabled', 'Gap Analysis',
 'Enable or disable the gap analysis / syllabus tracking feature',
 'true', 'boolean', false),

('feature_flags', 'feature.offline_mode_enabled', 'Offline Mode',
 'Enable or disable offline-first architecture',
 'true', 'boolean', false),

('feature_flags', 'feature.paystack_enabled', 'Paystack Payments',
 'Enable or disable Paystack payment integration',
 'true', 'boolean', false),

('feature_flags', 'feature.multi_school_enabled', 'Multi-School Support',
 'Allow teachers to manage multiple schools',
 'true', 'boolean', false);

CREATE OR REPLACE FUNCTION update_platform_settings_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_platform_settings_updated_at ON platform_settings;
CREATE TRIGGER trg_platform_settings_updated_at
  BEFORE UPDATE ON platform_settings
  FOR EACH ROW EXECUTE FUNCTION update_platform_settings_timestamp();

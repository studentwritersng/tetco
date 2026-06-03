-- User suspension support
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMPTZ;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS suspended_reason TEXT;

-- Also track who suspended/restored (admin user ID, if needed later)
-- ALTER TABLE profiles ADD COLUMN IF NOT EXISTS suspended_by UUID REFERENCES auth.users(id);

-- Index for fast lookups of suspended users
CREATE INDEX IF NOT EXISTS idx_profiles_suspended_at ON profiles(suspended_at);

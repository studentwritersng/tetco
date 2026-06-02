import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    );

    const sql = `
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $func$
BEGIN
  INSERT INTO public.profiles (id, full_name, referral_code)
  VALUES (
    NEW.id,
    NEW.raw_user_meta_data->>'full_name',
    'TCH-' || UPPER(SUBSTRING(MD5(RANDOM()::TEXT || CLOCK_TIMESTAMP()::TEXT) FROM 1 FOR 4))
  )
  ON CONFLICT (id) DO NOTHING;
  RETURN NEW;
END;
$func$;

CREATE OR REPLACE FUNCTION generate_referral_code()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $func$
BEGIN
  IF NEW.referral_code IS NULL THEN
    LOOP
      NEW.referral_code := 'TCH-' || UPPER(SUBSTRING(MD5(RANDOM()::TEXT || CLOCK_TIMESTAMP()::TEXT) FROM 1 FOR 4));
      EXIT WHEN NOT EXISTS (SELECT 1 FROM public.profiles WHERE referral_code = NEW.referral_code);
    END LOOP;
  END IF;
  RETURN NEW;
END;
$func$;

CREATE OR REPLACE FUNCTION sync_has_lesson_note()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $func$
BEGIN
  IF TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND NEW.deleted_at IS NULL) THEN
    UPDATE public.syllabus_topics
    SET has_lesson_note = TRUE
    WHERE id = NEW.syllabus_topic_id;
  ELSIF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE' AND NEW.deleted_at IS NOT NULL) THEN
    UPDATE public.syllabus_topics
    SET has_lesson_note = FALSE
    WHERE id = COALESCE(NEW.syllabus_topic_id, OLD.syllabus_topic_id);
  END IF;
  IF TG_OP = 'DELETE' THEN
    RETURN OLD;
  END IF;
  RETURN NEW;
END;
$func$;

CREATE OR REPLACE FUNCTION process_referral_reward()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $func$
DECLARE
  referrer_id UUID;
  ref_code TEXT;
BEGIN
  IF NEW.referred_by_code IS NOT NULL AND NEW.referral_reward_issued = FALSE THEN
    SELECT id, referral_code INTO referrer_id, ref_code FROM public.profiles WHERE referral_code = NEW.referred_by_code;
    IF referrer_id IS NULL OR referrer_id = NEW.id THEN
      RETURN NEW;
    END IF;

    IF (SELECT COUNT(*) FROM public.syllabus_topics WHERE teacher_id = NEW.id AND deleted_at IS NULL) >= 1 THEN
      INSERT INTO public.referrals (referrer_id, referred_id, reward_granted) VALUES (referrer_id, NEW.id, TRUE)
      ON CONFLICT (referred_id) DO NOTHING;

      INSERT INTO public.referral_credits (teacher_id, month, lesson_note_credits, mcq_credits, essay_credits, guide_credits, referral_source)
      VALUES (referrer_id, TO_CHAR(now(), 'YYYY-MM'), 5, 3, 3, 2, NEW.id)
      ON CONFLICT (teacher_id, month) DO UPDATE
      SET lesson_note_credits = public.referral_credits.lesson_note_credits + 5,
          mcq_credits = public.referral_credits.mcq_credits + 3,
          essay_credits = public.referral_credits.essay_credits + 3,
          guide_credits = public.referral_credits.guide_credits + 2;

      INSERT INTO public.referral_credits (teacher_id, month, lesson_note_credits, mcq_credits, essay_credits, guide_credits, referral_source)
      VALUES (NEW.id, TO_CHAR(now(), 'YYYY-MM'), 3, 2, 2, 1, referrer_id)
      ON CONFLICT (teacher_id, month) DO UPDATE
      SET lesson_note_credits = public.referral_credits.lesson_note_credits + 3,
          mcq_credits = public.referral_credits.mcq_credits + 2,
          essay_credits = public.referral_credits.essay_credits + 2,
          guide_credits = public.referral_credits.guide_credits + 1;

      UPDATE public.profiles SET referral_reward_issued = TRUE WHERE id = NEW.id;
    END IF;
  END IF;
  RETURN NEW;
END;
$func$;

CREATE OR REPLACE FUNCTION sync_profile_plan()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $func$
BEGIN
  UPDATE public.profiles
  SET plan_id = NEW.plan_id,
      plan_name = (SELECT name FROM public.plans WHERE id = NEW.plan_id),
      plan = LOWER((SELECT name FROM public.plans WHERE id = NEW.plan_id)),
      plan_expires_at = NEW.current_period_end
  WHERE id = NEW.teacher_id;
  RETURN NEW;
END;
$func$;
    `;

    // Execute raw SQL via the RPC or db execution
    const { error } = await supabase.rpc("exec_sql", { sql_text: sql });
    
    if (error) {
      // RPC might not exist, try direct SQL execution via raw query
      const { error: sqlError } = await supabase.from("_exec_sql").insert({ sql }).single();
      if (sqlError) {
        // Last resort: try using the pg_ddl extension or raw connection
        return new Response(
          JSON.stringify({ 
            error: sqlError.message,
            hint: "Could not execute SQL via Supabase client. The edge function does not have raw database access."
          }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
        );
      }
    }

    // Verify the fix by reading the function source
    const { data: funcs, error: verifyError } = await supabase
      .from("pg_proc")
      .select("proname, prosrc")
      .in("proname", ["handle_new_user", "generate_referral_code", "sync_has_lesson_note", "process_referral_reward", "sync_profile_plan"]);

    const results = (funcs || []).map((f: any) => ({
      name: f.proname,
      has_public_prefix: f.prosrc?.includes("public.") || false
    }));

    return new Response(
      JSON.stringify({ 
        status: "completed",
        functions_verified: results 
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
    );
  }
});

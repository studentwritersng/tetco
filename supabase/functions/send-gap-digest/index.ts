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

    const { data: teachers } = await supabase
      .from("profiles")
      .select("id, fcm_token")
      .eq("gap_digest_enabled", true);

    if (!teachers || teachers.length === 0) {
      return new Response(JSON.stringify({ sent: 0 }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }

    const firebaseProjectId = Deno.env.get("FIREBASE_PROJECT_ID");
    let sentCount = 0;

    for (const teacher of teachers) {
      if (!teacher.fcm_token) continue;

      const { data: gaps } = await supabase
        .from("syllabus_topics")
        .select("title, term, week_number, subjects(name, school_classes(name, class_levels(name), schools(name)))")
        .eq("teacher_id", teacher.id)
        .eq("has_lesson_note", false)
        .is("deleted_at", null)
        .limit(3);

      if (!gaps || gaps.length === 0) continue;

      const body = gaps.map((g: any) => {
        const subject = g.subjects?.name || "";
        const className = g.subjects?.school_classes?.class_levels?.name || "";
        return `Week ${g.week_number} – ${g.title} · ${className} ${subject}`;
      }).join("\n");

      await fetch(`https://fcm.googleapis.com/v1/projects/${firebaseProjectId}/messages:send`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${Deno.env.get("FCM_ACCESS_TOKEN")}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: teacher.fcm_token,
            notification: {
              title: `You have ${gaps.length} uncovered topics`,
              body,
            },
            data: { screen: "gaps" },
          },
        }),
      });

      sentCount++;
    }

    return new Response(
      JSON.stringify({ sent: sentCount }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
    );
  }
});

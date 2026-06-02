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

    const { referrer_id, referee_id } = await req.json();
    const firebaseProjectId = Deno.env.get("FIREBASE_PROJECT_ID");

    const { data: referrer } = await supabase
      .from("profiles")
      .select("fcm_token")
      .eq("id", referrer_id)
      .single();

    if (referrer?.fcm_token) {
      await fetch(`https://fcm.googleapis.com/v1/projects/${firebaseProjectId}/messages:send`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${Deno.env.get("FCM_ACCESS_TOKEN")}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: referrer.fcm_token,
            notification: {
              title: "A teacher you referred has joined!",
              body: "You've earned bonus AI credits. Check your referral page.",
            },
            data: { screen: "referral" },
          },
        }),
      });
    }

    return new Response(
      JSON.stringify({ success: true }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
    );
  }
});

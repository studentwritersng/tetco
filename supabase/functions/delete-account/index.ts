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

    const authHeader = req.headers.get("authorization");
    if (!authHeader) throw new Error("No authorization header");

    const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""));
    if (!user) throw new Error("Invalid token");

    const { password } = await req.json();

    const supabaseAuth = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!
    );

    const { error: authError } = await supabaseAuth.auth.signInWithPassword({
      email: user.email!,
      password,
    });

    if (authError) {
      return new Response(
        JSON.stringify({ error: "INVALID_PASSWORD" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 401 }
      );
    }

    const { data: sub } = await supabase
      .from("subscriptions")
      .select("paystack_subscription_code")
      .eq("teacher_id", user.id)
      .eq("status", "active")
      .single();

    if (sub?.paystack_subscription_code) {
      const paystackSecret = Deno.env.get("PAYSTACK_SECRET_KEY");
      await fetch(`https://api.paystack.co/subscription/${sub.paystack_subscription_code}/disable`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${paystackSecret}`,
          "Content-Type": "application/json",
        },
      });
    }

    const now = new Date().toISOString();
    const tables = ["schools", "school_classes", "subjects", "syllabus_topics", "lesson_notes", "questions", "alarms", "period_reminders"];

    for (const table of tables) {
      await supabase
        .from(table)
        .update({ deleted_at: now })
        .eq("teacher_id", user.id)
        .is("deleted_at", null);
    }

    await supabase.auth.admin.deleteUser(user.id);

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

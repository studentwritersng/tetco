import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function calculateReferralDiscount(qualifiedCount: number): number {
  let discount = 0;
  for (let i = 1; i <= Math.min(qualifiedCount, 10); i++) {
    discount += (11 - i);
  }
  return Math.min(discount, 55);
}

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

    const body = await req.json();
    const teacherId = body.teacher_id || user.id;

    const { data: referralHistory } = await supabase
      .from("referral_history")
      .select("qualified")
      .eq("referrer_id", teacherId)
      .eq("qualified", true);

    const qualifiedCount = referralHistory?.length || 0;
    const discountPercent = calculateReferralDiscount(qualifiedCount);

    const tiers = [];
    let cumulative = 0;
    for (let i = 1; i <= 10; i++) {
      const incremental = 11 - i;
      cumulative += incremental;
      tiers.push({
        referral_number: i,
        incremental_percent: incremental,
        cumulative_percent: cumulative,
        achieved: i <= qualifiedCount,
      });
    }

    return new Response(
      JSON.stringify({
        discount_percent: discountPercent,
        referral_count: qualifiedCount,
        max_discount: 55,
        tiers,
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

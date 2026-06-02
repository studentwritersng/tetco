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

    const { plan_id } = await req.json();
    if (!plan_id) throw new Error("Missing plan_id");

    const { data: plan } = await supabase
      .from("plans")
      .select("*")
      .eq("id", plan_id)
      .single();

    if (!plan) throw new Error("Plan not found");
    if (plan.is_free) throw new Error("Cannot pay for free plan");

    const { data: referralHistory } = await supabase
      .from("referral_history")
      .select("qualified")
      .eq("referrer_id", user.id)
      .eq("qualified", true);

    const qualifiedCount = referralHistory?.length || 0;
    const discountPercent = calculateReferralDiscount(qualifiedCount);
    const originalAmount = plan.price_ngn * 100;
    const discountedAmount = Math.round(originalAmount * (100 - discountPercent) / 100);

    const paystackSecret = Deno.env.get("PAYSTACK_SECRET_KEY");
    if (!paystackSecret) throw new Error("Payment system not configured");

    const reference = `TC-${user.id.slice(0, 8)}-${Date.now()}`;

    const response = await fetch("https://api.paystack.co/transaction/initialize", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${paystackSecret}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email: user.email,
        amount: discountedAmount,
        plan: plan.paystack_plan_code || undefined,
        reference,
        metadata: {
          plan_id: plan.id,
          teacher_id: user.id,
          discount_percent: discountPercent,
          original_amount_kobo: originalAmount,
          discounted_amount_kobo: discountedAmount,
          qualified_referrals: qualifiedCount,
        },
        callback_url: `${Deno.env.get("APP_URL") || "https://teacherscompanion.app"}/payment-success`,
      }),
    });

    const data = await response.json();

    if (!data.status) throw new Error(data.message || "Paystack initialization failed");

    return new Response(
      JSON.stringify({
        authorization_url: data.data.authorization_url,
        reference,
        discount_percent: discountPercent,
        original_amount: plan.price_ngn,
        discounted_amount: Math.round(plan.price_ngn * (100 - discountPercent) / 100),
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

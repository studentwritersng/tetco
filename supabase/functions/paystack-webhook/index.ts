import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-paystack-signature",
};

function verifySignature(payload: string, signature: string, secret: string): boolean {
  const key = new TextEncoder().encode(secret);
  const msg = new TextEncoder().encode(payload);
  const hash = crypto.subtle.sync.sign("HMAC", key, msg);
  const hashArray = Array.from(new Uint8Array(hash));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, "0")).join("");
  return hashHex === signature;
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

    const body = await req.text();
    const signature = req.headers.get("x-paystack-signature") || "";
    const paystackSecret = Deno.env.get("PAYSTACK_SECRET_KEY") || "";

    if (!verifySignature(body, signature, paystackSecret)) {
      return new Response("Invalid signature", { status: 401 });
    }

    const event = JSON.parse(body);

    switch (event.event) {
      case "charge.success": {
        const { reference, customer, metadata, amount } = event.data;
        const teacherId = metadata?.teacher_id;
        const planId = metadata?.plan_id;

        if (!teacherId || !planId) break;

        const now = new Date();
        const periodEnd = new Date(now);
        periodEnd.setMonth(periodEnd.getMonth() + 1);

        await supabase.from("subscriptions").upsert({
          teacher_id: teacherId,
          plan_id: planId,
          status: "active",
          paystack_customer_id: customer.email,
          current_period_start: now.toISOString(),
          current_period_end: periodEnd.toISOString(),
        });

        break;
      }

      case "subscription.disable": {
        const { customer } = event.data;
        const { data: subs } = await supabase
          .from("subscriptions")
          .select("teacher_id")
          .eq("paystack_customer_id", customer.email)
          .eq("status", "active");

        if (subs && subs.length > 0) {
          for (const sub of subs) {
            await supabase
              .from("subscriptions")
              .update({ status: "cancelled", cancel_at_period_end: true })
              .eq("teacher_id", sub.teacher_id);
          }
        }
        break;
      }

      case "invoice.payment_failed": {
        const { customer } = event.data;
        const { data: subs } = await supabase
          .from("subscriptions")
          .select("teacher_id")
          .eq("paystack_customer_id", customer.email)
          .eq("status", "active");

        if (subs && subs.length > 0) {
          for (const sub of subs) {
            await supabase
              .from("subscriptions")
              .update({ status: "past_due" })
              .eq("teacher_id", sub.teacher_id);
          }
        }
        break;
      }
    }

    return new Response(JSON.stringify({ received: true }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 500,
    });
  }
});

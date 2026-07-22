import { BillingActions } from "@/components/billing-actions";
import { SignInRequired } from "@/components/sign-in-required";
import { getSubscription } from "@/lib/api";
import Link from "next/link";
import { cookies } from "next/headers";

export const metadata = { title: "Account" };

export default async function Account({ searchParams }: { searchParams?: Promise<{ billing?: string }> }) {
  if (!(await cookies()).get("movento_access")?.value) return <SignInRequired feature="Account and billing" />;
  const params = await searchParams;
  const subscription = await getSubscription();
  const status = subscription.status ?? "INACTIVE";
  const hasCustomer = Boolean(subscription.stripeCustomerId);
  const isGuest = status === "UNAVAILABLE";
  const billingState = params?.billing;
  const billingMessage = billingState === "demo"
    ? "Stripe checkout is not configured yet. Add STRIPE_SECRET_KEY and STRIPE_PRICE_ID to .env, then recreate payment-service."
    : billingState === "success"
      ? "Stripe checkout completed. If the status has not changed yet, send the Stripe webhook event to the local webhook endpoint."
      : billingState === "cancelled"
        ? "Stripe checkout was cancelled. You can start it again anytime."
        : "";

  return <main className="mx-auto max-w-4xl px-5 pb-20 pt-28">
    <h1 className="text-4xl font-black">Account</h1>
    {billingMessage && <p role="status" className="mt-5 rounded-xl border border-amber-500/40 bg-amber-500/10 p-4 text-sm text-amber-100">{billingMessage}</p>}
    {isGuest && <div className="mt-5 rounded-xl border border-primary/40 bg-primary/10 p-4 text-sm text-white/75">
      <p className="font-semibold text-white">You are browsing as a guest.</p>
      <p className="mt-1">Guests can browse and search. Sign in to use billing, profiles, watchlist, playback, and progress.</p>
      <Link href="/sign-in" className="mt-3 inline-flex rounded-full bg-primary px-4 py-2 font-semibold text-white transition hover:brightness-110">Sign in for member features</Link>
    </div>}
    <div className="mt-8 grid gap-5 md:grid-cols-2">
      <section className="rounded-xl border bg-card p-6">
        <p className="text-sm text-muted-foreground">Plan</p>
        <h2 className="mt-2 text-2xl font-bold">Movento Standard</h2>
        <p className="mt-2 text-sm text-muted-foreground">Stripe test mode · monthly subscription checkout</p>
        <div className="mt-4 rounded-lg border border-white/10 bg-black/25 p-3 text-sm">
          <p>Status: <span className="font-semibold text-white">{status}</span></p>
          <p className="mt-1 text-muted-foreground">Test card: <code>4242 4242 4242 4242</code>, any future expiry, any CVC.</p>
          {!hasCustomer && !isGuest && <p className="mt-2 text-amber-200">If checkout opens a local demo URL, set <code>STRIPE_SECRET_KEY</code> and <code>STRIPE_PRICE_ID</code> in `.env`, then recreate payment-service.</p>}
        </div>
        <BillingActions hasCustomer={hasCustomer} />
      </section>
      <section className="rounded-xl border bg-card p-6">
        <p className="text-sm text-muted-foreground">Playback</p>
        <h2 className="mt-2 text-2xl font-bold">HD streaming</h2>
        <p className="mt-2 text-sm text-muted-foreground">Adaptive quality on all supported devices.</p>
      </section>
    </div>
  </main>;
}

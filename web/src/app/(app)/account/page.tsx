import { CheckCircle2, MailWarning } from "lucide-react";
import { ResendVerificationButton } from "@/components/account-forms";
import { PlanPicker, SubscriptionActions } from "@/components/billing-actions";
import { SignInRequired } from "@/components/sign-in-required";
import { getCurrentUser, getPayments, getPlans, getSubscription, isSignedIn } from "@/lib/api";

export const metadata = { title: "Account" };

const MESSAGES: Record<string, string> = {
  success: "Payment succeeded — your plan is active. Enjoy watching!",
  declined: "The test payment was declined, so your plan did not change. Try again with “Approve”.",
  cancelled: "Checkout was cancelled. You can choose a plan anytime.",
};

const money = (amount: number, currency: string) => new Intl.NumberFormat("en-IE", { style: "currency", currency }).format(amount);
const date = (value?: string | null) => value ? new Date(value).toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" }) : "—";

export default async function Account({ searchParams }: { searchParams: Promise<{ billing?: string }> }) {
  if (!(await isSignedIn())) return <SignInRequired feature="Account and billing" />;
  const { billing } = await searchParams;
  const [user, subscription, { plans, testMode }, payments] = await Promise.all([getCurrentUser(), getSubscription(), getPlans(), getPayments()]);
  const message = billing ? MESSAGES[billing] : undefined;

  return <main className="mx-auto max-w-5xl px-5 pb-24 pt-28">
    <h1 className="text-4xl font-black">Account</h1>
    {message && <p role="status" className={`mt-5 rounded-lg border p-4 text-sm ${billing === "success" ? "border-emerald-500/40 bg-emerald-500/10 text-emerald-100" : "border-amber-500/40 bg-amber-500/10 text-amber-100"}`}>{message}</p>}

    {user && <section className="mt-8 rounded-xl border bg-card p-6">
      <h2 className="text-sm uppercase tracking-[.2em] text-white/50">Membership</h2>
      <p className="mt-3 text-lg font-semibold">{user.email}</p>
      {user.emailVerified
        ? <p className="mt-1 flex items-center gap-2 text-sm text-emerald-300"><CheckCircle2 className="size-4" />Email verified</p>
        : <p className="mt-1 flex flex-wrap items-center gap-2 text-sm text-amber-200"><MailWarning className="size-4" />Email not verified yet. <ResendVerificationButton email={user.email} /></p>}
    </section>}

    <section className="mt-6 rounded-xl border bg-card p-6" aria-labelledby="plan-heading">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 id="plan-heading" className="text-sm uppercase tracking-[.2em] text-white/50">Plan</h2>
          {subscription.active
            ? <>
                <p className="mt-3 text-2xl font-bold">{subscription.planName} <span className="text-base font-normal text-white/60">· {subscription.videoQuality} · {subscription.maxStreams} screen{subscription.maxStreams === 1 ? "" : "s"}</span></p>
                <p className="mt-1 text-sm text-white/65">{subscription.cancelAtPeriodEnd ? `Cancelled — you can keep watching until ${date(subscription.currentPeriodEnd)}.` : `Next billing date: ${date(subscription.currentPeriodEnd)}`}</p>
              </>
            : <p className="mt-3 text-lg text-white/80">No active plan. Choose one below to start watching.</p>}
        </div>
        {testMode && <span className="rounded border border-amber-400/50 bg-amber-400/10 px-2 py-1 text-xs font-bold uppercase tracking-wider text-amber-200">Test mode · no real money</span>}
      </div>
      {subscription.active && <SubscriptionActions cancelAtPeriodEnd={Boolean(subscription.cancelAtPeriodEnd)} testMode={testMode} />}
    </section>

    <section id="plans" className="mt-6" aria-labelledby="plans-heading">
      <h2 id="plans-heading" className="mb-4 text-2xl font-bold">{subscription.active ? "Change plan" : "Choose your plan"}</h2>
      <PlanPicker plans={plans} currentPlanId={subscription.active ? subscription.planId ?? undefined : undefined} testMode={testMode} />
    </section>

    <section className="mt-10" aria-labelledby="history-heading">
      <h2 id="history-heading" className="mb-4 text-2xl font-bold">Payment history</h2>
      {payments.length === 0
        ? <p className="text-sm text-white/60">No payments yet.</p>
        : <div className="overflow-hidden rounded-xl border">
            <table className="w-full text-left text-sm">
              <thead className="bg-card text-white/60"><tr><th className="p-3">Date</th><th className="p-3">Description</th><th className="p-3">Amount</th><th className="p-3">Status</th></tr></thead>
              <tbody>{payments.map((payment) => <tr key={payment.id} className="border-t">
                <td className="p-3 text-white/70">{date(payment.createdAt)}</td>
                <td className="p-3">{payment.description}</td>
                <td className="p-3">{money(payment.amount, payment.currency)}</td>
                <td className="p-3"><span className={`rounded px-2 py-0.5 text-xs font-semibold ${payment.status === "SUCCEEDED" ? "bg-emerald-500/15 text-emerald-300" : "bg-red-500/15 text-red-300"}`}>{payment.status === "SUCCEEDED" ? "Paid" : "Declined"}</span></td>
              </tr>)}</tbody>
            </table>
          </div>}
    </section>
  </main>;
}

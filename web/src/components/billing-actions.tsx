"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { Plan } from "@/lib/types";

async function billing(path: string, body?: unknown) {
  const response = await fetch(`/api/backend/billing/${path}`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: body ? JSON.stringify(body) : undefined,
  }).catch(() => null);
  const data = response ? await response.json().catch(() => ({})) : {};
  return { ok: Boolean(response?.ok), data, status: response?.status ?? 503 };
}

/**
 * Plan cards. In test mode there is no card form at all: the viewer picks the simulated outcome
 * (approve / decline), like using Stripe's test cards, and nothing is charged.
 */
export function PlanPicker({ plans, currentPlanId, testMode }: { plans: Plan[]; currentPlanId?: string; testMode: boolean }) {
  const router = useRouter();
  const [selected, setSelected] = useState(currentPlanId ?? plans[1]?.id ?? plans[0]?.id);
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState("");

  async function confirm(outcome: "success" | "decline") {
    if (!selected) return;
    setPending(true);
    setMessage("");
    const result = currentPlanId && outcome === "success"
      ? await billing("change-plan", { planId: selected })
      : await billing("checkout", { planId: selected, outcome });
    setPending(false);
    if (!result.ok) return setMessage(result.data.message ?? "Billing is unavailable right now.");
    if (result.data.url) window.location.assign(result.data.url);
    else router.refresh();
  }

  if (plans.length === 0) return <p className="text-sm text-white/60">Plans are unavailable right now.</p>;
  return <div className="space-y-5">
    <div role="radiogroup" aria-label="Plans" className="grid gap-4 md:grid-cols-3">
      {plans.map((plan) => {
        const active = plan.id === selected;
        return <button key={plan.id} role="radio" aria-checked={active} onClick={() => setSelected(plan.id)}
          className={`rounded-xl border-2 p-5 text-left transition ${active ? "border-primary bg-primary/10" : "border-white/10 bg-card hover:border-white/30"}`}>
          <div className="flex items-center justify-between">
            <span className="text-xl font-bold">{plan.name}</span>
            {plan.id === currentPlanId && <span className="rounded bg-white/15 px-2 py-0.5 text-xs">Current</span>}
          </div>
          <p className="mt-1 text-sm text-white/60">{plan.videoQuality}</p>
          <p className="mt-4 text-2xl font-black">{new Intl.NumberFormat("en-IE", { style: "currency", currency: plan.currency }).format(plan.monthlyPrice)}<span className="text-sm font-normal text-white/60"> /month</span></p>
          <ul className="mt-4 space-y-1.5 text-sm text-white/75">{plan.features.map((feature) => <li key={feature} className="flex gap-2"><Check className="size-4 flex-none text-primary" />{feature}</li>)}</ul>
        </button>;
      })}
    </div>
    <div className="flex flex-wrap items-center gap-3">
      {currentPlanId
        ? <Button onClick={() => confirm("success")} disabled={pending || selected === currentPlanId}>{pending ? "Saving…" : "Switch plan"}</Button>
        : testMode
          ? <>
              <Button onClick={() => confirm("success")} disabled={pending}>{pending ? "Processing…" : "Subscribe — approve test payment"}</Button>
              <Button variant="outline" onClick={() => confirm("decline")} disabled={pending}>Simulate declined payment</Button>
            </>
          : <Button onClick={() => confirm("success")} disabled={pending}>{pending ? "Opening checkout…" : "Continue to Stripe Checkout (test)"}</Button>}
    </div>
    {testMode && !currentPlanId && <p className="text-xs text-white/50">Test mode: no card details are requested and no money moves. Payments are recorded with <code>test_</code> IDs.</p>}
    {message && <p role="alert" className="rounded-md border border-amber-500/40 bg-amber-500/10 p-3 text-sm text-amber-100">{message}</p>}
  </div>;
}

export function SubscriptionActions({ cancelAtPeriodEnd, testMode }: { cancelAtPeriodEnd: boolean; testMode: boolean }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState("");

  async function run(path: "cancel" | "resume" | "portal") {
    setPending(true);
    const result = await billing(path);
    setPending(false);
    if (!result.ok) return setMessage(result.data.message ?? "Billing is unavailable right now.");
    if (result.data.url) window.location.assign(result.data.url);
    else router.refresh();
  }

  return <div className="mt-5 flex flex-wrap items-center gap-3">
    {!testMode && <Button variant="outline" onClick={() => run("portal")} disabled={pending}>Manage in Stripe</Button>}
    {testMode && (cancelAtPeriodEnd
      ? <Button onClick={() => run("resume")} disabled={pending}>Resume membership</Button>
      : <Button variant="outline" onClick={() => run("cancel")} disabled={pending}>Cancel membership</Button>)}
    {message && <p role="alert" className="text-sm text-amber-200">{message}</p>}
  </div>;
}

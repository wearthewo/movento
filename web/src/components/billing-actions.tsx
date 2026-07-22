"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";

export function BillingActions({ hasCustomer = false }: { hasCustomer?: boolean }) {
  const [pending, setPending] = useState<"checkout" | "portal" | null>(null);
  const [message, setMessage] = useState("");

  async function go(path: "checkout" | "portal") {
    setPending(path);
    setMessage("");
    try {
      const response = await fetch(`/api/backend/billing/${path}`, { method: "POST" });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setMessage(data.message ?? (response.status === 401 ? "Sign in first, then open billing." : "Billing is not ready yet."));
        return;
      }
      if (data.url) window.location.assign(data.url);
      else setMessage("Billing returned no checkout URL.");
    } catch {
      setMessage("Billing service is unavailable.");
    } finally {
      setPending(null);
    }
  }

  return <div className="mt-6 space-y-3">
    <div className="flex flex-wrap gap-3">
      <Button onClick={() => go("checkout")} disabled={pending !== null}>{pending === "checkout" ? "Opening..." : "Choose plan"}</Button>
      <Button variant="outline" onClick={() => go("portal")} disabled={pending !== null || !hasCustomer}>{pending === "portal" ? "Opening..." : "Manage billing"}</Button>
    </div>
    {message && <p role="alert" className="rounded-md border border-amber-500/40 bg-amber-500/10 p-3 text-sm text-amber-100">{message}</p>}
  </div>;
}

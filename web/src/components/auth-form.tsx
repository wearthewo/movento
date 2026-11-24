"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";

/** Only same-site relative paths are accepted as a post-login destination (no open redirects). */
function safeNext(next?: string) {
  return next && next.startsWith("/") && !next.startsWith("//") ? next : undefined;
}

export function AuthForm({ mode, next }: { mode: "login" | "register"; next?: string }) {
  const router = useRouter();
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setError("");
    const body = Object.fromEntries(new FormData(event.currentTarget));
    const response = await fetch(`/api/auth/${mode}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    const result = await response.json().catch(() => ({}));
    setPending(false);
    if (!response.ok) return setError(result.message ?? "Unable to continue. Please try again.");
    const destination = safeNext(next);
    router.push(destination ? `/profiles?next=${encodeURIComponent(destination)}` : "/profiles");
    router.refresh();
  }

  return <form onSubmit={submit} className="space-y-4">
    {mode === "register" && <div className="grid grid-cols-2 gap-3"><Field name="firstName" label="First name" autoComplete="given-name"/><Field name="lastName" label="Last name" autoComplete="family-name"/></div>}
    <Field name="email" label="Email" type="email" autoComplete="email"/>
    <Field name="password" label="Password" type="password" minLength={mode === "register" ? 8 : undefined} autoComplete={mode === "login" ? "current-password" : "new-password"}/>
    {mode === "register" && <p className="text-xs text-muted-foreground">Passwords need at least 8 characters.</p>}
    {error && <p role="alert" className="rounded-md border border-red-500/40 bg-red-500/10 p-3 text-sm text-red-200">{error}</p>}
    <Button type="submit" className="w-full" disabled={pending}>{pending ? "Please wait..." : mode === "login" ? "Sign in" : "Create account"}</Button>
  </form>;
}

function Field({ label, ...props }: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return <label className="block space-y-2 text-sm font-medium"><span>{label}</span><input required {...props} className="h-11 w-full rounded-md border bg-black/35 px-3 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/25"/></label>;
}

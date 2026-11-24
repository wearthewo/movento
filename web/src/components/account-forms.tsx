"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { Button } from "@/components/ui/button";

async function post(action: string, body: unknown) {
  const response = await fetch(`/api/auth/${action}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
  const data = await response.json().catch(() => ({}));
  return { ok: response.ok, message: data.message as string | undefined };
}

const inputClass = "h-11 w-full rounded-md border bg-black/35 px-3 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/25";

export function ForgotPasswordForm() {
  const [sent, setSent] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    const result = await post("forgot-password", { email: new FormData(event.currentTarget).get("email") });
    setPending(false);
    if (result.ok) setSent(true);
    else setError(result.message ?? "Please try again.");
  }

  if (sent) {
    return <div role="status" className="space-y-3 text-sm text-white/75">
      <p>If an account exists for that email, a reset link is on its way. It expires in 30 minutes.</p>
      <p className="text-white/50">Running locally? Open the Mailpit inbox at <a className="underline" href="http://localhost:8025" target="_blank" rel="noreferrer">localhost:8025</a>.</p>
      <Link href="/sign-in" className="inline-block font-semibold text-white hover:underline">Back to sign in</Link>
    </div>;
  }
  return <form onSubmit={submit} className="space-y-4">
    <label className="block space-y-2 text-sm font-medium"><span>Email</span><input name="email" type="email" required autoComplete="email" className={inputClass} /></label>
    {error && <p role="alert" className="rounded-md border border-red-500/40 bg-red-500/10 p-3 text-sm text-red-200">{error}</p>}
    <Button type="submit" className="w-full" disabled={pending}>{pending ? "Sending…" : "Email me a reset link"}</Button>
  </form>;
}

export function ResetPasswordForm({ token }: { token: string }) {
  const [done, setDone] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    if (form.get("password") !== form.get("confirm")) return setError("The passwords don't match.");
    setPending(true);
    const result = await post("reset-password", { token, password: form.get("password") });
    setPending(false);
    if (result.ok) setDone(true);
    else setError(result.message ?? "This link is invalid or has expired.");
  }

  if (!token) return <p className="text-sm text-red-200">This reset link is incomplete. <Link href="/forgot-password" className="underline">Request a new one.</Link></p>;
  if (done) {
    return <div role="status" className="space-y-4 text-sm text-white/75">
      <p>Your password has been changed and you have been signed out on all devices.</p>
      <Button asChild className="w-full"><Link href="/sign-in">Sign in</Link></Button>
    </div>;
  }
  return <form onSubmit={submit} className="space-y-4">
    <label className="block space-y-2 text-sm font-medium"><span>New password</span><input name="password" type="password" required minLength={8} autoComplete="new-password" className={inputClass} /></label>
    <label className="block space-y-2 text-sm font-medium"><span>Confirm new password</span><input name="confirm" type="password" required minLength={8} autoComplete="new-password" className={inputClass} /></label>
    {error && <p role="alert" className="rounded-md border border-red-500/40 bg-red-500/10 p-3 text-sm text-red-200">{error}</p>}
    <Button type="submit" className="w-full" disabled={pending}>{pending ? "Saving…" : "Change password"}</Button>
  </form>;
}

export function ResendVerificationButton({ email }: { email: string }) {
  const [state, setState] = useState<"idle" | "sending" | "sent">("idle");
  async function resend() {
    setState("sending");
    await post("resend-verification", { email });
    setState("sent");
  }
  return <button onClick={resend} disabled={state !== "idle"} className="font-semibold text-white underline disabled:no-underline disabled:opacity-70">
    {state === "sent" ? "Verification email sent" : state === "sending" ? "Sending…" : "Resend verification email"}
  </button>;
}

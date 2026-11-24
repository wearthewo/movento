import Link from "next/link";
import { CheckCircle2, XCircle } from "lucide-react";
import { gatewayUrl } from "@/lib/session";

export const metadata = { title: "Verify email" };
export const dynamic = "force-dynamic";

/** Verification is a one-time POST made server-side when the emailed link is opened. */
export default async function VerifyEmail({ searchParams }: { searchParams: Promise<{ token?: string }> }) {
  const { token } = await searchParams;
  let ok = false;
  if (token) {
    const response = await fetch(`${gatewayUrl()}/api/v1/auth/verify-email`, {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ token }), cache: "no-store",
    }).catch(() => null);
    ok = Boolean(response?.ok);
  }
  return <div className="text-center">
    {ok ? <CheckCircle2 className="mx-auto size-12 text-emerald-400" /> : <XCircle className="mx-auto size-12 text-red-400" />}
    <h1 className="mt-4 text-2xl font-black">{ok ? "Email confirmed" : "This link is invalid or has expired"}</h1>
    <p className="mt-2 text-sm text-white/65">{ok ? "Thanks! Your account is fully set up." : "Sign in and request a new verification email from your account page."}</p>
    <Link href={ok ? "/browse" : "/account"} className="mt-6 inline-block rounded bg-primary px-5 py-2 font-semibold">{ok ? "Start watching" : "Go to account"}</Link>
  </div>;
}

import Link from "next/link";
import { AuthForm } from "@/components/auth-form";

export const metadata = { title: "Sign in" };

export default async function SignIn({ searchParams }: { searchParams: Promise<{ next?: string; loggedOut?: string }> }) {
  const { next, loggedOut } = await searchParams;
  return <>
    <h1 className="mb-2 text-3xl font-black">Sign in</h1>
    <p className="mb-7 text-sm text-muted-foreground">{loggedOut ? "You have been signed out." : "Sign in to continue watching."}</p>
    <AuthForm mode="login" next={next} />
    <p className="mt-4 text-right text-sm"><Link href="/forgot-password" className="text-white/70 hover:underline">Forgot password?</Link></p>
    <p className="mt-6 text-sm text-white/60">New to Movento? <Link href="/sign-up" className="font-semibold text-white hover:underline">Sign up now.</Link></p>
  </>;
}

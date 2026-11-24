import Link from "next/link";
import { AuthForm } from "@/components/auth-form";

export const metadata = { title: "Create account" };

export default function SignUp() {
  return <>
    <h1 className="mb-2 text-3xl font-black">Join Movento</h1>
    <p className="mb-7 text-sm text-muted-foreground">Create your account and find your next story.</p>
    <AuthForm mode="register" />
    <p className="mt-6 text-sm text-white/60">Already have an account? <Link href="/sign-in" className="font-semibold text-white hover:underline">Sign in.</Link></p>
  </>;
}

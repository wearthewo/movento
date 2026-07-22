import Link from "next/link";
import { LockKeyhole } from "lucide-react";
import { Button } from "@/components/ui/button";

export function SignInRequired({ feature }: { feature: string }) {
  return <main className="grid min-h-screen place-items-center px-5 text-center">
    <div className="max-w-lg rounded-2xl border border-white/10 bg-card/80 p-8 shadow-2xl">
      <LockKeyhole className="mx-auto size-10 text-primary" />
      <h1 className="mt-5 text-3xl font-black">Sign in required</h1>
      <p className="mt-3 text-white/65">{feature} is available after you sign in. Guests can still browse, search, and open title details.</p>
      <div className="mt-7 flex justify-center gap-3">
        <Button asChild><Link href="/sign-in">Sign in</Link></Button>
        <Button asChild variant="outline"><Link href="/sign-up">Create account</Link></Button>
      </div>
    </div>
  </main>;
}

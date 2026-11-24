import { ForgotPasswordForm } from "@/components/account-forms";

export const metadata = { title: "Forgot password" };

export default function ForgotPassword() {
  return <>
    <h1 className="mb-2 text-3xl font-black">Forgot your password?</h1>
    <p className="mb-7 text-sm text-muted-foreground">We&apos;ll email you a link to choose a new one.</p>
    <ForgotPasswordForm />
  </>;
}

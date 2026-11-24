import { ResetPasswordForm } from "@/components/account-forms";

export const metadata = { title: "Choose a new password" };

export default async function ResetPassword({ searchParams }: { searchParams: Promise<{ token?: string }> }) {
  const { token = "" } = await searchParams;
  return <>
    <h1 className="mb-2 text-3xl font-black">Choose a new password</h1>
    <p className="mb-7 text-sm text-muted-foreground">At least 8 characters. You&apos;ll be signed out everywhere else.</p>
    <ResetPasswordForm token={token} />
  </>;
}

import { cookies } from "next/headers";
import { ProfilePicker } from "@/components/profile-picker";
import { SignInRequired } from "@/components/sign-in-required";
import { getProfiles } from "@/lib/api";

export default async function ProfilesPage() {
  if (!(await cookies()).get("movento_access")?.value) return <SignInRequired feature="Profile selection" />;
  const profiles = await getProfiles();
  return <main className="mx-auto flex min-h-screen max-w-5xl flex-col justify-center px-6 py-24"><h1 className="mb-10 text-center text-4xl font-bold md:text-5xl">Who&apos;s watching?</h1><ProfilePicker profiles={profiles}/></main>;
}

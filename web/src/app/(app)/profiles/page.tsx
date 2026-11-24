import { cookies } from "next/headers";
import { ProfilePicker } from "@/components/profile-picker";
import { SignInRequired } from "@/components/sign-in-required";
import { getProfiles } from "@/lib/api";
import { ACCESS_COOKIE } from "@/lib/session";

export const metadata = { title: "Who's watching?" };

export default async function ProfilesPage({ searchParams }: { searchParams: Promise<{ next?: string; manage?: string }> }) {
  if (!(await cookies()).get(ACCESS_COOKIE)?.value) return <SignInRequired feature="Profile selection" />;
  const { next, manage } = await searchParams;
  const profiles = await getProfiles();
  const destination = next && next.startsWith("/") && !next.startsWith("//") ? next : "/browse";
  return <main className="fixed inset-0 z-[60] flex flex-col items-center justify-center overflow-y-auto bg-[#0b0b0b] px-6 py-24">
    <h1 className="mb-10 text-center text-4xl font-medium md:text-6xl">{manage ? "Manage profiles" : "Who's watching?"}</h1>
    <ProfilePicker profiles={profiles} next={destination} initialManage={Boolean(manage)} />
  </main>;
}

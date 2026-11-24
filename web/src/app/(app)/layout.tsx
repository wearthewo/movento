import { cookies } from "next/headers";
import { SiteHeader } from "@/components/site-header";
import { ACCESS_COOKIE, PROFILE_META_COOKIE, parseProfileMeta } from "@/lib/session";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const jar = await cookies();
  const signedIn = Boolean(jar.get(ACCESS_COOKIE)?.value);
  const profile = parseProfileMeta(jar.get(PROFILE_META_COOKIE)?.value);
  return <>
    <SiteHeader signedIn={signedIn} profile={profile} />
    {children}
  </>;
}

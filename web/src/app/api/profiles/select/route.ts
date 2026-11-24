import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { ACCESS_COOKIE, gatewayUrl, writeProfileCookies, writeSessionCookies } from "@/lib/session";
import type { ViewerProfile } from "@/lib/types";

/** Exchanges the account token for a profile-scoped token (profile id + maturity claim). */
export async function POST(request: Request) {
  const { profileId } = await request.json().catch(() => ({}));
  if (typeof profileId !== "string" || !profileId) return NextResponse.json({ message: "Profile is required" }, { status: 400 });
  const access = (await cookies()).get(ACCESS_COOKIE)?.value;
  if (!access) return NextResponse.json({ message: "Please sign in again." }, { status: 401 });

  const upstream = await fetch(`${gatewayUrl()}/api/v1/users/me/profiles/${encodeURIComponent(profileId)}/select`, {
    method: "POST", headers: { Authorization: `Bearer ${access}` }, cache: "no-store",
  }).catch(() => null);
  if (!upstream?.ok) return NextResponse.json({ message: "Profile selection failed" }, { status: upstream?.status ?? 503 });

  const payload = await upstream.json() as { accessToken: string; expiresIn?: number; profile?: ViewerProfile };
  const response = NextResponse.json({ selected: true });
  writeSessionCookies(response.cookies, payload);
  writeProfileCookies(response.cookies, payload.profile ?? { id: profileId, name: "Viewer", kidsMode: false });
  return response;
}

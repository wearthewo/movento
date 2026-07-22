import { NextResponse } from "next/server";
import { cookies } from "next/headers";

export async function POST(request: Request) {
  const { profileId } = await request.json();
  if (!profileId) return NextResponse.json({ message: "Profile is required" }, { status: 400 });
  const response = NextResponse.json({ selected: true });
  const access = (await cookies()).get("movento_access")?.value;
  if (access && access !== "demo") {
    const upstream = await fetch(`${process.env.GATEWAY_URL ?? "http://localhost:8080"}/api/v1/users/me/profiles/${profileId}/select`, { method: "POST", headers: { Authorization: `Bearer ${access}` } });
    if (!upstream.ok) return NextResponse.json({ message: "Profile selection failed" }, { status: upstream.status });
    const payload = await upstream.json();
    response.cookies.set("movento_access", payload.accessToken, { httpOnly: true, sameSite: "lax", secure: process.env.COOKIE_SECURE === "true", path: "/", maxAge: payload.expiresIn ?? 900 });
  }
  response.cookies.set("movento_profile", profileId, { httpOnly: true, sameSite: "lax", secure: process.env.COOKIE_SECURE === "true", path: "/", maxAge: 2592000 });
  response.cookies.set("movento_signed_in", "1", { sameSite: "lax", secure: process.env.COOKIE_SECURE === "true", path: "/", maxAge: 2592000 });
  return response;
}

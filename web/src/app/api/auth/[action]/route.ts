import { NextRequest, NextResponse } from "next/server";
import { PROFILE_COOKIE, REFRESH_COOKIE, clearSessionCookies, gatewayUrl, writeSessionCookies } from "@/lib/session";

const ACTIONS = ["login", "register", "refresh", "logout"];

export async function POST(request: NextRequest, context: { params: Promise<{ action: string }> }) {
  const { action } = await context.params;
  if (!ACTIONS.includes(action)) return NextResponse.json({ message: "Not found" }, { status: 404 });
  try {
    const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
    if (action === "logout") {
      if (refreshToken) {
        await fetch(`${gatewayUrl()}/api/v1/auth/logout`, {
          method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ refreshToken }), cache: "no-store",
        }).catch(() => undefined);
      }
      const response = NextResponse.json({ loggedOut: true });
      clearSessionCookies(response.cookies);
      return response;
    }
    if (action === "refresh" && !refreshToken) {
      const response = NextResponse.json({ message: "Your session has expired. Please sign in again." }, { status: 401 });
      clearSessionCookies(response.cookies);
      return response;
    }
    const body = action === "refresh"
      ? JSON.stringify({ refreshToken, profileId: request.cookies.get(PROFILE_COOKIE)?.value })
      : await request.text();
    const upstream = await fetch(`${gatewayUrl()}/api/v1/auth/${action}`, {
      method: "POST", headers: { "Content-Type": "application/json" }, body, cache: "no-store", signal: AbortSignal.timeout(15000),
    });
    const payload = await upstream.json().catch(() => ({}));
    if (!upstream.ok) {
      const fallback = action === "register" ? "Could not create your account." : "Incorrect email or password.";
      return NextResponse.json({ message: payload.message ?? fallback }, { status: upstream.status });
    }
    // Tokens stay in httpOnly cookies; the browser only learns who signed in.
    const response = NextResponse.json({ id: payload.id, email: payload.email, roles: payload.roles });
    if (payload.accessToken) writeSessionCookies(response.cookies, payload);
    if (action !== "refresh") response.cookies.set(PROFILE_COOKIE, "", { path: "/", maxAge: 0 });
    return response;
  } catch {
    return NextResponse.json({ message: "Authentication service is unavailable" }, { status: 503 });
  }
}

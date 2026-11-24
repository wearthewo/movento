import { NextRequest, NextResponse } from "next/server";
import { PROFILE_COOKIE, REFRESH_COOKIE, clearSessionCookies, clientIp, gatewayUrl, writeSessionCookies } from "@/lib/session";

const SESSION_ACTIONS = ["login", "register", "refresh", "logout"];
/** Account actions forward the JSON body and never touch cookies. */
const ACCOUNT_ACTIONS = ["verify-email", "resend-verification", "forgot-password", "reset-password"];

const FALLBACK_MESSAGES: Record<string, string> = {
  login: "Incorrect email or password.",
  register: "Could not create your account.",
  "verify-email": "This link is invalid or has expired.",
  "reset-password": "This link is invalid or has expired.",
};

export async function POST(request: NextRequest, context: { params: Promise<{ action: string }> }) {
  const { action } = await context.params;
  if (![...SESSION_ACTIONS, ...ACCOUNT_ACTIONS].includes(action)) return NextResponse.json({ message: "Not found" }, { status: 404 });
  const forwardHeaders = { "Content-Type": "application/json", "X-Forwarded-For": clientIp(request.headers) };
  try {
    const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
    if (action === "logout") {
      if (refreshToken) {
        await fetch(`${gatewayUrl()}/api/v1/auth/logout`, {
          method: "POST", headers: forwardHeaders, body: JSON.stringify({ refreshToken }), cache: "no-store",
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
      method: "POST", headers: forwardHeaders, body, cache: "no-store", signal: AbortSignal.timeout(15000),
    });
    const payload = await upstream.json().catch(() => ({}));
    if (upstream.status === 429) {
      return NextResponse.json({ message: payload.message ?? "Too many attempts. Please wait a minute and try again." }, { status: 429 });
    }
    if (!upstream.ok) {
      return NextResponse.json({ message: payload.message ?? FALLBACK_MESSAGES[action] ?? "Something went wrong. Please try again." }, { status: upstream.status });
    }
    if (ACCOUNT_ACTIONS.includes(action)) return NextResponse.json({ ok: true, ...payload }, { status: 200 });

    // Tokens stay in httpOnly cookies; the browser only learns who signed in.
    const response = NextResponse.json({ id: payload.id, email: payload.email, roles: payload.roles });
    if (payload.accessToken) writeSessionCookies(response.cookies, payload);
    if (action !== "refresh") response.cookies.set(PROFILE_COOKIE, "", { path: "/", maxAge: 0 });
    return response;
  } catch {
    return NextResponse.json({ message: "Authentication service is unavailable" }, { status: 503 });
  }
}

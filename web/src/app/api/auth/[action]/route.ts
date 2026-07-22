import { NextRequest, NextResponse } from "next/server";

const gateway = process.env.GATEWAY_URL ?? "http://localhost:8080";

function clearSessionCookies(response: NextResponse) {
  for (const name of ["movento_access", "movento_refresh", "movento_signed_in", "movento_profile"]) {
    response.cookies.set(name, "", { path: "/", maxAge: 0 });
  }
}

export async function POST(request: NextRequest, context: { params: Promise<{ action: string }> }) {
  const { action } = await context.params;
  if (!["login", "register", "refresh", "logout"].includes(action)) return NextResponse.json({ message: "Not found" }, { status: 404 });
  try {
    const refreshToken = request.cookies.get("movento_refresh")?.value;
    if (action === "logout") {
      if (refreshToken) {
        await fetch(`${gateway}/api/v1/auth/logout`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
          cache: "no-store",
        }).catch(() => undefined);
      }
      const response = NextResponse.json({ loggedOut: true });
      clearSessionCookies(response);
      return response;
    }
    if (action === "refresh" && !refreshToken) {
      const response = NextResponse.json({ message: "Your session has expired. Please sign in again." }, { status: 401 });
      clearSessionCookies(response);
      return response;
    }
    const body = action === "refresh" ? JSON.stringify({ refreshToken }) : await request.text();
    const upstream = await fetch(`${gateway}/api/v1/auth/${action}`, { method: "POST", headers: { "Content-Type": "application/json" }, body, cache: "no-store" });
    const payload = await upstream.json().catch(() => ({}));
    const response = NextResponse.json(upstream.ok ? payload : { message: payload.message ?? payload.error ?? "Authentication failed. Check your email and password." }, { status: upstream.status });
    if (upstream.ok && payload.accessToken) {
      const secure = process.env.COOKIE_SECURE === "true";
      response.cookies.set("movento_access", payload.accessToken, { httpOnly: true, secure, sameSite: "lax", path: "/", maxAge: payload.expiresIn ?? 900 });
      if (payload.refreshToken) response.cookies.set("movento_refresh", payload.refreshToken, { httpOnly: true, secure, sameSite: "lax", path: "/", maxAge: 2592000 });
      response.cookies.set("movento_signed_in", "1", { secure, sameSite: "lax", path: "/", maxAge: 2592000 });
    }
    return response;
  } catch {
    return NextResponse.json({ message: "Authentication service is unavailable" }, { status: 503 });
  }
}

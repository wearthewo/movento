import { NextRequest, NextResponse } from "next/server";
import {
  ACCESS_COOKIE, PROFILE_COOKIE, REFRESH_COOKIE, clearSessionCookies, clientIp, gatewayUrl, secondsUntilExpiry, writeSessionCookies,
} from "@/lib/session";

/** Pages that need a selected viewer profile, like Netflix's "Who's watching?" gate. */
const PROFILE_GATED = ["/browse", "/movies", "/series", "/my-list", "/title", "/watch", "/search"];
const REFRESH_WINDOW_SECONDS = 60;

/**
 * Runs before pages and BFF API calls:
 *  1. Silently refreshes the 15-minute access token from the refresh cookie (keeping the selected profile),
 *     so viewers stay signed in like on a real streaming service.
 *  2. Sends signed-in viewers without a selected profile to /profiles.
 */
export async function proxy(request: NextRequest) {
  const access = request.cookies.get(ACCESS_COOKIE)?.value;
  const refresh = request.cookies.get(REFRESH_COOKIE)?.value;
  const profileId = request.cookies.get(PROFILE_COOKIE)?.value;

  let refreshed: { accessToken: string; refreshToken?: string; expiresIn?: number } | null = null;
  let refreshFailed = false;
  if (refresh && secondsUntilExpiry(access) < REFRESH_WINDOW_SECONDS) {
    try {
      const upstream = await fetch(`${gatewayUrl()}/api/v1/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Forwarded-For": clientIp(request.headers) },
        body: JSON.stringify({ refreshToken: refresh, profileId: profileId && profileId !== "demo" ? profileId : undefined }),
        cache: "no-store",
        signal: AbortSignal.timeout(5000),
      });
      if (upstream.ok) refreshed = await upstream.json();
      else if (upstream.status === 401) refreshFailed = true;
    } catch {
      // Backend unavailable: keep the current cookies and let the page degrade gracefully.
    }
  }

  const signedIn = Boolean(refreshed?.accessToken || (access && !refreshFailed));
  const path = request.nextUrl.pathname;
  if (signedIn && !profileId && PROFILE_GATED.some((prefix) => path === prefix || path.startsWith(`${prefix}/`))) {
    const target = new URL("/profiles", request.url);
    target.searchParams.set("next", `${path}${request.nextUrl.search}`);
    const redirect = NextResponse.redirect(target);
    if (refreshed) writeSessionCookies(redirect.cookies, refreshed);
    return redirect;
  }

  if (refreshed) {
    // Make the new token visible to this render as well as to the browser.
    request.cookies.set(ACCESS_COOKIE, refreshed.accessToken);
    const response = NextResponse.next({ request: { headers: request.headers } });
    writeSessionCookies(response.cookies, refreshed);
    return response;
  }
  if (refreshFailed) {
    request.cookies.delete(ACCESS_COOKIE);
    const response = NextResponse.next({ request: { headers: request.headers } });
    clearSessionCookies(response.cookies);
    return response;
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|api/auth|api/health|api/media|icon.svg|favicon.ico|robots.txt|sitemap.xml).*)"],
};

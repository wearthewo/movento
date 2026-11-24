import type { NextResponse } from "next/server";

/** Cookie names shared by the BFF route handlers and the request proxy. */
export const ACCESS_COOKIE = "movento_access";
export const REFRESH_COOKIE = "movento_refresh";
export const SIGNED_IN_COOKIE = "movento_signed_in";
export const PROFILE_COOKIE = "movento_profile";
/** Non-sensitive display info for the header avatar: "<name>|<kids 0/1>". Readable by client JS. */
export const PROFILE_META_COOKIE = "movento_profile_meta";

const THIRTY_DAYS = 60 * 60 * 24 * 30;

export const gatewayUrl = () => process.env.GATEWAY_URL ?? "http://localhost:8080";
const secure = () => process.env.COOKIE_SECURE === "true";

export interface TokenPayload {
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number;
}

type CookieWriter = Pick<NextResponse["cookies"], "set">;

export function writeSessionCookies(cookies: CookieWriter, payload: TokenPayload) {
  cookies.set(ACCESS_COOKIE, payload.accessToken, { httpOnly: true, secure: secure(), sameSite: "lax", path: "/", maxAge: payload.expiresIn ?? 900 });
  if (payload.refreshToken) {
    cookies.set(REFRESH_COOKIE, payload.refreshToken, { httpOnly: true, secure: secure(), sameSite: "lax", path: "/", maxAge: THIRTY_DAYS });
  }
  cookies.set(SIGNED_IN_COOKIE, "1", { secure: secure(), sameSite: "lax", path: "/", maxAge: THIRTY_DAYS });
}

export function writeProfileCookies(cookies: CookieWriter, profile: { id: string; name: string; kidsMode: boolean }) {
  cookies.set(PROFILE_COOKIE, profile.id, { httpOnly: true, secure: secure(), sameSite: "lax", path: "/", maxAge: THIRTY_DAYS });
  cookies.set(PROFILE_META_COOKIE, `${encodeURIComponent(profile.name)}|${profile.kidsMode ? 1 : 0}`, { secure: secure(), sameSite: "lax", path: "/", maxAge: THIRTY_DAYS });
}

export function clearSessionCookies(cookies: CookieWriter) {
  for (const name of [ACCESS_COOKIE, REFRESH_COOKIE, SIGNED_IN_COOKIE, PROFILE_COOKIE, PROFILE_META_COOKIE]) {
    cookies.set(name, "", { path: "/", maxAge: 0 });
  }
}

export function parseProfileMeta(raw?: string): { name: string; kids: boolean } | null {
  if (!raw) return null;
  const [name, kids] = raw.split("|");
  try {
    return { name: decodeURIComponent(name), kids: kids === "1" };
  } catch {
    return null;
  }
}

/** Seconds until the JWT expires (no signature check: only used to decide when to refresh). */
export function secondsUntilExpiry(token?: string): number {
  if (!token) return -1;
  try {
    const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    return typeof payload.exp === "number" ? payload.exp - Math.floor(Date.now() / 1000) : -1;
  } catch {
    return -1;
  }
}

/**
 * Browser IP for gateway rate limiting. Behind Caddy the real client is the last X-Forwarded-For hop
 * (the one Caddy appended); locally there is no proxy, so fall back to a fixed key.
 */
export function clientIp(headers: Headers): string {
  const forwarded = headers.get("x-forwarded-for");
  if (forwarded) {
    const hops = forwarded.split(",").map((hop) => hop.trim()).filter(Boolean);
    if (hops.length) return hops[hops.length - 1];
  }
  return headers.get("x-real-ip") ?? "127.0.0.1";
}

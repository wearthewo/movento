import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { ACCESS_COOKIE, gatewayUrl } from "@/lib/session";

export async function POST(_request: Request, context: { params: Promise<{ assetId: string }> }) {
  const { assetId } = await context.params;
  if (!/^\d+$/.test(assetId)) return NextResponse.json({ message: "Unknown title" }, { status: 404 });
  const token = (await cookies()).get(ACCESS_COOKIE)?.value;
  if (!token) return NextResponse.json({ message: "Sign in to start playback." }, { status: 401 });
  try {
    const response = await fetch(`${gatewayUrl()}/api/v1/playback/${assetId}/session`, {
      method: "POST", headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal: AbortSignal.timeout(10000),
    });
    return new NextResponse(await response.text(), { status: response.status, headers: { "Content-Type": "application/json" } });
  } catch {
    if (process.env.NODE_ENV !== "production") return NextResponse.json({ manifestUrl: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", demo: true });
    return NextResponse.json({ message: "Playback service unavailable" }, { status: 503 });
  }
}

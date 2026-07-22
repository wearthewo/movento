import { cookies } from "next/headers";
import { NextResponse } from "next/server";

export async function POST(_request: Request, context: { params: Promise<{ assetId: string }> }) {
  const { assetId } = await context.params;
  const token = (await cookies()).get("movento_access")?.value;
  try {
    const response = await fetch(`${process.env.GATEWAY_URL ?? "http://localhost:8080"}/api/v1/playback/${assetId}/session`, { method: "POST", headers: token ? { Authorization: `Bearer ${token}` } : {} });
    return new NextResponse(await response.text(), { status: response.status, headers: { "Content-Type": "application/json" } });
  } catch {
    if (process.env.NODE_ENV !== "production") return NextResponse.json({ manifestUrl: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", demo: true });
    return NextResponse.json({ message: "Playback service unavailable" }, { status: 503 });
  }
}

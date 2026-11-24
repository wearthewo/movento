import { NextRequest, NextResponse } from "next/server";
import { gatewayUrl } from "@/lib/session";

/**
 * Signed media URLs (/api/media/{session}/{title}/master.m3u8?exp=&sig=) are checked by streaming-service,
 * which answers with a redirect to the media origin. We pass that redirect straight to the player.
 */
export async function GET(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  const { path } = await params;
  if (path.length !== 3 || !/^\d+$/.test(path[0]) || !/^\d+$/.test(path[1]) || path[2] !== "master.m3u8") {
    return NextResponse.json({ message: "Not found" }, { status: 404 });
  }
  try {
    const upstream = await fetch(`${gatewayUrl()}/api/v1/media/${path.join("/")}${request.nextUrl.search}`, {
      redirect: "manual", cache: "no-store", signal: AbortSignal.timeout(5000),
    });
    const location = upstream.headers.get("location");
    if (upstream.status >= 300 && upstream.status < 400 && location) {
      const response = NextResponse.redirect(new URL(location, request.url), 302);
      response.headers.set("Cache-Control", "no-store");
      return response;
    }
    return NextResponse.json({ message: "This playback link has expired. Start the title again." }, { status: upstream.status === 200 ? 502 : upstream.status });
  } catch {
    return NextResponse.json({ message: "Media service unavailable" }, { status: 503 });
  }
}

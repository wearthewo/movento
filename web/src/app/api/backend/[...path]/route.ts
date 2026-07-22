import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const gateway = process.env.GATEWAY_URL ?? "http://localhost:8080";

async function proxy(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params;
  const token = (await cookies()).get("movento_access")?.value;
  if (!token) return NextResponse.json({ message: "Unauthorized" }, { status: 401 });

  const url = new URL(`${gateway}/api/v1/${path.join("/")}`);
  request.nextUrl.searchParams.forEach((value, key) => url.searchParams.append(key, value));
  const started = Date.now();
  const timeoutMs = path[0] === "billing" ? 60000 : 20000;

  try {
    const upstream = await fetch(url, {
      method: request.method,
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": request.headers.get("content-type") ?? "application/json",
      },
      body: ["GET", "HEAD"].includes(request.method) ? undefined : await request.text(),
      cache: "no-store",
      signal: AbortSignal.timeout(timeoutMs),
    });
    const elapsed = Date.now() - started;
    if (elapsed > 1000) console.warn(`[movento-web] slow backend request ${request.method} ${url.pathname} ${elapsed}ms`);
    return new NextResponse(await upstream.text(), {
      status: upstream.status,
      headers: { "Content-Type": upstream.headers.get("content-type") ?? "application/json" },
    });
  } catch {
    return NextResponse.json({ message: "Service unavailable" }, { status: 503 });
  }
}

export { proxy as GET, proxy as POST, proxy as PUT, proxy as PATCH, proxy as DELETE };

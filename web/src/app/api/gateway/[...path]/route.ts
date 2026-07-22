import { NextRequest, NextResponse } from "next/server";

const gateway = process.env.GATEWAY_URL ?? "http://localhost:8080";

async function forward(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  const { path } = await params;
  const token = request.cookies.get("movento_access")?.value;
  const started = Date.now();
  const response = await fetch(`${gateway}/${path.join("/")}${request.nextUrl.search}`, {
    method: request.method,
    headers: {
      "Content-Type": request.headers.get("content-type") ?? "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(request.headers.get("x-profile-id") ? { "X-Profile-Id": request.headers.get("x-profile-id")! } : {}),
    },
    body: ["GET", "HEAD"].includes(request.method) ? undefined : await request.text(),
    cache: "no-store",
    signal: AbortSignal.timeout(10000),
  });
  const elapsed = Date.now() - started;
  if (elapsed > 1000) console.warn(`[movento-web] slow gateway request ${request.method} /${path.join("/")} ${elapsed}ms`);
  return new NextResponse(response.body, {
    status: response.status,
    headers: { "content-type": response.headers.get("content-type") ?? "application/json" },
  });
}

export const GET = forward;
export const POST = forward;
export const PUT = forward;
export const PATCH = forward;
export const DELETE = forward;

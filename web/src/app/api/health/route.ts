import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

/** Liveness probe for Docker/Caddy. Does not touch the backend. */
export function GET() {
  return NextResponse.json({ status: "UP" });
}

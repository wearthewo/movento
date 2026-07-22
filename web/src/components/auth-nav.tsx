"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { CreditCard, LogOut, UserRound } from "lucide-react";
import type { CurrentUser } from "@/lib/types";

export function AuthNav() {
  const [signedIn, setSignedIn] = useState(false);
  const [user, setUser] = useState<CurrentUser | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    const timer = window.setTimeout(async () => {
      const marker = document.cookie.split("; ").some((cookie) => cookie === "movento_signed_in=1");
      setSignedIn(marker);
      try {
        const response = await fetch("/api/backend/users/me", { signal: controller.signal });
        if (!response.ok) {
          setSignedIn(false);
          setUser(null);
          document.cookie = "movento_signed_in=; Max-Age=0; path=/";
          return;
        }
        const payload = await response.json() as CurrentUser;
        document.cookie = "movento_signed_in=1; Max-Age=2592000; path=/";
        setSignedIn(true);
        setUser(payload);
      } catch {
        // Keep the local marker if the backend is warming up; protected pages will still enforce auth.
      }
    }, 0);
    return () => {
      controller.abort();
      window.clearTimeout(timer);
    };
  }, []);

  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" });
    document.cookie = "movento_signed_in=; Max-Age=0; path=/";
    document.cookie = "movento_profile=; Max-Age=0; path=/";
    setSignedIn(false);
    setUser(null);
    window.location.assign("/sign-in?loggedOut=1");
  }

  const displayName = user?.firstName || user?.email?.split("@")[0] || "member";

  return <div className="flex items-center gap-2 text-sm">
    <Link href="/account" className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-2 text-white/85 transition hover:bg-white/10 hover:text-white">
      <CreditCard className="size-4" />
      <span className="hidden sm:inline">Billing</span>
    </Link>
    {signedIn ? <>
      <Link href="/account" className="hidden max-w-48 truncate rounded-full border border-emerald-400/25 bg-emerald-400/10 px-3 py-2 text-emerald-100 transition hover:bg-emerald-400/15 md:inline-block" title={user?.email ?? "Signed in"}>
        Welcome, {displayName}
      </Link>
      <Link href="/profiles" className="rounded-full p-2 text-white/80 transition hover:bg-white/10 hover:text-white" aria-label="Profiles">
        <UserRound className="size-5" />
      </Link>
      <button onClick={logout} className="hidden rounded-full p-2 text-white/70 transition hover:bg-white/10 hover:text-white sm:inline-flex" aria-label="Sign out">
        <LogOut className="size-5" />
      </button>
    </> : <Link href="/sign-in" className="rounded-full border border-primary/50 bg-primary/15 px-3 py-2 font-semibold text-white transition hover:bg-primary/25">
      Sign in
    </Link>}
  </div>;
}

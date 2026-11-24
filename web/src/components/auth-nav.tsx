"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { ChevronDown, CreditCard, LogOut, Pencil, Users } from "lucide-react";
import { ProfileAvatar } from "./profile-avatar";

export function AuthNav({ signedIn, profile }: { signedIn: boolean; profile: { name: string; kids: boolean } | null }) {
  const [open, setOpen] = useState(false);
  const menu = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const close = (event: MouseEvent | KeyboardEvent) => {
      if (event instanceof KeyboardEvent ? event.key === "Escape" : !menu.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", close);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", close);
    };
  }, [open]);

  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" });
    window.location.assign("/sign-in?loggedOut=1");
  }

  if (!signedIn) {
    return <Link href="/sign-in" className="rounded bg-primary px-4 py-1.5 text-sm font-semibold text-white transition hover:brightness-110">Sign in</Link>;
  }

  return <div ref={menu} className="relative">
    <button onClick={() => setOpen((value) => !value)} aria-haspopup="menu" aria-expanded={open} className="flex items-center gap-1.5" aria-label="Account menu">
      <ProfileAvatar name={profile?.name ?? "?"} kids={profile?.kids} className="size-8 rounded text-sm" />
      <ChevronDown className={`size-4 transition ${open ? "rotate-180" : ""}`} />
    </button>
    {open && <div role="menu" className="absolute right-0 top-11 w-56 overflow-hidden rounded-md border border-white/10 bg-black/95 py-2 text-sm shadow-2xl">
      {profile && <p className="truncate px-4 pb-2 text-white/60">Watching as <span className="font-semibold text-white">{profile.name}</span></p>}
      <MenuLink href="/profiles" icon={<Users className="size-4" />}>Switch profile</MenuLink>
      <MenuLink href="/profiles?manage=1" icon={<Pencil className="size-4" />}>Manage profiles</MenuLink>
      <MenuLink href="/account" icon={<CreditCard className="size-4" />}>Account &amp; billing</MenuLink>
      <button role="menuitem" onClick={logout} className="mt-1 flex w-full items-center gap-3 border-t border-white/10 px-4 pt-3 text-left hover:underline">
        <LogOut className="size-4" />Sign out of Movento
      </button>
    </div>}
  </div>;
}

function MenuLink({ href, icon, children }: { href: string; icon: React.ReactNode; children: React.ReactNode }) {
  return <Link role="menuitem" href={href} className="flex items-center gap-3 px-4 py-2 hover:underline">{icon}{children}</Link>;
}

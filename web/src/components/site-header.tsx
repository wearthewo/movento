"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { Search } from "lucide-react";
import { cn } from "@/lib/utils";
import { AuthNav } from "./auth-nav";
import { Logo } from "./logo";

const NAV = [
  { href: "/browse", label: "Home" },
  { href: "/series", label: "Series" },
  { href: "/movies", label: "Movies" },
  { href: "/my-list", label: "My List" },
];

export function SiteHeader({ signedIn, profile }: { signedIn: boolean; profile: { name: string; kids: boolean } | null }) {
  const pathname = usePathname();
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  if (pathname.startsWith("/watch")) return null; // the player is full-screen

  return <header className={cn(
    "fixed inset-x-0 top-0 z-50 flex h-16 items-center gap-4 px-5 transition-colors duration-500 md:h-[4.25rem] md:gap-10 md:px-12",
    scrolled ? "bg-[#0b0b0b]/95 shadow-lg shadow-black/40 backdrop-blur" : "bg-gradient-to-b from-black/85 to-transparent",
  )}>
    <Logo />
    <nav aria-label="Primary" className="hidden gap-5 text-sm lg:flex">
      {NAV.map((item) => {
        const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
        return <Link key={item.href} href={item.href} aria-current={active ? "page" : undefined}
          className={cn("transition hover:text-white/70", active ? "font-semibold text-white" : "text-white/80")}>{item.label}</Link>;
      })}
    </nav>
    <nav aria-label="Primary mobile" className="flex gap-4 text-sm text-white/80 lg:hidden">
      <Link href="/series" className="hidden sm:inline">Series</Link>
      <Link href="/movies" className="hidden sm:inline">Movies</Link>
      <Link href="/my-list">My List</Link>
    </nav>
    <div className="ml-auto flex items-center gap-2 sm:gap-4">
      <Link href="/search" className="rounded-full p-2 text-white/85 transition hover:bg-white/10 hover:text-white" aria-label="Search">
        <Search className="size-5" />
      </Link>
      {profile?.kids && <span className="rounded bg-sky-500 px-2 py-0.5 text-xs font-black uppercase text-white">Kids</span>}
      <AuthNav signedIn={signedIn} profile={profile} />
    </div>
  </header>;
}

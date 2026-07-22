import Link from "next/link";
import { Search } from "lucide-react";
import { AuthNav } from "./auth-nav";
import { Logo } from "./logo";

export function SiteHeader() {
  return <header className="fixed inset-x-0 top-0 z-50 flex min-h-16 items-center gap-4 bg-gradient-to-b from-black/95 via-black/80 to-transparent px-5 py-3 md:gap-8 md:px-12">
    <Logo />
    <nav className="hidden gap-5 text-sm text-muted-foreground md:flex">
      <Link href="/browse" className="hover:text-white">Home</Link>
      <Link href="/movies" className="hover:text-white">Movies</Link>
      <Link href="/series" className="hover:text-white">Series</Link>
      <Link href="/my-list" className="hover:text-white">My List</Link>
    </nav>
    <div className="ml-auto flex items-center gap-2 sm:gap-4">
      <Link href="/search" className="rounded-full p-2 text-white/80 transition hover:bg-white/10 hover:text-white" aria-label="Search">
        <Search className="size-5" />
      </Link>
      <AuthNav />
    </div>
  </header>;
}

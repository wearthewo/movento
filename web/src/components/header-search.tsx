"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { Search, X } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Netflix-style search: the icon expands an input right in the header and results update as you type
 * (debounced). It works instantly on any page because opening it needs no server round trip.
 */
export function HeaderSearch() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const onSearchPage = pathname === "/search";
  const [open, setOpen] = useState(onSearchPage);
  const [query, setQuery] = useState(onSearchPage ? params.get("q") ?? "" : "");
  const input = useRef<HTMLInputElement>(null);
  const debounce = useRef<number | undefined>(undefined);

  // Follow navigation: open on /search, close elsewhere (render-phase sync, no effect needed).
  const [lastPath, setLastPath] = useState(pathname);
  if (pathname !== lastPath) {
    setLastPath(pathname);
    setOpen(onSearchPage);
    if (!onSearchPage) setQuery("");
  }

  // Keep the box in step when the URL query changes from elsewhere (quick-search chips, back button).
  const urlQuery = onSearchPage ? params.get("q") ?? "" : "";
  const [lastUrlQuery, setLastUrlQuery] = useState(urlQuery);
  if (urlQuery !== lastUrlQuery) {
    setLastUrlQuery(urlQuery);
    if (urlQuery.trim() !== query.trim()) setQuery(urlQuery);
  }

  useEffect(() => {
    if (open) input.current?.focus();
  }, [open]);

  useEffect(() => () => window.clearTimeout(debounce.current), []);

  function update(value: string) {
    setQuery(value);
    window.clearTimeout(debounce.current);
    debounce.current = window.setTimeout(() => {
      const target = value.trim() ? `/search?q=${encodeURIComponent(value.trim())}` : "/search";
      if (onSearchPage) router.replace(target, { scroll: false });
      else router.push(target);
    }, 300);
  }

  function close() {
    window.clearTimeout(debounce.current);
    setQuery("");
    setOpen(false);
    if (onSearchPage) router.push("/browse");
  }

  if (!open) {
    return <button onClick={() => setOpen(true)} className="rounded-full p-2 text-white/85 transition hover:bg-white/10 hover:text-white" aria-label="Search">
      <Search className="size-5" />
    </button>;
  }

  return <form role="search" onSubmit={(event) => { event.preventDefault(); update(query); }}
    className={cn("flex h-9 items-center gap-2 border border-white/80 bg-black/85 px-2.5 transition-all", "w-[44vw] sm:w-72")}>
    <Search className="size-4 flex-none text-white/80" aria-hidden />
    <input ref={input} value={query} onChange={(event) => update(event.target.value)}
      onKeyDown={(event) => { if (event.key === "Escape") close(); }}
      onBlur={() => { if (!query.trim() && !onSearchPage) setOpen(false); }}
      placeholder="Titles, genres, years" aria-label="Search titles"
      className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-white/50" />
    {(query || onSearchPage) && <button type="button" onMouseDown={(event) => event.preventDefault()} onClick={close} aria-label="Clear search" className="text-white/70 hover:text-white">
      <X className="size-4" />
    </button>}
  </form>;
}

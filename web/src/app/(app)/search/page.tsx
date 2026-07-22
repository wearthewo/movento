import { Search } from "lucide-react";
import { TitleCard } from "@/components/title-card";
import { searchTitles } from "@/lib/api";

const quickSearches = ["sci-fi", "documentary", "thriller", "family", "2026"];

export default async function SearchPage({ searchParams }: { searchParams: Promise<{ q?: string }> }) {
  const { q = "" } = await searchParams;
  const query = q.trim();
  const results = await searchTitles(query);

  return (
    <main className="min-h-screen px-5 pb-20 pt-28 md:px-12">
      <section className="max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[.22em] text-primary">Find your next watch</p>
        <h1 className="mt-3 text-4xl font-black md:text-6xl">Search Movento</h1>
        <form className="mt-8">
          <label className="relative block">
            <Search className="absolute left-4 top-1/2 size-5 -translate-y-1/2 text-muted-foreground" />
            <input autoFocus name="q" defaultValue={query} placeholder="Titles, genres, years, ratings..." className="h-14 w-full rounded-md border bg-card pl-12 pr-4 text-lg outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/30" />
          </label>
        </form>
        <div className="mt-4 flex flex-wrap gap-2">
          {quickSearches.map((term) => (
            <a key={term} href={`/search?q=${encodeURIComponent(term)}`} className="rounded-full border px-3 py-1 text-sm text-muted-foreground transition hover:border-primary hover:text-white">
              {term}
            </a>
          ))}
        </div>
      </section>

      <div className="mt-10 flex items-center justify-between gap-4">
        <p className="text-sm text-muted-foreground">{query ? `${results.length} result${results.length === 1 ? "" : "s"} for "${query}"` : "Browse the full local catalog"}</p>
      </div>

      {results.length > 0 ? (
        <div className="mt-5 grid grid-cols-2 gap-5 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-7">
          {results.map((item) => <TitleCard key={item.id} item={item} />)}
        </div>
      ) : (
        <div className="mt-8 rounded-md border bg-card/60 p-8">
          <h2 className="text-xl font-semibold">No matching titles</h2>
          <p className="mt-2 text-sm text-muted-foreground">Try a genre like sci-fi, mystery, documentary, action, family, or a release year.</p>
        </div>
      )}
    </main>
  );
}

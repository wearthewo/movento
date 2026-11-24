import Link from "next/link";
import { TitleCard } from "@/components/title-card";
import { searchTitles } from "@/lib/api";

export const metadata = { title: "Search" };

const quickSearches = ["sci-fi", "documentary", "thriller", "family", "2026"];

/** Results for the header search box (it opens automatically on this page). */
export default async function SearchPage({ searchParams }: { searchParams: Promise<{ q?: string }> }) {
  const { q = "" } = await searchParams;
  const query = q.trim();
  const results = await searchTitles(query);

  return (
    <main className="min-h-screen px-5 pb-20 pt-28 md:px-12">
      <h1 className="text-2xl font-bold text-white/90 md:text-3xl">
        {query ? <>Results for <span className="text-white">“{query}”</span></> : "Explore the catalog"}
      </h1>
      <div className="mt-4 flex flex-wrap items-center gap-2 text-sm">
        <span className="text-white/50">Try:</span>
        {quickSearches.map((term) => (
          <Link key={term} href={`/search?q=${encodeURIComponent(term)}`} replace scroll={false}
            className="rounded-full border px-3 py-1 text-white/70 transition hover:border-primary hover:text-white">{term}</Link>
        ))}
      </div>

      {results.length > 0 ? (
        <>
          <p className="mt-8 text-sm text-white/55">{results.length} title{results.length === 1 ? "" : "s"}</p>
          <div className="mt-3 grid grid-cols-2 gap-x-3 gap-y-8 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
            {results.map((item) => <TitleCard key={item.id} item={item} />)}
          </div>
        </>
      ) : (
        <div className="mt-10 max-w-lg text-white/70">
          <p className="text-lg">Your search for “{query}” did not have any matches.</p>
          <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-white/55">
            <li>Try different keywords</li>
            <li>Try a genre like sci-fi, mystery, documentary or family</li>
            <li>Try a release year such as 2025</li>
          </ul>
        </div>
      )}
    </main>
  );
}

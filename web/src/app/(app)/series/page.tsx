import { ContentRail } from "@/components/content-rail";
import { TitleCard } from "@/components/title-card";
import { getTitles } from "@/lib/api";

export const metadata = { title: "Series" };

/** Genre rows first (like Netflix category pages), then the full A–Z grid. */
export default async function SeriesPage() {
  const items = await getTitles("SERIES");
  const genres = [...new Set(items.flatMap((item) => item.genres))].sort();
  const rails = genres
    .map((genre) => ({ id: genre, title: genre, items: items.filter((item) => item.genres.includes(genre)) }))
    .filter((rail) => rail.items.length >= 2);

  return <main className="pb-24 pt-24">
    <h1 className="mb-6 px-5 text-3xl font-black md:px-12 md:text-4xl">Series</h1>
    <div className="space-y-8">{rails.map((rail) => <ContentRail key={rail.id} rail={rail} />)}</div>
    <section className="mt-12 px-5 md:px-12">
      <h2 className="mb-4 text-xl font-bold">All Series</h2>
      <div className="grid grid-cols-2 gap-x-3 gap-y-8 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
        {[...items].sort((a, b) => a.title.localeCompare(b.title)).map((item) => <TitleCard key={item.id} item={item} />)}
      </div>
    </section>
  </main>;
}

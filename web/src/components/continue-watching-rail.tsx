import type { ContinueWatchingItem } from "@/lib/types";
import { TitleCard } from "./title-card";

function remaining(item: ContinueWatchingItem) {
  const minutes = Math.max(1, Math.round((item.progress.durationSeconds - item.progress.progressSeconds) / 60));
  return item.title.type === "SERIES" ? `Resume episode · ${minutes}m left` : `${minutes}m left`;
}

/** Cards jump straight back into the player at the saved position. */
export function ContinueWatchingRail({ items }: { items: ContinueWatchingItem[] }) {
  return <section aria-labelledby="rail-continue">
    <h2 id="rail-continue" className="mb-2 px-5 text-lg font-bold md:px-12 md:text-xl">Continue Watching</h2>
    <div className="no-scrollbar flex snap-x gap-2 overflow-x-auto px-5 py-3 md:gap-3 md:px-12">
      {items.map((item) => {
        const ep = item.progress.episodeKey ? `?ep=${encodeURIComponent(item.progress.episodeKey)}` : "";
        return <div key={item.title.id} className="w-[66vw] flex-none snap-start sm:w-[40vw] md:w-[28vw] lg:w-[21vw] xl:w-[17.5vw]">
          <TitleCard item={item.title} href={`/watch/${item.title.id}${ep}`} progressPercent={item.progress.progressPercent} subtitle={remaining(item)} />
        </div>;
      })}
    </div>
  </section>;
}

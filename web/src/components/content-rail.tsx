import type { HomeRail } from "@/lib/types";
import { TitleCard } from "./title-card";

export function ContentRail({ rail }: { rail: HomeRail }) {
  return (
    <section className="space-y-3">
      <div className="flex items-end justify-between gap-4">
        <h2 className="text-xl font-semibold md:text-2xl">{rail.title}</h2>
        <p className="hidden text-sm text-muted-foreground sm:block">{rail.items.length} titles</p>
      </div>
      <div className="rail-fade -mx-5 flex snap-x gap-4 overflow-x-auto overscroll-x-contain px-5 pb-4 scroll-smooth no-scrollbar md:-mx-12 md:px-12">
        {rail.items.map((item) => (
          <div key={item.id} className="w-[42vw] flex-none snap-start sm:w-[29vw] md:w-[18vw] lg:w-[14vw] xl:w-[11vw]">
            <TitleCard item={item} />
          </div>
        ))}
      </div>
    </section>
  );
}

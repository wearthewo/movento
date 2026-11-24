"use client";

import { useRef, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import type { HomeRail } from "@/lib/types";
import { TitleCard } from "./title-card";

export function ContentRail({ rail }: { rail: HomeRail }) {
  const scroller = useRef<HTMLDivElement>(null);
  const [atStart, setAtStart] = useState(true);
  const [atEnd, setAtEnd] = useState(false);
  const top10 = rail.kind === "top10";

  function page(direction: 1 | -1) {
    const el = scroller.current;
    if (el) el.scrollBy({ left: direction * el.clientWidth * 0.9, behavior: "smooth" });
  }

  function onScroll() {
    const el = scroller.current;
    if (!el) return;
    setAtStart(el.scrollLeft < 8);
    setAtEnd(el.scrollLeft + el.clientWidth >= el.scrollWidth - 8);
  }

  return (
    <section className="group/rail relative" aria-labelledby={`rail-${rail.id}`}>
      <h2 id={`rail-${rail.id}`} className="mb-2 px-5 text-lg font-bold md:px-12 md:text-xl">{rail.title}</h2>
      <div ref={scroller} onScroll={onScroll} className="no-scrollbar flex snap-x gap-2 overflow-x-auto scroll-smooth px-5 py-3 md:gap-3 md:px-12">
        {rail.items.map((item, index) => (
          <div key={item.id} className={top10 ? "flex w-[78vw] flex-none snap-start items-end sm:w-[44vw] md:w-[31vw] lg:w-[24vw] xl:w-[21vw]" : "w-[66vw] flex-none snap-start sm:w-[40vw] md:w-[28vw] lg:w-[21vw] xl:w-[17.5vw]"}>
            {top10 && <span aria-hidden className="-mr-3 select-none text-[5.5rem] font-black leading-[.8] text-[#0b0b0b] [-webkit-text-stroke:3px_rgba(255,255,255,.55)] md:text-[7.5rem]">{index + 1}</span>}
            <div className="min-w-0 flex-1">
              <TitleCard item={item} />
            </div>
          </div>
        ))}
      </div>
      {!atStart && <RailButton side="left" onClick={() => page(-1)} />}
      {!atEnd && rail.items.length > 4 && <RailButton side="right" onClick={() => page(1)} />}
    </section>
  );
}

function RailButton({ side, onClick }: { side: "left" | "right"; onClick: () => void }) {
  const Icon = side === "left" ? ChevronLeft : ChevronRight;
  return <button onClick={onClick} aria-label={side === "left" ? "Scroll left" : "Scroll right"}
    className={`absolute bottom-10 top-11 z-20 hidden w-10 place-items-center bg-black/55 opacity-0 transition hover:bg-black/75 group-hover/rail:opacity-100 md:grid md:w-12 ${side === "left" ? "left-0" : "right-0"}`}>
    <Icon className="size-8 transition hover:scale-125" />
  </button>;
}

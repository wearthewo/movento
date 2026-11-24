"use client";

import Image from "next/image";
import Link from "next/link";
import { useState } from "react";
import { Play } from "lucide-react";
import type { Season } from "@/lib/types";

export function EpisodeList({ contentId, seasons, signedIn }: { contentId: string; seasons: Season[]; signedIn: boolean }) {
  const [seasonNumber, setSeasonNumber] = useState(seasons[0]?.seasonNumber ?? 1);
  const season = seasons.find((s) => s.seasonNumber === seasonNumber) ?? seasons[0];

  return <section aria-labelledby="episodes">
    <div className="mb-4 flex items-center justify-between gap-4">
      <h2 id="episodes" className="text-2xl font-bold">Episodes</h2>
      {seasons.length > 1 && <select value={seasonNumber} onChange={(e) => setSeasonNumber(Number(e.target.value))} aria-label="Season"
        className="rounded border border-white/30 bg-zinc-900 px-4 py-2 text-sm font-semibold">
        {seasons.map((s) => <option key={s.seasonNumber} value={s.seasonNumber}>{s.title}</option>)}
      </select>}
      {seasons.length === 1 && <span className="text-lg text-white/70">{season.title}</span>}
    </div>
    <ol className="divide-y divide-white/10 rounded-md border border-white/10">
      {season.episodes.map((episode) => {
        const href = `/watch/${contentId}?ep=${episode.id}`;
        return <li key={episode.id}>
          <Link href={signedIn ? href : `/sign-in?next=${encodeURIComponent(href)}`} className="group flex items-center gap-4 p-4 transition hover:bg-white/5 md:gap-6 md:p-6">
            <span className="w-6 text-center text-2xl text-white/50">{episode.episodeNumber}</span>
            <span className="relative aspect-video w-32 flex-none overflow-hidden rounded bg-muted md:w-44">
              {episode.thumbnailUrl && <Image src={episode.thumbnailUrl} alt="" fill sizes="176px" className="object-cover" />}
              <span className="absolute inset-0 grid place-items-center opacity-0 transition group-hover:opacity-100">
                <span className="grid size-10 place-items-center rounded-full border-2 border-white bg-black/50"><Play className="size-4 fill-white" /></span>
              </span>
            </span>
            <span className="min-w-0 flex-1">
              <span className="flex justify-between gap-3 font-semibold"><span className="truncate">{episode.title}</span>{episode.runtimeMinutes && <span className="font-normal text-white/60">{episode.runtimeMinutes}m</span>}</span>
              {episode.synopsis && <span className="mt-1 line-clamp-2 text-sm text-white/60">{episode.synopsis}</span>}
            </span>
          </Link>
        </li>;
      })}
    </ol>
  </section>;
}

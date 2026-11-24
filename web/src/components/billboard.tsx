import Image from "next/image";
import Link from "next/link";
import { Info } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { Title } from "@/lib/types";
import { PlaybackButton } from "./playback-button";

export function Billboard({ title, signedIn }: { title: Title; signedIn: boolean }) {
  return <section className="relative isolate flex h-[82vh] min-h-[520px] items-end pb-[14vh] md:h-[92vh]">
    <Image src={title.backdropUrl} alt="" fill priority sizes="100vw" className="-z-20 object-cover" />
    <div className="absolute inset-0 -z-10 bg-gradient-to-r from-black/90 via-black/45 to-transparent" />
    <div className="absolute inset-x-0 bottom-0 -z-10 h-1/2 bg-gradient-to-t from-[#0b0b0b] via-[#0b0b0b]/60 to-transparent" />
    <div className="max-w-2xl px-5 md:px-12">
      <p className="mb-3 flex items-center gap-2 text-xs font-bold uppercase tracking-[.3em] text-white/80">
        <span className="text-xl font-black tracking-normal text-primary">M</span>{title.type === "SERIES" ? "Series" : "Film"}
      </p>
      <h1 className="text-5xl font-black leading-[.95] drop-shadow-2xl md:text-7xl lg:text-8xl">{title.title}</h1>
      <div className="mt-4 flex flex-wrap items-center gap-3 text-sm text-white/80">
        {title.trending && <span className="font-bold text-emerald-400">Trending</span>}
        <span>{title.releaseYear}</span>
        <span className="rounded-sm border border-white/40 px-1.5 text-xs">{title.maturityRating}</span>
        <span>{title.type === "MOVIE" ? `${title.runtimeMinutes ?? 0}m` : "Series"}</span>
        <span className="text-white/60">{title.genres.slice(0, 3).join(" • ")}</span>
      </div>
      <p className="mt-4 line-clamp-3 max-w-xl text-base leading-7 text-white/85 drop-shadow md:text-lg">{title.synopsis}</p>
      <div className="mt-6 flex flex-wrap gap-3">
        <PlaybackButton contentId={title.id} signedIn={signedIn} />
        <Button asChild variant="outline" className="border-0 bg-zinc-500/60 px-6 text-base backdrop-blur hover:bg-zinc-500/45 md:px-8">
          <Link href={`/title/${title.slug}`}><Info className="size-5" />More Info</Link>
        </Button>
      </div>
    </div>
  </section>;
}

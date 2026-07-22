import Image from "next/image";
import Link from "next/link";
import { Info, Search } from "lucide-react";
import { AccessGuide } from "@/components/access-guide";
import { ContentRail } from "@/components/content-rail";
import { PlaybackButton } from "@/components/playback-button";
import { Button } from "@/components/ui/button";
import { getHomeFeed } from "@/lib/api";

export default async function Browse() {
  const feed = await getHomeFeed();
  const hero = feed.featured;

  return (
    <main className="pb-20">
      <section className="relative flex min-h-[76vh] items-end px-5 pb-14 pt-28 md:px-12">
        <Image src={hero.backdropUrl} alt="" fill priority sizes="100vw" className="-z-20 object-cover" />
        <div className="absolute inset-0 -z-10 bg-gradient-to-r from-black via-black/65 to-black/10" />
        <div className="absolute inset-0 -z-10 bg-gradient-to-t from-background via-background/20 to-transparent" />
        <div className="max-w-3xl">
          <p className="mb-3 text-sm font-semibold uppercase tracking-[.25em] text-primary">Movento spotlight</p>
          <h1 className="text-5xl font-black leading-none md:text-7xl">{hero.title}</h1>
          <div className="mt-4 flex flex-wrap gap-3 text-sm text-white/70">
            <span>{hero.releaseYear}</span>
            <span>{hero.maturityRating}</span>
            <span>{hero.type === "MOVIE" ? `${hero.runtimeMinutes ?? 0} min` : "Series"}</span>
            <span>{hero.genres.slice(0, 3).join(" / ")}</span>
          </div>
          <p className="mt-5 max-w-2xl text-base leading-7 text-white/75 md:text-lg">{hero.synopsis}</p>
          <div className="mt-7 flex flex-wrap items-start gap-3">
            <PlaybackButton contentId={hero.id} />
            <Button asChild variant="outline"><Link href={`/title/${hero.slug}`}><Info className="size-4" />More info</Link></Button>
            <Button asChild variant="ghost"><Link href="/search"><Search className="size-4" />Search catalog</Link></Button>
          </div>
        </div>
      </section>
      <AccessGuide />
      <div className="space-y-12 px-5 md:px-12">{feed.rails.map((rail) => <ContentRail key={rail.id} rail={rail} />)}</div>
    </main>
  );
}

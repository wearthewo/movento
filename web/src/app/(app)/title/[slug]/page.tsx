import type { Metadata } from "next";
import Image from "next/image";
import { notFound } from "next/navigation";
import { EpisodeList } from "@/components/episode-list";
import { PlaybackButton } from "@/components/playback-button";
import { TitleCard } from "@/components/title-card";
import { WatchlistButton } from "@/components/watchlist-button";
import { getEpisodes, getProgress, getSimilar, getTitle, isInMyList, isSignedIn } from "@/lib/api";

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const item = await getTitle((await params).slug);
  return item ? { title: item.title, description: item.synopsis, openGraph: { images: [item.backdropUrl] } } : {};
}

export default async function Details({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const item = await getTitle(slug);
  if (!item) notFound();

  const [signedIn, seasons, similar, saved, progress] = await Promise.all([
    isSignedIn(), item.type === "SERIES" ? getEpisodes(slug) : Promise.resolve([]), getSimilar(slug), isInMyList(item.id), getProgress(item.id),
  ]);
  const resume = progress && !progress.completed && progress.progressSeconds > 30;
  const episodeCount = seasons.reduce((sum, season) => sum + season.episodes.length, 0);
  const playLabel = resume ? "Resume" : item.type === "SERIES" && seasons[0]?.episodes[0] ? "Play S1:E1" : "Play";
  const playEpisode = resume ? progress.episodeKey || undefined : seasons[0]?.episodes[0]?.id;

  return <main className="min-h-screen pb-24">
    <section className="relative flex min-h-[78vh] items-end pb-12 pt-28">
      <Image src={item.backdropUrl} alt="" fill priority sizes="100vw" className="-z-20 object-cover" />
      <div className="absolute inset-0 -z-10 bg-gradient-to-r from-black via-black/70 to-transparent" />
      <div className="absolute inset-x-0 bottom-0 -z-10 h-2/3 bg-gradient-to-t from-[#0b0b0b] to-transparent" />
      <div className="max-w-3xl px-5 md:px-12">
        <h1 className="text-5xl font-black leading-none md:text-7xl">{item.title}</h1>
        <div className="mt-5 flex flex-wrap items-center gap-3 text-sm text-white/80">
          {item.trending && <span className="font-bold text-emerald-400">Trending now</span>}
          <span>{item.releaseYear}</span>
          <span className="rounded-sm border border-white/40 px-1.5 text-xs">{item.maturityRating}</span>
          <span>{item.type === "SERIES" ? `${seasons.length || 1} Season${seasons.length > 1 ? "s" : ""}${episodeCount ? ` · ${episodeCount} episodes` : ""}` : `${item.runtimeMinutes ?? 0}m`}</span>
          <span className="rounded-sm border border-white/40 px-1 text-[10px] font-bold">HD</span>
        </div>
        {resume && <div className="mt-5 flex max-w-sm items-center gap-3 text-sm text-white/70">
          <div className="h-1 flex-1 bg-white/25"><div className="h-full bg-primary" style={{ width: `${progress.progressPercent}%` }} /></div>
          <span>{Math.max(1, Math.round((progress.durationSeconds - progress.progressSeconds) / 60))}m left</span>
        </div>}
        <div className="mt-6 flex flex-wrap items-center gap-3">
          <PlaybackButton contentId={item.id} signedIn={signedIn} label={playLabel} episodeId={playEpisode} />
          <WatchlistButton contentId={item.id} initialSaved={saved} signedIn={signedIn} />
        </div>
        <p className="mt-6 max-w-2xl text-lg leading-8 text-white/85">{item.synopsis}</p>
        <p className="mt-3 text-sm text-white/55"><span className="text-white/40">Genres: </span>{item.genres.join(", ")}</p>
      </div>
    </section>

    <div className="space-y-14 px-5 md:px-12">
      {seasons.length > 0 && <EpisodeList contentId={item.id} seasons={seasons} signedIn={signedIn} />}
      {similar.length > 0 && <section aria-labelledby="similar">
        <h2 id="similar" className="mb-4 text-2xl font-bold">More Like This</h2>
        <div className="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6">
          {similar.map((title) => <TitleCard key={title.id} item={title} />)}
        </div>
      </section>}
    </div>
  </main>;
}

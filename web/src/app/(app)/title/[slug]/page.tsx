import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, LockKeyhole } from "lucide-react";
import { PlaybackButton } from "@/components/playback-button";
import { WatchlistButton } from "@/components/watchlist-button";
import { getTitle } from "@/lib/api";

export default async function Details({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const item = await getTitle(slug);
  if (!item) notFound();

  return <main className="min-h-screen">
    <section className="relative min-h-[78vh] px-5 pb-16 pt-24 md:px-12 md:pt-28">
      <Image src={item.backdropUrl} alt="" fill priority sizes="100vw" className="-z-20 object-cover" />
      <div className="absolute inset-0 -z-10 bg-gradient-to-r from-black via-black/70 to-transparent" />
      <div className="absolute inset-0 -z-10 bg-gradient-to-t from-background via-background/30 to-transparent" />

      <Link href="/browse" className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-black/45 px-4 py-2 text-sm text-white/80 backdrop-blur transition hover:bg-white/10 hover:text-white">
        <ArrowLeft className="size-4" />
        Back to browse
      </Link>

      <div className="flex min-h-[calc(78vh-7rem)] items-end">
        <div className="max-w-2xl">
          <p className="text-sm uppercase tracking-[.22em] text-primary">{item.type}</p>
          <h1 className="mt-3 text-5xl font-black md:text-7xl">{item.title}</h1>
          <div className="mt-4 flex flex-wrap gap-3 text-sm text-white/70">
            <span>{item.releaseYear}</span>
            <span>{item.maturityRating}</span>
            {item.runtimeMinutes && <span>{item.runtimeMinutes} min</span>}
          </div>
          <p className="mt-5 text-lg leading-8 text-white/75">{item.synopsis}</p>
          <p className="mt-3 text-sm text-white/55">{item.genres.join(" · ")}</p>
          <div className="mt-7 flex flex-wrap items-start gap-3">
            <PlaybackButton contentId={item.id} />
            <WatchlistButton contentId={item.id} />
          </div>
          <div className="mt-6 max-w-xl rounded-xl border border-white/10 bg-black/35 p-4 text-sm text-white/65">
            <p className="flex items-center gap-2 font-semibold text-white"><LockKeyhole className="size-4 text-primary" />Access rules</p>
            <p className="mt-2">Guests can browse, search, and view details. Sign in to create profiles, save to My List, open billing, and start playback.</p>
          </div>
        </div>
      </div>
    </section>
  </main>;
}

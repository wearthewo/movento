import Link from "next/link";
import { redirect } from "next/navigation";
import { VideoPlayer } from "@/components/video-player";
import { getEpisodes, getProgress, getTitleById, isSignedIn } from "@/lib/api";

export const metadata = { title: "Watch" };

export default async function WatchPage({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ ep?: string }> }) {
  const { id } = await params;
  const { ep } = await searchParams;
  if (!(await isSignedIn())) redirect(`/sign-in?next=${encodeURIComponent(`/watch/${id}${ep ? `?ep=${ep}` : ""}`)}`);

  // The resume point only needs the id, so fetch it alongside the title instead of after it.
  const [title, progress] = await Promise.all([getTitleById(id), getProgress(id)]);
  if (!title) {
    return <main className="grid min-h-screen place-items-center bg-black text-center">
      <div><h1 className="text-3xl font-bold">This title isn&apos;t available</h1><Link href="/browse" className="mt-4 inline-block underline">Back to browse</Link></div>
    </main>;
  }

  const seasons = title.type === "SERIES" ? await getEpisodes(title.slug) : [];
  const episodes = seasons.flatMap((season) => season.episodes.map((episode) => ({ ...episode, seasonNumber: season.seasonNumber })));
  const index = ep ? episodes.findIndex((episode) => episode.id === ep) : 0;
  const current = episodes[Math.max(index, 0)];
  const next = current ? episodes[episodes.indexOf(current) + 1] : undefined;
  const episodeKey = current?.id ?? "";
  const startAt = progress && !progress.completed && progress.episodeKey === episodeKey ? progress.progressSeconds : 0;

  return <main className="fixed inset-0 z-[60] bg-black">
    {/* key: remount per episode so player state (countdown, resume point) never leaks between episodes */}
    <VideoPlayer
      key={episodeKey || id}
      assetId={id}
      episodeKey={episodeKey}
      startAt={startAt}
      poster={title.backdropUrl}
      heading={title.title}
      subheading={current ? `S${current.seasonNumber}:E${current.episodeNumber} “${current.title}”` : undefined}
      backHref={`/title/${title.slug}`}
      nextHref={next ? `/watch/${id}?ep=${next.id}` : undefined}
      nextLabel={next ? `S${next.seasonNumber}:E${next.episodeNumber} · ${next.title}` : undefined}
    />
  </main>;
}

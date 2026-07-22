import Link from "next/link";
import { cookies } from "next/headers";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { VideoPlayer } from "@/components/video-player";
import { getTitles } from "@/lib/api";

export default async function WatchPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const title = (await getTitles()).find((item) => item.id === id);
  const signedIn = Boolean((await cookies()).get("movento_access")?.value);

  if (!signedIn) {
    return <main className="grid min-h-screen place-items-center bg-black px-5 text-center">
      <div className="max-w-lg">
        <p className="text-sm font-semibold uppercase tracking-[.25em] text-primary">Member feature</p>
        <h1 className="mt-3 text-4xl font-black">Sign in to watch</h1>
        <p className="mt-4 text-white/65">Guests can browse the catalog and open title details. Playback, resume progress, My List, and billing require a signed-in account.</p>
        <div className="mt-7 flex justify-center gap-3">
          <Button asChild><Link href="/sign-in">Sign in</Link></Button>
          <Button asChild variant="outline"><Link href={title ? `/title/${title.slug}` : "/browse"}>Back to details</Link></Button>
        </div>
      </div>
    </main>;
  }

  return <main className="min-h-screen bg-black px-4 py-6 md:px-10">
    <Link href={title ? `/title/${title.slug}` : "/browse"} className="mb-5 inline-flex items-center gap-2 text-sm text-white/70 hover:text-white"><ArrowLeft className="size-4" />Back</Link>
    <VideoPlayer assetId={id} />
    <div className="mx-auto mt-7 max-w-5xl">
      <h1 className="text-2xl font-bold">{title?.title ?? "Movento presentation"}</h1>
      <p className="mt-2 text-sm text-white/55">Demo playback uses a public HLS test stream until an R2 manifest is configured.</p>
    </div>
  </main>;
}

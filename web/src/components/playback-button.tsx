import Link from "next/link";
import { Play } from "lucide-react";
import { Button } from "@/components/ui/button";

/** Server-rendered so it knows the auth state up front (no flash of "Sign in to watch"). */
export function PlaybackButton({ contentId, signedIn, label = "Play", episodeId }: { contentId: string; signedIn: boolean; label?: string; episodeId?: string }) {
  const watch = `/watch/${contentId}${episodeId ? `?ep=${episodeId}` : ""}`;
  return <Button asChild variant="secondary" className="px-6 text-base md:px-8">
    <Link href={signedIn ? watch : `/sign-in?next=${encodeURIComponent(watch)}`}><Play className="size-5 fill-current" />{label}</Link>
  </Button>;
}

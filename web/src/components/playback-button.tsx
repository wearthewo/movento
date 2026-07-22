"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { LockKeyhole, Play } from "lucide-react";
import { Button } from "@/components/ui/button";

export function PlaybackButton({ contentId, label = "Play" }: { contentId: string; label?: string }) {
  const [signedIn, setSignedIn] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setSignedIn(document.cookie.split("; ").some((cookie) => cookie === "movento_signed_in=1"));
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  if (!signedIn) {
    return <div className="space-y-2">
      <Button asChild variant="secondary">
        <Link href="/sign-in"><LockKeyhole className="size-4" />Sign in to watch</Link>
      </Button>
      <p className="max-w-60 text-xs text-amber-200">Playback, progress, and subscriptions are member features.</p>
    </div>;
  }

  return <Button asChild variant="secondary">
    <Link href={`/watch/${contentId}`}><Play className="size-4 fill-current" />{label}</Link>
  </Button>;
}

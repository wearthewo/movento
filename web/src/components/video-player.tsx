"use client";

import { useEffect, useRef, useState } from "react";
import Hls from "hls.js";

export function VideoPlayer({ assetId, fallbackUrl, poster }: { assetId: string; fallbackUrl?: string; poster?: string }) {
  const ref = useRef<HTMLVideoElement>(null);
  const lastSaved = useRef(0);
  const [message, setMessage] = useState("Preparing secure playback...");

  useEffect(() => {
    let hls: Hls | undefined;
    let cancelled = false;

    async function load() {
      const response = await fetch(`/api/playback/${assetId}`, { method: "POST" });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        setMessage(response.status === 401 ? "Sign in to start playback." : "Playback requires an active local test subscription or available demo session.");
        return;
      }
      const source = payload.manifestUrl || fallbackUrl || "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8";
      if (cancelled || !ref.current) return;
      if (ref.current.canPlayType("application/vnd.apple.mpegurl")) ref.current.src = source;
      else if (Hls.isSupported()) {
        hls = new Hls();
        hls.loadSource(source);
        hls.attachMedia(ref.current);
      } else {
        setMessage("HLS playback is not supported by this browser.");
        return;
      }
      setMessage("");
    }

    load().catch(() => setMessage("Video is temporarily unavailable."));
    return () => {
      cancelled = true;
      hls?.destroy();
    };
  }, [assetId, fallbackUrl]);

  function save(force = false) {
    const video = ref.current;
    if (!video || !Number.isFinite(video.duration)) return;
    if (!force && video.currentTime - lastSaved.current < 15) return;
    lastSaved.current = video.currentTime;
    void fetch(`/api/backend/library/progress/${assetId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ progressSeconds: Math.floor(video.currentTime), durationSeconds: Math.floor(video.duration) }),
    });
  }

  return <div className="relative aspect-video w-full overflow-hidden rounded-xl bg-black shadow-2xl">
    <video ref={ref} poster={poster} controls autoPlay className="h-full w-full" onTimeUpdate={() => save()} onPause={() => save(true)} onEnded={() => save(true)} onError={() => setMessage("Playback failed. Please try again.")} />
    {message && <div className="absolute inset-0 grid place-items-center bg-black px-5 text-center text-sm text-white/65">{message}</div>}
  </div>;
}

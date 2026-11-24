"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import Hls from "hls.js";
import { ArrowLeft, SkipForward } from "lucide-react";

const SAVE_EVERY_SECONDS = 10;
const NEXT_EPISODE_COUNTDOWN = 8;

interface Props {
  assetId: string;
  episodeKey: string;
  startAt: number;
  poster?: string;
  heading: string;
  subheading?: string;
  backHref: string;
  nextHref?: string;
  nextLabel?: string;
}

/**
 * Full-screen HLS player: resumes from the saved position, saves progress per episode,
 * offers "Next Episode" near the end with an auto-play countdown, and supports keyboard shortcuts.
 */
export function VideoPlayer({ assetId, episodeKey, startAt, poster, heading, subheading, backHref, nextHref, nextLabel }: Props) {
  const router = useRouter();
  const video = useRef<HTMLVideoElement>(null);
  const lastSaved = useRef(0);
  const saving = useRef(false);
  const hideTimer = useRef<number | undefined>(undefined);
  const [message, setMessage] = useState("Preparing playback…");
  const [needsPlan, setNeedsPlan] = useState(false);
  const [chromeVisible, setChromeVisible] = useState(true);
  const [showNext, setShowNext] = useState(false);
  const [countdown, setCountdown] = useState<number | null>(null);

  const save = useCallback((force = false) => {
    const el = video.current;
    if (!el || !Number.isFinite(el.duration) || el.duration <= 0) return;
    if (!force && Math.abs(el.currentTime - lastSaved.current) < SAVE_EVERY_SECONDS) return;
    // One request at a time, except the final save on pause/exit which must not be dropped.
    if (saving.current && !force) return;
    lastSaved.current = el.currentTime;
    saving.current = true;
    void fetch(`/api/backend/library/progress/${assetId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      keepalive: true,
      body: JSON.stringify({ progressSeconds: Math.floor(el.currentTime), durationSeconds: Math.max(1, Math.floor(el.duration)), episodeKey }),
    }).catch(() => undefined).finally(() => { saving.current = false; });
  }, [assetId, episodeKey]);

  useEffect(() => {
    let hls: Hls | undefined;
    let cancelled = false;

    async function load() {
      const response = await fetch(`/api/playback/${assetId}`, { method: "POST" });
      const payload = await response.json().catch(() => ({}));
      if (cancelled) return;
      if (!response.ok) {
        if (response.status === 402) setNeedsPlan(true);
        setMessage(payload.message ?? (response.status === 401 ? "Sign in to start playback." : response.status === 429 ? "Too many playback requests. Please wait a moment." : "Playback is temporarily unavailable."));
        return;
      }
      const el = video.current;
      if (!el) return;
      const source: string = payload.manifestUrl;
      const start = () => { if (startAt > 0) el.currentTime = startAt; };
      if (el.canPlayType("application/vnd.apple.mpegurl")) {
        el.src = source;
        el.addEventListener("loadedmetadata", start, { once: true });
      } else if (Hls.isSupported()) {
        hls = new Hls({ startPosition: startAt > 0 ? startAt : -1 });
        hls.loadSource(source);
        hls.attachMedia(el);
        hls.on(Hls.Events.ERROR, (_event, data) => { if (data.fatal) setMessage("Playback failed. Please try again."); });
      } else {
        setMessage("HLS playback is not supported by this browser.");
        return;
      }
      setMessage("");
      el.play().catch(() => undefined); // autoplay may be blocked until the user interacts
    }

    load().catch(() => { if (!cancelled) setMessage("Playback is temporarily unavailable."); });
    const flush = () => save(true);
    window.addEventListener("pagehide", flush);
    return () => {
      cancelled = true;
      window.removeEventListener("pagehide", flush);
      save(true);
      hls?.destroy();
    };
  }, [assetId, startAt, save]);

  // Keyboard shortcuts: space/k play-pause, ←/→ seek 10s, f fullscreen, Esc back.
  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      const el = video.current;
      if (!el) return;
      if (event.key === " " || event.key === "k") { event.preventDefault(); if (el.paused) void el.play(); else el.pause(); }
      else if (event.key === "ArrowRight") el.currentTime = Math.min(el.duration || Infinity, el.currentTime + 10);
      else if (event.key === "ArrowLeft") el.currentTime = Math.max(0, el.currentTime - 10);
      else if (event.key === "f") { if (document.fullscreenElement) void document.exitFullscreen(); else void el.parentElement?.requestFullscreen(); }
      else if (event.key === "Escape" && !document.fullscreenElement) router.push(backHref);
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [backHref, router]);

  // Auto-advance to the next episode when the countdown reaches zero.
  useEffect(() => {
    if (countdown === null || !nextHref) return;
    if (countdown <= 0) { router.push(nextHref); return; }
    const timer = window.setTimeout(() => setCountdown((value) => (value === null ? null : value - 1)), 1000);
    return () => window.clearTimeout(timer);
  }, [countdown, nextHref, router]);

  function revealChrome() {
    setChromeVisible(true);
    window.clearTimeout(hideTimer.current);
    hideTimer.current = window.setTimeout(() => setChromeVisible(false), 3000);
  }

  function onTimeUpdate() {
    const el = video.current;
    save();
    if (el && nextHref && Number.isFinite(el.duration)) setShowNext(el.duration - el.currentTime < 30);
  }

  return <div className="group relative h-full w-full bg-black" onMouseMove={revealChrome}>
    <video ref={video} poster={poster} controls playsInline className="h-full w-full"
      onTimeUpdate={onTimeUpdate} onPause={() => save(true)}
      onEnded={() => { save(true); if (nextHref) setCountdown(NEXT_EPISODE_COUNTDOWN); }}
      onError={() => setMessage("Playback failed. Please try again.")} />

    <div className={`pointer-events-none absolute inset-x-0 top-0 flex items-start gap-4 bg-gradient-to-b from-black/80 to-transparent p-5 transition-opacity duration-500 md:p-8 ${chromeVisible ? "opacity-100" : "opacity-0"}`}>
      <Link href={backHref} aria-label="Back" className="pointer-events-auto rounded-full p-2 transition hover:bg-white/10"><ArrowLeft className="size-8" /></Link>
      <div>
        <p className="text-xl font-bold md:text-2xl">{heading}</p>
        {subheading && <p className="text-sm text-white/70 md:text-base">{subheading}</p>}
      </div>
    </div>

    {(showNext || countdown !== null) && nextHref && <div className="absolute bottom-24 right-6 flex flex-col items-end gap-2 md:right-10">
      {nextLabel && <p className="text-sm text-white/80">{nextLabel}</p>}
      <Link href={nextHref} className="flex items-center gap-2 rounded bg-white px-5 py-3 font-bold text-black transition hover:bg-white/85">
        <SkipForward className="size-5 fill-current" />Next Episode{countdown !== null ? ` in ${countdown}` : ""}
      </Link>
    </div>}

    {message && <div className="absolute inset-0 grid place-items-center bg-black/85 px-5 text-center">
      <div>
        <p className="text-lg text-white/85">{message}</p>
        {needsPlan && <Link href="/account#plans" className="mt-5 inline-block rounded bg-primary px-6 py-2.5 font-semibold">Choose a plan</Link>}
        <Link href={backHref} className="mt-4 block text-sm underline">Back</Link>
      </div>
    </div>}
  </div>;
}

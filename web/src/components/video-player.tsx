"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import Hls from "hls.js";
import { ArrowLeft, SkipForward } from "lucide-react";

const SAVE_EVERY_SECONDS = 10;
const NEXT_EPISODE_COUNTDOWN = 8;
/** Recovery budget before the viewer sees an error (hls.js "fatal" errors are usually recoverable). */
const MAX_NETWORK_RETRIES = 3;
const MAX_MEDIA_RECOVERIES = 2;
const MAX_AUTO_RESTARTS = 1;

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
  const [notice, setNotice] = useState("");
  const [failed, setFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const resumeAt = useRef(startAt);
  const autoRestarts = useRef(0);
  const usingHls = useRef(false);

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
    const timers: number[] = [];
    usingHls.current = false;
    // A retry ("Try again" or an automatic restart) resumes where playback stopped, not where it began.
    const from = attempt > 0 ? resumeAt.current : startAt;

    function fail() {
      if (cancelled) return;
      const el = video.current;
      if (el && el.currentTime > 0) resumeAt.current = el.currentTime;
      hls?.destroy();
      hls = undefined;
      setNotice("");
      setFailed(true);
      setMessage("Playback failed. Please try again.");
    }

    /** The signed link is single-session and expires; ask for a fresh one once without bothering the viewer. */
    function restartWithFreshLink() {
      if (autoRestarts.current >= MAX_AUTO_RESTARTS) { fail(); return; }
      autoRestarts.current += 1;
      const el = video.current;
      if (el && el.currentTime > 0) resumeAt.current = el.currentTime;
      setNotice("Playback link expired. Restarting…");
      setAttempt((value) => value + 1);
    }

    async function load() {
      const response = await fetch(`/api/playback/${assetId}`, { method: "POST" });
      const payload = await response.json().catch(() => ({}));
      if (cancelled) return;
      if (!response.ok) {
        if (response.status === 402) setNeedsPlan(true);
        if (response.status >= 500) setFailed(true);
        setMessage(payload.message ?? (response.status === 401 ? "Sign in to start playback." : response.status === 429 ? "Too many playback requests. Please wait a moment." : "Playback is temporarily unavailable."));
        return;
      }
      const el = video.current;
      if (!el) return;
      const source: string = payload.manifestUrl;
      const seek = () => { if (from > 0) el.currentTime = from; };
      if (Hls.isSupported()) {
        usingHls.current = true;
        let networkRetries = 0;
        let mediaRecoveries = 0;
        const player = new Hls({ startPosition: from > 0 ? from : -1 });
        hls = player;
        player.loadSource(source);
        player.attachMedia(el);
        // Healthy again: forget earlier failures and hide the "Reconnecting…" notice.
        player.on(Hls.Events.FRAG_BUFFERED, () => { networkRetries = 0; mediaRecoveries = 0; setNotice(""); });
        player.on(Hls.Events.ERROR, (_event, data) => {
          if (!data.fatal || cancelled) return;
          if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
            const status = data.response?.code ?? 0;
            if (data.details === Hls.ErrorDetails.MANIFEST_LOAD_ERROR && status >= 400 && status < 500) {
              restartWithFreshLink();
              return;
            }
            if (networkRetries < MAX_NETWORK_RETRIES) {
              networkRetries += 1;
              setNotice("Reconnecting…");
              const manifestFailed = data.details === Hls.ErrorDetails.MANIFEST_LOAD_ERROR || data.details === Hls.ErrorDetails.MANIFEST_LOAD_TIMEOUT;
              timers.push(window.setTimeout(() => {
                if (cancelled || hls !== player) return;
                if (manifestFailed) player.loadSource(source); else player.startLoad();
              }, 1000 * networkRetries));
              return;
            }
          } else if (data.type === Hls.ErrorTypes.MEDIA_ERROR && mediaRecoveries < MAX_MEDIA_RECOVERIES) {
            mediaRecoveries += 1;
            player.recoverMediaError();
            return;
          }
          fail();
        });
      } else if (el.canPlayType("application/vnd.apple.mpegurl")) {
        el.src = source; // Safari/iOS play HLS natively
        el.addEventListener("loadedmetadata", seek, { once: true });
      } else {
        setMessage("HLS playback is not supported by this browser.");
        return;
      }
      setMessage("");
      setFailed(false);
      el.play().catch(() => undefined); // autoplay may be blocked until the user interacts
    }

    load().catch(() => { if (!cancelled) { setFailed(true); setMessage("Playback is temporarily unavailable."); } });
    const flush = () => save(true);
    window.addEventListener("pagehide", flush);
    return () => {
      cancelled = true;
      timers.forEach((timer) => window.clearTimeout(timer));
      window.removeEventListener("pagehide", flush);
      save(true);
      hls?.destroy();
    };
  }, [assetId, startAt, save, attempt]);

  function retry() {
    autoRestarts.current = 0;
    setFailed(false);
    setNotice("");
    setMessage("Preparing playback…");
    setAttempt((value) => value + 1);
  }

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
      onPlaying={() => setNotice("")}
      onError={() => {
        // With hls.js, media errors arrive (and are recovered) through its ERROR event instead.
        if (usingHls.current) return;
        const el = video.current;
        if (el && el.currentTime > 0) resumeAt.current = el.currentTime;
        setFailed(true);
        setMessage("Playback failed. Please try again.");
      }} />

    {notice && !message && <div role="status" className="pointer-events-none absolute inset-x-0 bottom-24 flex justify-center">
      <p className="rounded-full bg-black/75 px-4 py-2 text-sm text-white/90">{notice}</p>
    </div>}

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
        {failed && <button type="button" onClick={retry} className="mt-5 inline-block rounded bg-white px-6 py-2.5 font-semibold text-black transition hover:bg-white/85">Try again</button>}
        <Link href={backHref} className="mt-4 block text-sm underline">Back</Link>
      </div>
    </div>}
  </div>;
}

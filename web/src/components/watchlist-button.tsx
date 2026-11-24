"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Check, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";

export function WatchlistButton({ contentId, initialSaved, signedIn, compact = false }: { contentId: string; initialSaved: boolean; signedIn: boolean; compact?: boolean }) {
  const router = useRouter();
  const [saved, setSaved] = useState(initialSaved);
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState("");

  async function toggle() {
    if (!signedIn) return router.push("/sign-in");
    const next = !saved;
    setSaved(next); // optimistic, like Netflix
    setPending(true);
    setMessage("");
    const response = await fetch(`/api/backend/library/watchlist/${contentId}`, { method: next ? "PUT" : "DELETE" }).catch(() => null);
    if (!response?.ok) {
      setSaved(!next);
      setMessage(response?.status === 401 ? "Your session expired. Please sign in again." : "Could not update My List.");
    } else {
      router.refresh();
    }
    setPending(false);
  }

  const label = saved ? "Remove from My List" : "Add to My List";
  return <div className="relative">
    <Button variant="outline" onClick={toggle} disabled={pending} aria-pressed={saved} aria-label={label} title={label}
      className={compact ? "size-11 rounded-full border-2 border-white/60 bg-black/40 p-0" : "border-white/40 bg-white/10 px-6 text-base backdrop-blur hover:bg-white/20"}>
      {saved ? <Check className="size-5" /> : <Plus className="size-5" />}
      {!compact && "My List"}
    </Button>
    {message && <p role="status" className="absolute left-0 top-full mt-2 w-56 text-xs text-amber-200">{message}</p>}
  </div>;
}

"use client";

import { useState } from "react";
import { Check, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";

export function WatchlistButton({ contentId }: { contentId: string }) {
  const [saved, setSaved] = useState(false);
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState("");

  async function toggle() {
    const signedIn = document.cookie.split("; ").some((cookie) => cookie === "movento_signed_in=1");
    if (!signedIn) {
      setMessage("Sign in to save titles to My List.");
      return;
    }
    setPending(true);
    setMessage("");
    const response = await fetch(`/api/backend/library/watchlist/${contentId}`, { method: saved ? "DELETE" : "PUT" });
    if (response.ok) setSaved(!saved);
    else setMessage(response.status === 401 ? "Your session expired. Please sign in again." : "Could not update My List.");
    setPending(false);
  }

  return <div className="space-y-2">
    <Button variant="outline" onClick={toggle} disabled={pending}>{saved ? <Check className="size-4" /> : <Plus className="size-4" />}{saved ? "In My List" : "My List"}</Button>
    {message && <p className="max-w-56 text-xs text-amber-200">{message}</p>}
  </div>;
}

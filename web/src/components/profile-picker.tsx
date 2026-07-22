"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { ViewerProfile } from "@/lib/types";

export function ProfilePicker({ profiles }: { profiles: ViewerProfile[] }) {
  const router = useRouter();
  const [error, setError] = useState("");
  const [pending, setPending] = useState("");

  async function select(id: string) {
    setPending(id);
    setError("");
    const response = await fetch("/api/profiles/select", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ profileId: id }),
    });
    if (!response.ok) {
      const data = await response.json().catch(() => ({}));
      setError(data.message ?? "Could not select this profile. Please sign in again.");
      setPending("");
      return;
    }
    router.push("/browse");
    router.refresh();
  }

  return <div className="space-y-6">
    {error && <p role="alert" className="mx-auto max-w-lg rounded-md border border-red-500/40 bg-red-500/10 p-3 text-center text-sm text-red-100">{error}</p>}
    <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-5">{profiles.map((profile) => <button key={profile.id} onClick={() => select(profile.id)} disabled={Boolean(pending)} className="group text-center disabled:opacity-60">
      <div className="grid aspect-square place-items-center rounded-xl border-2 border-transparent bg-gradient-to-br from-red-700 to-zinc-900 text-4xl font-black transition group-hover:border-white">{profile.name.slice(0, 1).toUpperCase()}</div>
      <p className="mt-3 text-muted-foreground group-hover:text-white">{pending === profile.id ? "Loading..." : profile.name}</p>
    </button>)}</div>
  </div>;
}

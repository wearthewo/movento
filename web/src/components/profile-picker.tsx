"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ViewerProfile } from "@/lib/types";
import { ProfileAvatar } from "./profile-avatar";

const MAX_PROFILES = 5;

export function ProfilePicker({ profiles, next, initialManage }: { profiles: ViewerProfile[]; next: string; initialManage: boolean }) {
  const router = useRouter();
  const [error, setError] = useState("");
  const [pending, setPending] = useState("");
  const [manage, setManage] = useState(initialManage);
  const [adding, setAdding] = useState(false);

  async function select(id: string) {
    setPending(id);
    setError("");
    const response = await fetch("/api/profiles/select", {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ profileId: id }),
    });
    if (!response.ok) {
      const data = await response.json().catch(() => ({}));
      setError(data.message ?? "Could not select this profile. Please sign in again.");
      setPending("");
      return;
    }
    router.push(next);
    router.refresh();
  }

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const kidsMode = form.get("kidsMode") === "on";
    const response = await fetch("/api/backend/users/me/profiles", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: String(form.get("name") ?? "").trim(), kidsMode, maturityLevel: kidsMode ? "KIDS" : "ADULT" }),
    });
    if (!response.ok) {
      const data = await response.json().catch(() => ({}));
      return setError(data.message ?? "Could not create the profile.");
    }
    setAdding(false);
    setError("");
    router.refresh();
  }

  async function remove(profile: ViewerProfile) {
    if (!window.confirm(`Delete the profile "${profile.name}"? Its My List and viewing history will be lost.`)) return;
    const response = await fetch(`/api/backend/users/me/profiles/${profile.id}`, { method: "DELETE" });
    if (!response.ok) {
      const data = await response.json().catch(() => ({}));
      return setError(data.message ?? "Could not delete the profile.");
    }
    router.refresh();
  }

  return <div className="w-full max-w-4xl space-y-10">
    {error && <p role="alert" className="mx-auto max-w-lg rounded-md border border-red-500/40 bg-red-500/10 p-3 text-center text-sm text-red-100">{error}</p>}
    <ul className="flex flex-wrap justify-center gap-6 md:gap-8">
      {profiles.map((profile) => <li key={profile.id} className="w-28 md:w-36">
        <button onClick={() => manage ? undefined : select(profile.id)} disabled={Boolean(pending)} className="group w-full text-center disabled:opacity-60" aria-label={manage ? `${profile.name} profile` : `Watch as ${profile.name}`}>
          <span className="relative block">
            <ProfileAvatar name={profile.name} kids={profile.kidsMode} className="aspect-square w-full rounded-md text-5xl ring-white transition group-hover:ring-4 md:text-6xl" />
            {manage && <span className="absolute inset-0 grid place-items-center rounded-md bg-black/55"><Pencil className="size-8" /></span>}
          </span>
          <span className="mt-3 block truncate text-white/60 transition group-hover:text-white">{pending === profile.id ? "Loading…" : profile.name}</span>
          {profile.kidsMode && <span className="mt-1 inline-block rounded bg-sky-500 px-1.5 text-[10px] font-black uppercase">Kids</span>}
        </button>
        {manage && profiles.length > 1 && <button onClick={() => remove(profile)} className="mx-auto mt-2 flex items-center gap-1 text-xs text-white/50 hover:text-red-300">
          <Trash2 className="size-3" />Delete
        </button>}
      </li>)}
      {profiles.length < MAX_PROFILES && <li className="w-28 md:w-36">
        <button onClick={() => setAdding(true)} className="group w-full text-center" aria-label="Add profile">
          <span className="grid aspect-square w-full place-items-center rounded-md border-2 border-transparent text-white/50 transition group-hover:bg-white/10 group-hover:text-white">
            <Plus className="size-16" />
          </span>
          <span className="mt-3 block text-white/60 group-hover:text-white">Add Profile</span>
        </button>
      </li>}
    </ul>

    {adding && <form onSubmit={create} className="mx-auto max-w-md space-y-4 rounded-lg border border-white/10 bg-card p-6">
      <h2 className="text-2xl font-semibold">Add a profile</h2>
      <label className="block space-y-2 text-sm"><span>Name</span>
        <input name="name" required maxLength={40} autoFocus className="h-11 w-full rounded border bg-black/40 px-3 outline-none focus:border-white" />
      </label>
      <label className="flex items-center gap-3 text-sm"><input type="checkbox" name="kidsMode" className="size-5 accent-sky-500" />
        <span>Kids profile <span className="text-white/50">— only titles rated for children</span></span>
      </label>
      <div className="flex gap-3"><Button type="submit">Save</Button><Button type="button" variant="outline" onClick={() => setAdding(false)}>Cancel</Button></div>
    </form>}

    <div className="text-center">
      <button onClick={() => setManage((value) => !value)} className="border border-white/50 px-6 py-2 tracking-[.2em] text-white/60 uppercase transition hover:border-white hover:text-white">
        {manage ? "Done" : "Manage Profiles"}
      </button>
    </div>
  </div>;
}

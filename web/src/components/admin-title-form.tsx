"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";

export function AdminTitleForm() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const fields = new FormData(event.currentTarget);
    const response = await fetch("/api/backend/admin/catalog", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        title: fields.get("title"), type: fields.get("type"), synopsis: fields.get("synopsis"),
        releaseYear: Number(fields.get("releaseYear")), maturityRating: fields.get("maturityRating"),
        artworkUrl: fields.get("artworkUrl"), backdropUrl: fields.get("backdropUrl"), featured: false, trending: false,
      }),
    });
    if (!response.ok) return setError("Unable to create title. Confirm this account has the ADMIN role.");
    setOpen(false); router.refresh();
  }

  if (!open) return <Button onClick={() => setOpen(true)}>Add title</Button>;
  return <div role="dialog" aria-modal="true" aria-labelledby="add-title-heading" className="fixed inset-0 z-[60] grid place-items-center bg-black/80 p-4">
    <form onSubmit={submit} className="w-full max-w-xl space-y-4 rounded-xl border bg-card p-6">
      <h2 id="add-title-heading" className="text-xl font-semibold">Add catalog title</h2>
      <div className="grid grid-cols-2 gap-3"><Input name="title" placeholder="Title"/><select name="type" className="h-11 rounded-md border bg-background px-3"><option value="MOVIE">Movie</option><option value="SERIES">Series</option></select><Input name="releaseYear" type="number" placeholder="Release year"/><Input name="maturityRating" placeholder="Maturity rating"/></div>
      <textarea required name="synopsis" placeholder="Synopsis" className="min-h-24 w-full rounded-md border bg-background p-3"/>
      <Input name="artworkUrl" placeholder="Poster URL"/><Input name="backdropUrl" placeholder="Backdrop URL"/>
      {error && <p role="alert" className="text-sm text-red-300">{error}</p>}
      <div className="flex justify-end gap-3"><Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Button><Button type="submit">Save draft</Button></div>
    </form>
  </div>;
}

function Input(props: React.InputHTMLAttributes<HTMLInputElement>) { return <input required {...props} className="h-11 w-full rounded-md border bg-background px-3"/>; }

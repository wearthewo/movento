import Link from "next/link";
import { CreditCard, LockKeyhole, Play, Search } from "lucide-react";

const guestItems = ["Browse catalog", "Search titles", "Open movie/series details"];
const memberItems = ["Create profiles", "Save to My List", "Play/resume videos", "Open billing"];

export function AccessGuide() {
  return <section className="mx-5 mb-10 grid gap-4 rounded-2xl border border-white/10 bg-white/[.035] p-5 shadow-2xl shadow-black/20 md:mx-12 md:grid-cols-[1fr_1fr_auto] md:p-6">
    <div>
      <p className="flex items-center gap-2 text-sm font-semibold uppercase tracking-[.2em] text-primary"><Search className="size-4" />Guest access</p>
      <p className="mt-3 text-sm leading-6 text-white/65">{guestItems.join(" · ")}</p>
    </div>
    <div>
      <p className="flex items-center gap-2 text-sm font-semibold uppercase tracking-[.2em] text-emerald-300"><LockKeyhole className="size-4" />Signed-in access</p>
      <p className="mt-3 text-sm leading-6 text-white/65">{memberItems.join(" · ")}</p>
    </div>
    <div className="flex flex-wrap items-center gap-3 md:justify-end">
      <Link href="/account" className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/10 px-4 py-2 text-sm font-semibold transition hover:bg-white/15"><CreditCard className="size-4" />Billing</Link>
      <Link href="/sign-in" className="inline-flex items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110"><Play className="size-4 fill-current" />Sign in to watch</Link>
    </div>
  </section>;
}

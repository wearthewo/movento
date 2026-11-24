import Image from "next/image";
import Link from "next/link";
import { Play } from "lucide-react";
import type { Title } from "@/lib/types";

/**
 * Landscape box-art card. On hover it lifts and reveals quick info, like the Netflix row tiles.
 * `href` lets Continue Watching jump straight into the player.
 */
export function TitleCard({ item, href, progressPercent, subtitle }: { item: Title; href?: string; progressPercent?: number; subtitle?: string }) {
  const percent = progressPercent ?? item.progressPercent;
  return (
    <Link href={href ?? `/title/${item.slug}`} className="group/card block min-w-0 rounded-md outline-none focus-visible:ring-2 focus-visible:ring-white">
      <div className="relative aspect-video overflow-hidden rounded-md bg-muted shadow-lg shadow-black/40 transition duration-300 group-hover/card:z-10 group-hover/card:scale-[1.06] group-hover/card:shadow-2xl group-focus-visible/card:scale-[1.06]">
        <Image src={item.backdropUrl || item.artworkUrl} alt={item.title} fill sizes="(max-width: 640px) 70vw, (max-width: 1024px) 33vw, 20vw" className="object-cover" />
        <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/10 to-transparent" />
        <p className="absolute bottom-2 left-3 right-3 line-clamp-1 text-sm font-bold drop-shadow md:text-base">{item.title}</p>
        <div className="absolute inset-0 flex items-center justify-center opacity-0 transition group-hover/card:opacity-100 group-focus-visible/card:opacity-100">
          <span className="grid size-11 place-items-center rounded-full border-2 border-white bg-black/50 backdrop-blur"><Play className="size-5 fill-white" /></span>
        </div>
        {item.type === "SERIES" && <span className="absolute left-2 top-2 rounded-sm bg-primary px-1.5 py-0.5 text-[10px] font-black uppercase tracking-wider">Series</span>}
        {percent !== undefined && percent > 0 && (
          <div className="absolute inset-x-0 bottom-0 h-1 bg-white/25" aria-label={`${percent}% watched`}>
            <div className="h-full bg-primary" style={{ width: `${Math.min(100, percent)}%` }} />
          </div>
        )}
      </div>
      <div className="mt-2 flex items-center gap-2 text-xs text-white/60">
        <span className="rounded-sm border border-white/30 px-1 text-[10px] text-white/75">{item.maturityRating}</span>
        <span>{subtitle ?? `${item.releaseYear} · ${item.genres.slice(0, 2).join(" · ")}`}</span>
      </div>
    </Link>
  );
}

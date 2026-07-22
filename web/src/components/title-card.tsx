import Image from "next/image";
import Link from "next/link";
import { Play } from "lucide-react";
import type { Title } from "@/lib/types";

export function TitleCard({ item }: { item: Title }) {
  return (
    <Link href={`/title/${item.slug}`} className="group block min-w-0 rounded-md outline-none focus-visible:ring-2 focus-visible:ring-primary">
      <div className="relative aspect-[2/3] overflow-hidden rounded-md bg-muted shadow-lg shadow-black/30">
        <Image src={item.artworkUrl} alt={item.title} fill sizes="(max-width: 768px) 45vw, 18vw" className="object-cover transition duration-500 group-hover:scale-105" />
        <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-transparent opacity-0 transition group-hover:opacity-100 group-focus-visible:opacity-100" />
        <div className="absolute bottom-3 left-3 right-3 translate-y-3 opacity-0 transition group-hover:translate-y-0 group-hover:opacity-100 group-focus-visible:translate-y-0 group-focus-visible:opacity-100">
          <span className="inline-flex size-9 items-center justify-center rounded-full bg-white text-black">
            <Play className="size-4 fill-current" />
          </span>
          <p className="mt-3 line-clamp-2 text-sm font-semibold text-white">{item.title}</p>
          <p className="mt-1 line-clamp-1 text-xs text-white/65">{item.genres.slice(0, 2).join(" / ")}</p>
        </div>
      </div>
      <p className="mt-2 truncate text-sm font-medium">{item.title}</p>
      <p className="text-xs text-muted-foreground">{item.releaseYear} · {item.maturityRating}</p>
    </Link>
  );
}

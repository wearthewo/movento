import { cn } from "@/lib/utils";

const PALETTE = ["from-red-600 to-red-900", "from-sky-500 to-blue-800", "from-amber-400 to-orange-700", "from-emerald-500 to-teal-800", "from-fuchsia-500 to-purple-800"];

/** Deterministic colour per name so a profile keeps its avatar everywhere. */
export function ProfileAvatar({ name, kids, className }: { name: string; kids?: boolean; className?: string }) {
  const index = [...name].reduce((sum, char) => sum + char.charCodeAt(0), 0) % PALETTE.length;
  return <span aria-hidden className={cn("grid place-items-center bg-gradient-to-br font-black text-white", kids ? "from-sky-400 to-indigo-600" : PALETTE[index], className)}>
    {kids ? "K" : name.slice(0, 1).toUpperCase()}
  </span>;
}

import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";
const variants=cva("inline-flex items-center justify-center gap-2 rounded-md text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:pointer-events-none disabled:opacity-50",{variants:{variant:{default:"bg-primary px-5 py-2.5 text-primary-foreground hover:brightness-110",secondary:"bg-white/90 px-5 py-2.5 text-black hover:bg-white",ghost:"px-3 py-2 text-foreground hover:bg-white/10",outline:"border border-border px-5 py-2.5 hover:bg-white/10"}},defaultVariants:{variant:"default"}});
export function Button({className,variant,asChild=false,...props}:React.ButtonHTMLAttributes<HTMLButtonElement>&VariantProps<typeof variants>&{asChild?:boolean}){const Comp=asChild?Slot:"button";return <Comp className={cn(variants({variant}),className)} {...props}/>}

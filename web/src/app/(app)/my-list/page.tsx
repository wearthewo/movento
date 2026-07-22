import { cookies } from "next/headers";
import { TitleCard } from "@/components/title-card";
import { SignInRequired } from "@/components/sign-in-required";
import { getTitles } from "@/lib/api";

export default async function MyListPage() {
  if (!(await cookies()).get("movento_access")?.value) return <SignInRequired feature="My List" />;
  const titles = (await getTitles()).slice(0, 3);
  return <main className="min-h-screen px-5 pb-20 pt-28 md:px-12"><h1 className="text-3xl font-bold">My List</h1><p className="mt-2 text-muted-foreground">Your saved films and series.</p><div className="mt-8 grid grid-cols-2 gap-5 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">{titles.map((item) => <TitleCard key={item.id} item={item}/>)}</div></main>;
}

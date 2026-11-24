import Link from "next/link";
import { SignInRequired } from "@/components/sign-in-required";
import { TitleCard } from "@/components/title-card";
import { getMyList, isSignedIn } from "@/lib/api";

export const metadata = { title: "My List" };

export default async function MyListPage() {
  if (!(await isSignedIn())) return <SignInRequired feature="My List" />;
  const titles = await getMyList();
  return <main className="min-h-screen px-5 pb-20 pt-28 md:px-12">
    <h1 className="text-3xl font-bold md:text-4xl">My List</h1>
    {titles.length === 0
      ? <div className="mt-16 max-w-md text-white/70">
          <p className="text-lg">You haven&apos;t added any titles to your list yet.</p>
          <p className="mt-2 text-sm">Press <span className="font-semibold text-white">+ My List</span> on any title to save it for later.</p>
          <Link href="/browse" className="mt-6 inline-block rounded bg-white px-5 py-2 font-semibold text-black">Browse titles</Link>
        </div>
      : <div className="mt-8 grid grid-cols-2 gap-x-3 gap-y-8 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
          {titles.map((item) => <TitleCard key={item.id} item={item} />)}
        </div>}
  </main>;
}

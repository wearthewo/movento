import { Suspense } from "react";
import { Billboard } from "@/components/billboard";
import { ContentRail } from "@/components/content-rail";
import { ContinueWatchingRail } from "@/components/continue-watching-rail";
import { getBehaviouralRails, getContinueWatching, getHomeFeed, getMyList, isSignedIn } from "@/lib/api";
import type { HomeFeed } from "@/lib/types";

/**
 * The billboard (the page's Largest Contentful Paint) only needs the cached home feed, so it is sent
 * immediately. Personal rows (Continue Watching, Because you watched, My List, real Top 10) need several
 * more service calls and stream in right after, behind a same-height skeleton.
 */
export default async function Browse() {
  const [feed, signedIn] = await Promise.all([getHomeFeed(), isSignedIn()]);

  return (
    <main className="overflow-x-hidden pb-24">
      <Billboard title={feed.featured} signedIn={signedIn} />
      <div className="relative z-10 -mt-[12vh] space-y-8 md:space-y-10">
        <Suspense fallback={<RowsSkeleton />}>
          <HomeRows feed={feed} />
        </Suspense>
      </div>
    </main>
  );
}

async function HomeRows({ feed }: { feed: HomeFeed }) {
  const [continueWatching, myList, behaviour] = await Promise.all([getContinueWatching(), getMyList(), getBehaviouralRails()]);
  // Real Top 10 (from playback events) replaces the catalog's editorial ranking once there is enough data.
  const rails = feed.rails.map((rail) => rail.kind === "top10" && behaviour.topTen ? { ...rail, items: behaviour.topTen } : rail);
  const [firstRail, ...otherRails] = rails;
  return <>
    {continueWatching.length > 0 && <ContinueWatchingRail items={continueWatching} />}
    {behaviour.becauseYouWatched && <ContentRail rail={behaviour.becauseYouWatched} />}
    {firstRail && <ContentRail rail={firstRail} />}
    {myList.length > 0 && <ContentRail rail={{ id: "my-list", title: "My List", items: myList }} />}
    {otherRails.map((rail) => <ContentRail key={rail.id} rail={rail} />)}
  </>;
}

function RowsSkeleton() {
  return <div aria-hidden className="space-y-8 md:space-y-10">
    {[0, 1, 2].map((row) => <section key={row}>
      <div className="mx-5 mb-3 h-6 w-48 animate-pulse rounded bg-white/10 md:mx-12" />
      <div className="flex gap-2 overflow-hidden px-5 py-3 md:gap-3 md:px-12">
        {[0, 1, 2, 3, 4, 5].map((card) => <div key={card} className="aspect-video w-[66vw] flex-none animate-pulse rounded-md bg-white/10 sm:w-[40vw] md:w-[28vw] lg:w-[21vw] xl:w-[17.5vw]" />)}
      </div>
    </section>)}
  </div>;
}

import { Billboard } from "@/components/billboard";
import { ContentRail } from "@/components/content-rail";
import { ContinueWatchingRail } from "@/components/continue-watching-rail";
import { getBehaviouralRails, getContinueWatching, getHomeFeed, getMyList, isSignedIn } from "@/lib/api";

export default async function Browse() {
  const [feed, signedIn, continueWatching, myList, behaviour] = await Promise.all([
    getHomeFeed(), isSignedIn(), getContinueWatching(), getMyList(), getBehaviouralRails(),
  ]);
  // Real Top 10 (from playback events) replaces the catalog's editorial ranking once there is enough data.
  const rails = feed.rails.map((rail) => rail.kind === "top10" && behaviour.topTen ? { ...rail, items: behaviour.topTen } : rail);
  const [firstRail, ...otherRails] = rails;

  return (
    <main className="overflow-x-hidden pb-24">
      <Billboard title={feed.featured} signedIn={signedIn} />
      <div className="relative z-10 -mt-[12vh] space-y-8 md:space-y-10">
        {continueWatching.length > 0 && <ContinueWatchingRail items={continueWatching} />}
        {behaviour.becauseYouWatched && <ContentRail rail={behaviour.becauseYouWatched} />}
        {firstRail && <ContentRail rail={firstRail} />}
        {myList.length > 0 && <ContentRail rail={{ id: "my-list", title: "My List", items: myList }} />}
        {otherRails.map((rail) => <ContentRail key={rail.id} rail={rail} />)}
      </div>
    </main>
  );
}

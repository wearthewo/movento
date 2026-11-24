import { Billboard } from "@/components/billboard";
import { ContentRail } from "@/components/content-rail";
import { ContinueWatchingRail } from "@/components/continue-watching-rail";
import { getContinueWatching, getHomeFeed, getMyList, isSignedIn } from "@/lib/api";

export default async function Browse() {
  const [feed, signedIn, continueWatching, myList] = await Promise.all([getHomeFeed(), isSignedIn(), getContinueWatching(), getMyList()]);
  const [firstRail, ...otherRails] = feed.rails;

  return (
    <main className="overflow-x-hidden pb-24">
      <Billboard title={feed.featured} signedIn={signedIn} />
      <div className="relative z-10 -mt-[12vh] space-y-8 md:space-y-10">
        {continueWatching.length > 0 && <ContinueWatchingRail items={continueWatching} />}
        {firstRail && <ContentRail rail={firstRail} />}
        {myList.length > 0 && <ContentRail rail={{ id: "my-list", title: "My List", items: myList }} />}
        {otherRails.map((rail) => <ContentRail key={rail.id} rail={rail} />)}
      </div>
    </main>
  );
}

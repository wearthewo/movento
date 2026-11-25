import { cookies } from "next/headers";
import { demoFeed, demoTitles } from "./demo-data";
import { ACCESS_COOKIE, gatewayUrl } from "./session";
import type { ContinueWatchingItem, CurrentUser, HomeFeed, HomeRail, PaymentRecord, Plan, ProgressItem, Season, SubscriptionState, Title, ViewerProfile } from "./types";

type NextFetchInit = RequestInit & { next?: { revalidate?: number } };

async function readJson<T>(response: Response, path: string): Promise<T> {
  if (!response.ok) throw new Error(`Gateway request failed for ${path} (${response.status})`);
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

async function accessToken() {
  return (await cookies()).get(ACCESS_COOKIE)?.value;
}

/** Anonymous catalog read, cached briefly by Next.js. */
export async function publicGatewayFetch<T>(path: string, init?: NextFetchInit): Promise<T> {
  const options: NextFetchInit = { ...init, headers: { "Content-Type": "application/json", ...init?.headers } };
  if (!options.cache && !options.next) options.next = { revalidate: 120 };
  return readJson<T>(await fetch(`${gatewayUrl()}${path}`, options), path);
}

/** Authenticated read with the viewer's access token. Never cached. */
export async function gatewayFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = await accessToken();
  const response = await fetch(`${gatewayUrl()}${path}`, {
    ...init,
    cache: "no-store",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}), ...init?.headers },
  });
  return readJson<T>(response, path);
}

/**
 * Catalog reads go through the viewer's token when signed in so the gateway can apply the
 * profile's maturity level (kids profiles only see kids titles). Guests get the cached public feed.
 */
async function catalogFetch<T>(path: string): Promise<T> {
  return (await accessToken()) ? gatewayFetch<T>(path) : publicGatewayFetch<T>(path);
}

export async function isSignedIn() {
  return Boolean(await accessToken());
}

export async function getHomeFeed(): Promise<HomeFeed> {
  try { return await catalogFetch("/api/v1/catalog/home"); } catch { return demoFeed; }
}

export async function getTitles(type?: "MOVIE" | "SERIES"): Promise<Title[]> {
  try { return await catalogFetch(`/api/v1/catalog/titles${type ? `?type=${type}` : ""}`); }
  catch { return type ? demoTitles.filter((x) => x.type === type) : demoTitles; }
}

export async function searchTitles(query: string): Promise<Title[]> {
  const q = query.trim();
  if (!q) return getTitles();
  try { return await catalogFetch(`/api/v1/catalog/search?q=${encodeURIComponent(q)}`); }
  catch {
    return demoTitles.filter((x) => `${x.title} ${x.synopsis} ${x.type} ${x.releaseYear} ${x.maturityRating} ${x.genres.join(" ")}`.toLowerCase().includes(q.toLowerCase()));
  }
}

export async function getTitle(slug: string): Promise<Title | undefined> {
  try { return await catalogFetch(`/api/v1/catalog/titles/${encodeURIComponent(slug)}`); }
  catch { return demoTitles.find((x) => x.slug === slug); }
}

export async function getTitleById(id: string): Promise<Title | undefined> {
  try { return (await catalogFetch<Title[]>(`/api/v1/catalog/titles?ids=${encodeURIComponent(id)}`))[0]; }
  catch { return demoTitles.find((x) => x.id === id); }
}

export async function getEpisodes(slug: string): Promise<Season[]> {
  try { return await catalogFetch(`/api/v1/catalog/titles/${encodeURIComponent(slug)}/episodes`); } catch { return []; }
}

export async function getSimilar(slug: string): Promise<Title[]> {
  try { return await catalogFetch(`/api/v1/catalog/titles/${encodeURIComponent(slug)}/similar`); }
  catch {
    const base = demoTitles.find((x) => x.slug === slug);
    return base ? demoTitles.filter((x) => x.slug !== slug && x.genres.some((g) => base.genres.includes(g))) : [];
  }
}

export async function getContinueWatching(): Promise<ContinueWatchingItem[]> {
  if (!(await isSignedIn())) return [];
  try { return await gatewayFetch("/api/v1/library/continue-watching"); } catch { return []; }
}

export async function getMyList(): Promise<Title[]> {
  if (!(await isSignedIn())) return [];
  try { return await gatewayFetch("/api/v1/library/watchlist/titles"); } catch { return []; }
}

export async function isInMyList(contentId: string): Promise<boolean> {
  if (!(await isSignedIn())) return false;
  try { return (await gatewayFetch<{ saved: boolean }>(`/api/v1/library/watchlist/${contentId}`)).saved; } catch { return false; }
}

export async function getProgress(contentId: string): Promise<ProgressItem | undefined> {
  if (!(await isSignedIn())) return undefined;
  try { return await gatewayFetch<ProgressItem | undefined>(`/api/v1/library/progress/${contentId}`); } catch { return undefined; }
}

export async function getProfiles(): Promise<ViewerProfile[]> {
  try { return await gatewayFetch("/api/v1/users/me/profiles"); }
  catch { return [{ id: "demo", name: "Movie fan", kidsMode: false, maturityLevel: "ADULT" }]; }
}

export async function getSubscription(): Promise<SubscriptionState> {
  try { return await gatewayFetch("/api/v1/billing/subscription"); }
  catch { return { status: "UNAVAILABLE" }; }
}

export async function getPlans(): Promise<{ plans: Plan[]; testMode: boolean }> {
  try { return await gatewayFetch("/api/v1/billing/plans"); } catch { return { plans: [], testMode: true }; }
}

export async function getPayments(): Promise<PaymentRecord[]> {
  try { return await gatewayFetch("/api/v1/billing/payments"); } catch { return []; }
}

export async function getCurrentUser(): Promise<CurrentUser | undefined> {
  try { return await gatewayFetch("/api/v1/users/me"); } catch { return undefined; }
}

/**
 * Rows driven by viewing events: recommendation-service records every playback start (via RabbitMQ)
 * and tells us what this profile watched last and what is popular. content-service then supplies the
 * titles, ranked by shared genres and filtered for the profile's maturity level.
 */
export async function getBehaviouralRails(): Promise<{ becauseYouWatched?: HomeRail; topTen?: Title[] }> {
  if (!(await isSignedIn())) return {};
  const [signal, popular] = await Promise.all([
    gatewayFetch<{ seedContentId: number | null; watchedContentIds: number[] }>("/api/v1/recommendations/because-you-watched").catch(() => null),
    gatewayFetch<{ contentId: number; plays: number }[]>("/api/v1/recommendations/popular?days=7&limit=10").catch(() => []),
  ]);

  // Both follow-up lookups run in parallel, one round trip each.
  const [becauseYouWatched, topTen] = await Promise.all([
    (async (): Promise<HomeRail | undefined> => {
      if (!signal?.seedContentId) return undefined;
      const exclude = signal.watchedContentIds.slice(0, 50).join(",");
      const result = await catalogFetch<{ seed: Title; items: Title[] }>(
        `/api/v1/catalog/titles/by-id/${signal.seedContentId}/similar${exclude ? `?exclude=${exclude}` : ""}`,
      ).catch(() => null);
      return result && result.items.length >= 2
        ? { id: "because-you-watched", title: `Because you watched ${result.seed.title}`, items: result.items }
        : undefined;
    })(),
    (async (): Promise<Title[] | undefined> => {
      if (popular.length < 3) return undefined;
      const ids = popular.map((entry) => entry.contentId).join(",");
      const titles = await catalogFetch<Title[]>(`/api/v1/catalog/titles?ids=${ids}`).catch(() => []);
      return titles.length >= 3 ? titles.slice(0, 10) : undefined;
    })(),
  ]);
  return { becauseYouWatched, topTen };
}

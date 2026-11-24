export type ContentType = "MOVIE" | "SERIES";

export interface Title {
  id: string;
  slug: string;
  title: string;
  synopsis: string;
  type: ContentType;
  releaseYear: number;
  maturityRating: string;
  runtimeMinutes?: number;
  genres: string[];
  artworkUrl: string;
  backdropUrl: string;
  trailerUrl?: string;
  featured?: boolean;
  trending?: boolean;
  progressPercent?: number;
}

/** "top10" rails render large rank numbers next to each card. */
export type RailKind = "standard" | "top10" | "continue";

export interface HomeRail {
  id: string;
  title: string;
  kind?: RailKind;
  items: Title[];
}

export interface HomeFeed {
  featured: Title;
  rails: HomeRail[];
}

export interface Episode {
  id: string;
  episodeNumber: number;
  title: string;
  synopsis?: string | null;
  runtimeMinutes?: number | null;
  thumbnailUrl?: string | null;
}

export interface Season {
  seasonNumber: number;
  title: string;
  episodes: Episode[];
}

export interface ProgressItem {
  contentId: number;
  episodeKey: string;
  progressSeconds: number;
  durationSeconds: number;
  completed: boolean;
  progressPercent: number;
  updatedAt: string;
}

export interface ContinueWatchingItem {
  title: Title;
  progress: ProgressItem;
}

export interface ViewerProfile {
  id: string;
  name: string;
  avatarUrl?: string;
  kidsMode: boolean;
  maturityLevel: string;
}

export interface Plan {
  id: string;
  name: string;
  monthlyPrice: number;
  currency: string;
  videoQuality: string;
  maxStreams: number;
  features: string[];
}

export interface SubscriptionState {
  planId?: string | null;
  planName?: string | null;
  status: string;
  currentPeriodEnd?: string | null;
  cancelAtPeriodEnd?: boolean;
  active?: boolean;
  maxStreams?: number;
  videoQuality?: string | null;
  testMode?: boolean;
}

export interface PaymentRecord {
  id: string;
  planId: string;
  amount: number;
  currency: string;
  status: string;
  description?: string | null;
  createdAt?: string | null;
}

export interface CurrentUser {
  id: number;
  email: string;
  firstName?: string | null;
  lastName?: string | null;
  emailVerified?: boolean;
}

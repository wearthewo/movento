export type ContentType = "MOVIE" | "SERIES";
export interface Title { id:string; slug:string; title:string; synopsis:string; type:ContentType; releaseYear:number; maturityRating:string; runtimeMinutes?:number; genres:string[]; artworkUrl:string; backdropUrl:string; trailerUrl?:string; featured?:boolean; trending?:boolean; progressPercent?:number }
export interface HomeRail { id:string; title:string; items:Title[] }
export interface HomeFeed { featured:Title; rails:HomeRail[] }
export interface ViewerProfile { id:string; name:string; avatarUrl?:string; kidsMode:boolean; maturityLevel:string }
export interface SubscriptionState { userId?:number; stripeCustomerId?:string|null; stripeSubscriptionId?:string|null; planId?:string|null; status?:string|null; currentPeriodEnd?:string|null; cancelAtPeriodEnd?:boolean }
export interface CurrentUser { id:number; email:string; firstName?:string|null; lastName?:string|null }

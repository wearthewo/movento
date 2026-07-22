import { cookies } from "next/headers";
import { demoFeed, demoTitles } from "./demo-data";
import type { HomeFeed, SubscriptionState, Title, ViewerProfile } from "./types";
const gateway=process.env.GATEWAY_URL??"http://localhost:8080";
async function readJson<T>(response:Response,path:string):Promise<T>{if(!response.ok)throw new Error(`Gateway request failed for ${path} (${response.status})`);return response.json() as Promise<T>}
type NextFetchInit = RequestInit & { next?: { revalidate?: number } };
export async function publicGatewayFetch<T>(path:string,init?:NextFetchInit):Promise<T>{const options:NextFetchInit={...init,headers:{"Content-Type":"application/json",...init?.headers}};if(!options.cache&&!options.next)options.next={revalidate:120};const response=await fetch(`${gateway}${path}`,options);return readJson<T>(response,path)}
export async function gatewayFetch<T>(path:string,init?:RequestInit):Promise<T>{const jar=await cookies();const token=jar.get("movento_access")?.value;const response=await fetch(`${gateway}${path}`,{...init,cache:init?.cache??"no-store",headers:{"Content-Type":"application/json",...(token?{Authorization:`Bearer ${token}`}:{ }),...init?.headers}});return readJson<T>(response,path)}
export async function getHomeFeed():Promise<HomeFeed>{try{return await publicGatewayFetch("/api/v1/catalog/home")}catch{return demoFeed}}
export async function getTitles(type?:"MOVIE"|"SERIES"):Promise<Title[]>{try{return await publicGatewayFetch(`/api/v1/catalog/titles${type?`?type=${type}`:""}`)}catch{return type?demoTitles.filter(x=>x.type===type):demoTitles}}
export async function searchTitles(query:string):Promise<Title[]>{const q=query.trim();if(!q)return getTitles();try{return await publicGatewayFetch(`/api/v1/catalog/search?q=${encodeURIComponent(q)}`,{cache:"no-store"})}catch{return demoTitles.filter(x=>`${x.title} ${x.synopsis} ${x.type} ${x.releaseYear} ${x.maturityRating} ${x.genres.join(" ")}`.toLowerCase().includes(q.toLowerCase()))}}
export async function getTitle(slug:string):Promise<Title|undefined>{try{return await publicGatewayFetch(`/api/v1/catalog/titles/${encodeURIComponent(slug)}`)}catch{return demoTitles.find(x=>x.slug===slug)}}
export async function getProfiles():Promise<ViewerProfile[]>{try{return await gatewayFetch("/api/v1/users/me/profiles")}catch{return [{id:"demo",name:"Movie fan",kidsMode:false,maturityLevel:"ADULT"}]}}
export async function getSubscription():Promise<SubscriptionState>{try{return await gatewayFetch("/api/v1/billing/subscription")}catch{return {status:"UNAVAILABLE",planId:"movento-monthly"}}}

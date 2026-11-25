import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  images: {
    remotePatterns: [{ protocol: "https", hostname: "images.unsplash.com" }, { protocol: "https", hostname: "**.r2.dev" }],
    // Full-width artwork is never served wider than 1920 px. On high-DPI screens the default list goes up to
    // 3840 px, which means a much bigger first-time resize and download for the billboard (the LCP image).
    deviceSizes: [640, 828, 1080, 1280, 1600, 1920],
    // Catalog artwork rarely changes: keep optimized variants for 30 days instead of re-fetching them.
    minimumCacheTTL: 60 * 60 * 24 * 30,
  },
};

export default nextConfig;

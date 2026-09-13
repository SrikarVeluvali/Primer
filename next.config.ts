import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    // Keep Auth.js' own /api/auth/* route inside Next.js. Only the Spring API is proxied.
    return [{ source: "/api/papers/:path*", destination: `${process.env.PRIMER_API_URL ?? "http://localhost:8080"}/api/papers/:path*` }];
  },
};

export default nextConfig;

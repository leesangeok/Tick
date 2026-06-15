import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  async redirects() {
    return [
      {
        source: "/:path*",
        has: [{ type: "host", value: "tick-ivory.vercel.app" }],
        destination: "https://tickk.dev/:path*",
        permanent: true,
      },
    ];
  },
};

export default nextConfig;

import type { NextConfig } from "next";
import path from "node:path";

const nextConfig: NextConfig = {
  // @moni/shared ships TypeScript source, so Next must compile it.
  transpilePackages: ["@moni/shared"],
  turbopack: {
    root: path.join(process.cwd(), ".."),
  },
};

export default nextConfig;

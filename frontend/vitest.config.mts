import path from "node:path";
import { defineConfig } from "vitest/config";

/**
 * Vitest 유닛 테스트 설정.
 * - React/DOM 필요한 훅 테스트는 jsdom 환경.
 * - `@/*` alias 는 tsconfig 와 동일하게 src.
 * - .next / node_modules 는 스캔 제외.
 */
export default defineConfig({
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    include: ["src/**/*.test.ts", "src/**/*.test.tsx"],
    exclude: ["node_modules", ".next", "dist"],
    setupFiles: ["./vitest.setup.ts"],
  },
});

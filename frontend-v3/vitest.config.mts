import angular from "@analogjs/vite-plugin-angular";
import { defineConfig } from "vitest/config";

export default defineConfig(() => ({
  plugins: [angular()],
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: ["src/test-setup.ts"],
    include: ["src/**/*.{spec,test}.ts"],
    reporters: ["default"],
    coverage: {
      provider: "v8",
      reportsDirectory: "coverage",
      reporter: ["text", "html", "lcov"],
      include: ["src/app/**/*.ts"],
      exclude: ["src/**/*.spec.ts", "src/**/*.test.ts", "src/main.ts"],
    },
  },
  define: {
    "import.meta.vitest": "undefined",
  },
}));

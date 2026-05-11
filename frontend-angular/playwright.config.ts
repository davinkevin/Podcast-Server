import { defineConfig, devices } from "@playwright/test";

const PORT = Number(process.env["PLAYWRIGHT_PORT"] ?? 4203);
const BASE_URL = `http://localhost:${PORT}`;

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env["CI"],
  retries: process.env["CI"] ? 2 : 0,
  workers: process.env["CI"] ? 1 : undefined,
  reporter: process.env["CI"] ? [["junit", { outputFile: "playwright-report/results.xml" }], ["list"]] : "list",
  use: {
    baseURL: BASE_URL,
    trace: "on-first-retry",
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
  ],
  webServer: {
    command: `npx ng serve --port=${PORT} --proxy-config proxy.conf.json`,
    url: `${BASE_URL}/v3/`,
    reuseExistingServer: !process.env["CI"],
    timeout: 120_000,
  },
});

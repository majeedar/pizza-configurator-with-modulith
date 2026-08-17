import { defineConfig } from "@playwright/test";

// Runs against an already-started `docker compose up` stack (agent.md §26
// "E2E": start compose test stack / run Flyway / run Playwright tests /
// collect logs on failure / tear down — the compose-up and teardown are the
// CI workflow's job, integration.yml, not this config's).
export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["html", { open: "never" }], ["list"]] : "list",
  use: {
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
});

export const CUSTOMER_WEB_URL = process.env.CUSTOMER_WEB_URL ?? "http://localhost:3000";
export const STAFF_WEB_URL = process.env.STAFF_WEB_URL ?? "http://localhost:3001";
export const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

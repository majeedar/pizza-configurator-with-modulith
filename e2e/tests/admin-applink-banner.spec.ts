import { test, expect, APIRequestContext } from "@playwright/test";
import { CUSTOMER_WEB_URL, BACKEND_URL } from "../playwright.config";

// Cross-app round trip (agent.md §7.10/§8.6, Phase 11): an admin app-link
// change surfaces live on the customer web footer's "Get the app" banner —
// exercised manually during Phase 11, formalized here as a real E2E test.
// Restores the original app-link URL afterwards so this test doesn't
// permanently mutate the shared demo-data row other tests may rely on.

const TEST_URL = "https://example.test/e2e-run/pizza-configurator-customer.apk";

async function adminToken(request: APIRequestContext): Promise<string> {
  const response = await request.post(`${BACKEND_URL}/api/v1/staff/login`, {
    data: { username: "admin", password: "admin123" },
  });
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  return body.token;
}

let originalUrl: string;
let originalActive: boolean;

test.beforeAll(async ({ request }) => {
  const response = await request.get(`${BACKEND_URL}/api/v1/app-links/android/customer`);
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  originalUrl = body.url;
  originalActive = body.active;
});

test.afterAll(async ({ request }) => {
  const token = await adminToken(request);
  await request.put(`${BACKEND_URL}/api/v1/admin/app-links/android/customer`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { url: originalUrl, active: originalActive },
  });
});

test("admin app-link update is reflected live on the customer web banner", async ({ page, request }) => {
  const token = await adminToken(request);

  const putResponse = await request.put(`${BACKEND_URL}/api/v1/admin/app-links/android/customer`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { url: TEST_URL, active: true },
  });
  expect(putResponse.ok()).toBeTruthy();

  await page.goto(CUSTOMER_WEB_URL);
  const appLink = page.getByRole("link", { name: "Get the Android app" });
  await expect(appLink).toBeVisible({ timeout: 10000 });
  await expect(appLink).toHaveAttribute("href", TEST_URL);
  await expect(page.getByAltText("QR code to download the customer Android app")).toBeVisible();
});

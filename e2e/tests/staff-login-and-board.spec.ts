import { test, expect } from "@playwright/test";
import { STAFF_WEB_URL } from "../playwright.config";

// Covers staff auth + KDS landing (agent.md §7.8/§8.2/§14.1) using the demo
// kitchen account seeded by db/dev-migration under the local profile.
test("kitchen staff logs in and sees the production board", async ({ page }) => {
  await page.goto(STAFF_WEB_URL);
  await expect(page.getByText("Staff sign in")).toBeVisible();

  await page.getByLabel("Username").fill("kitchen");
  await page.getByLabel("Password").fill("kitchen123");
  await page.getByRole("button", { name: "Log in" }).click();

  await expect(page.getByText("Production board")).toBeVisible({ timeout: 10000 });
  for (const column of ["Confirmed", "Approved", "In processing", "Ready"]) {
    await expect(page.getByText(column, { exact: true })).toBeVisible();
  }
});

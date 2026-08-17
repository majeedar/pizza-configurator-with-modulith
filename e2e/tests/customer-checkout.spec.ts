import { test, expect } from "@playwright/test";
import { CUSTOMER_WEB_URL } from "../playwright.config";

// Real checkout flow (agent.md §7.7), wired for the first time in this
// phase — mirrors the flow the native customer Android app already had.
test("guest can check out from the basket and see the order status page", async ({ page }) => {
  await page.goto(CUSTOMER_WEB_URL);
  await page.locator(".MuiCardActionArea-root", { hasText: "Margherita" }).click();
  await expect(page.getByRole("heading", { name: "Margherita" })).toBeVisible();

  await page.getByRole("button", { name: "Check availability & price" }).click();
  await expect(page.getByText(/^Total: \d+\.\d{2} \w+$/)).toBeVisible({ timeout: 15000 });
  await page.getByRole("button", { name: "Add to Basket" }).click();

  await expect(page.getByText("Your basket")).toBeVisible();
  await page.getByLabel("Notes for the kitchen (optional)").fill("Ring the bell twice — E2E test");

  await page.getByRole("button", { name: "Confirm & Checkout" }).click();

  await expect(page).toHaveURL(/\/order\/P-\d+/, { timeout: 15000 });
  await expect(page.getByText(/^Order P-\d+$/)).toBeVisible();
  await expect(page.getByText(/Pickup code:/)).toBeVisible();
  await expect(page.getByText("Confirmed — waiting for the kitchen to approve it")).toBeVisible();

  // The basket should be empty again — a fresh one was started after checkout.
  await page.goto(CUSTOMER_WEB_URL + "/basket");
  await expect(page.getByText("Your basket is empty.")).toBeVisible();
});

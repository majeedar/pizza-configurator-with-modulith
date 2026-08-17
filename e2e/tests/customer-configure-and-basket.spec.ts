import { test, expect } from "@playwright/test";
import { CUSTOMER_WEB_URL } from "../playwright.config";

// Covers the core customer flow (agent.md §4.1/§8.1): browse -> configure ->
// validate/price -> add to basket. Guest checkout, no login needed.
test("customer configures a Margherita and adds it to the basket", async ({ page }) => {
  await page.goto(CUSTOMER_WEB_URL);
  await expect(page.getByText("Choose your pizza")).toBeVisible();

  await page.locator(".MuiCardActionArea-root", { hasText: "Margherita" }).click();
  await expect(page.getByRole("heading", { name: "Margherita" })).toBeVisible();

  await page.getByRole("button", { name: "Check availability & price" }).click();
  await expect(page.getByText(/^Total: \d+\.\d{2} \w+$/)).toBeVisible({ timeout: 15000 });

  const addToBasket = page.getByRole("button", { name: "Add to Basket" });
  await expect(addToBasket).toBeEnabled();
  await addToBasket.click();

  await expect(page.getByText("Your basket")).toBeVisible();
  await expect(page.getByText(/Margherita/)).toBeVisible();
});

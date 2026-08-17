import { test, expect } from "@playwright/test";
import { CUSTOMER_WEB_URL } from "../playwright.config";

// Rules-derived customer-facing constraints (rules.web.ExtraConstraintsController,
// backed by the demo MARGHERITA_NO_PINEAPPLE OPTION_ALLOWED rule and the
// pre-existing CHEESE_MAX_2 MAX_QUANTITY rule, V901). Not just a "does the
// rule reject it on submit" check (already covered by backend integration
// tests) — this covers whether the *UI itself* hides/caps the option before
// the customer ever tries to select it.
test("Margherita hides pineapple and caps extra cheese at its rule's max, with unit labels shown", async ({ page }) => {
  await page.goto(CUSTOMER_WEB_URL);
  await page.locator(".MuiCardActionArea-root", { hasText: "Margherita" }).click();
  await expect(page.getByRole("heading", { name: "Margherita" })).toBeVisible();

  // Pineapple isn't offered on a Margherita at all — not merely rejected.
  await expect(page.getByText("Pineapple", { exact: true })).toHaveCount(0);

  // Piece vs portion units are shown next to countable/scoopable extras.
  await expect(page.getByText(/Anchovy\s*\(piece\)/)).toBeVisible();
  await expect(page.getByText(/Extra Cheese\s*\(portion \(100g\), max 2\)/)).toBeVisible();

  // The "+" stepper for Extra Cheese stops incrementing at the rule's max.
  const cheeseRow = page.locator("xpath=//p[contains(., 'Extra Cheese')]/ancestor::div[contains(@class,'MuiStack-root')][1]");
  const plus = cheeseRow.locator("button").nth(1);
  const quantity = cheeseRow.locator("p").nth(1);

  await plus.click();
  await plus.click();
  await expect(quantity).toHaveText("2");
  await expect(plus).toBeDisabled();
});

import { test, expect } from "@playwright/test";
import { CUSTOMER_WEB_URL, STAFF_WEB_URL } from "../playwright.config";

// Agent.md §4.3/§7.11/§13.3: a customer comment the AI adapter can't
// resolve (no AI provider is configured locally — see application.yml's
// blank api-key defaults — so every comment takes this path deterministically)
// escalates to the kitchen review queue; a kitchen decision (here: reject)
// flows back to the customer's page automatically via polling, with no
// manual refresh. First real click-through of the review queue UI added in
// this pass — the backend pipeline itself already had integration coverage,
// but the staff review-queue page and the customer-side polling/response UI
// did not.
test("a comment that needs review reaches the kitchen queue and a reject decision reaches the customer", async ({
  browser,
}) => {
  const customerContext = await browser.newContext();
  const customerPage = await customerContext.newPage();

  await customerPage.goto(CUSTOMER_WEB_URL);
  await customerPage.locator(".MuiCardActionArea-root", { hasText: "Napoli" }).click();
  await expect(customerPage.getByRole("heading", { name: "Napoli" })).toBeVisible();

  await customerPage.getByLabel("Anything else? (optional)").fill("Make it extra swirly, whatever that means");
  await customerPage.getByRole("button", { name: "Check availability & price" }).click();
  await expect(customerPage.getByText(/a team member is taking a quick look/)).toBeVisible({ timeout: 10000 });

  const staffContext = await browser.newContext();
  const staffPage = await staffContext.newPage();
  await staffPage.goto(STAFF_WEB_URL);
  await staffPage.getByLabel("Username").fill("admin");
  await staffPage.getByLabel("Password").fill("admin123");
  await staffPage.getByRole("button", { name: "Log in" }).click();
  await expect(staffPage).toHaveURL(/\/admin\/pizzas$/, { timeout: 10000 });

  await staffPage.getByRole("link", { name: "Reviews" }).click();
  await expect(staffPage.getByText(/Configuration [0-9a-f]{8}/).first()).toBeVisible({ timeout: 10000 });
  const card = staffPage.locator(".MuiCard-root", { hasText: "AI could not fully interpret" }).first();
  await card.getByRole("button", { name: "Review" }).click();
  await expect(staffPage.getByText("extra swirly")).toBeVisible();
  await staffPage.getByRole("button", { name: "Reject" }).click();

  await expect(customerPage.getByText(/couldn't accommodate this request/)).toBeVisible({ timeout: 20000 });

  await customerContext.close();
  await staffContext.close();
});

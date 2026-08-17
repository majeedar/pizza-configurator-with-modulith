import { test, expect } from "@playwright/test";
import { BACKEND_URL, STAFF_WEB_URL } from "../playwright.config";

// Regression test for a real reported bug: navigating from /reviews back to
// /board didn't work, because the old single toggle button's target
// depended on whether the *previous* page happened to be /board (it wasn't,
// when arriving from /reviews) — see AppLayout.tsx's history. Also covers
// the badge-notification feature added alongside the fix: a live count of
// unactioned orders/reviews shown on the nav button for the *other* queue.
test("kitchen staff can navigate both ways between board and reviews, with live badge counts", async ({
  page,
  request,
}) => {
  const pizzas = await (await request.get(`${BACKEND_URL}/api/v1/catalog/pizzas`)).json();
  const pizzaId = pizzas.find((p: { code: string }) => p.code === "MARGHERITA").pizzaId;

  // A comment that can't resolve locally (no AI provider configured) always
  // produces an OPEN review — deterministic without needing a live AI key.
  const config = await (
    await request.post(`${BACKEND_URL}/api/v1/configurations`, {
      data: { pizzaId, sizeCode: "S", doughCode: "CLASSIC", removedIngredients: [], extras: [], comment: "nav badge test" },
    })
  ).json();
  await request.post(`${BACKEND_URL}/api/v1/configurations/${config.configurationId}/validate`);

  await page.goto(STAFF_WEB_URL);
  await page.getByLabel("Username").fill("kitchen");
  await page.getByLabel("Password").fill("kitchen123");
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page).toHaveURL(/\/board$/, { timeout: 10000 });

  // Scoped to the Reviews nav item specifically. Asserts the badge shows up
  // at all (a >0 count) rather than an exact number — exactly one OPEN
  // review is guaranteed by this test's own setup, but a shared backend
  // across repeated local runs can carry over other still-open reviews too;
  // a real CI run always starts from a fresh database.
  const reviewsBadgeRoot = page.locator(".MuiBadge-root", { has: page.getByRole("link", { name: "Reviews" }) });
  await expect(reviewsBadgeRoot.locator(".MuiBadge-badge")).toBeVisible({ timeout: 10000 });

  await page.getByRole("link", { name: "Reviews" }).click();
  await expect(page).toHaveURL(/\/reviews$/);

  // The actual bug: this link previously pointed at /admin (or didn't
  // exist at all for a KITCHEN-role user) instead of /board.
  await page.getByRole("link", { name: /Production board/ }).click();
  await expect(page).toHaveURL(/\/board$/, { timeout: 10000 });
});

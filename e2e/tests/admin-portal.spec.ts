import { test, expect } from "@playwright/test";
import { STAFF_WEB_URL } from "../playwright.config";

// Real click-through of the new Admin Portal (agent.md §8.3): login lands an
// admin on /admin, and each of the CRUD screens actually persists a change
// through the real backend, not a mock.
test("admin can log in, land on the Admin Portal, and manage rules/sizes/staff/audit", async ({ page }) => {
  await page.goto(STAFF_WEB_URL);
  await page.getByLabel("Username").fill("admin");
  await page.getByLabel("Password").fill("admin123");
  await page.getByRole("button", { name: "Log in" }).click();

  await expect(page.getByRole("heading", { name: "Pizzas" })).toBeVisible({ timeout: 10000 });
  await expect(page).toHaveURL(/\/admin\/pizzas$/);

  // Sizes: create a new size, see it in the table.
  await page.getByRole("tab", { name: "Sizes" }).click();
  await page.getByRole("button", { name: "New size" }).click();
  const sizeCode = `E2E${Date.now()}`;
  await page.getByLabel("Code").fill(sizeCode);
  await page.getByLabel("Display name").fill("E2E Test Size");
  await page.getByLabel("Price modifier").fill("1.50");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByText(sizeCode)).toBeVisible({ timeout: 10000 });

  // Rules: create a MAX_QUANTITY rule targeting a code no real order uses.
  await page.getByRole("tab", { name: "Rules" }).click();
  await page.getByRole("button", { name: "New rule" }).click();
  const ruleCode = `E2E_RULE_${Date.now()}`;
  await page.getByLabel("Rule code").fill(ruleCode);
  await page.getByLabel("Ingredient code").fill("E2E_TEST_INGREDIENT_XYZ");
  await page.getByLabel("Max quantity").fill("3");
  await page.getByLabel("Violation message").fill("E2E test rule — safe to ignore.");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByText(ruleCode)).toBeVisible({ timeout: 10000 });

  // Audit log: the rule creation above should show up.
  await page.getByRole("tab", { name: "Audit Log" }).click();
  await expect(page.getByText("RULE_CREATED").first()).toBeVisible({ timeout: 10000 });

  // Staff: create a new kitchen account, see it enabled.
  await page.getByRole("tab", { name: "Staff" }).click();
  await page.getByRole("button", { name: "New staff account" }).click();
  const username = `e2e_kitchen_${Date.now()}`;
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Display name").fill("E2E Kitchen Tester");
  await page.getByLabel("Password").fill("e2eTestPassword123");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByText(username)).toBeVisible({ timeout: 10000 });
});

// This test caught two real bugs during development: a LazyInitializationException
// on GET .../recipe (spring.jpa.open-in-view: false + an un-fetched lazy
// association read outside the transaction — fixed with a JOIN FETCH query in
// PizzaIngredientRepository), and a raw 500 instead of a 409 when the recipe
// dropdown let an admin pick an ingredient already on the recipe (fixed with a
// DataIntegrityViolationException handler in CatalogExceptionHandler *and* by
// filtering the dropdown so the conflict can't be reached from the UI at all).
test("admin can add, edit, and remove a pizza's recipe lines", async ({ page }) => {
  await page.goto(STAFF_WEB_URL);
  await page.getByLabel("Username").fill("admin");
  await page.getByLabel("Password").fill("admin123");
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page.getByRole("heading", { name: "Pizzas" })).toBeVisible({ timeout: 10000 });

  await page.getByRole("button", { name: "Recipe" }).first().click();
  await expect(page.getByRole("button", { name: "Add ingredient" })).toBeVisible({ timeout: 10000 });
  await expect(page.getByText("Request failed")).not.toBeVisible();

  await page.getByRole("button", { name: "Add ingredient" }).click();
  await page.getByRole("combobox", { name: "Ingredient" }).click();
  await page.getByRole("option").first().click();
  await page.getByLabel("Default quantity").fill("2");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByRole("dialog")).not.toBeVisible({ timeout: 10000 });

  await page.getByRole("button", { name: "Edit" }).first().click();
  await page.getByLabel("Default quantity").fill("5");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByRole("dialog")).not.toBeVisible({ timeout: 10000 });
  await expect(page.getByRole("cell", { name: "5", exact: true })).toBeVisible({ timeout: 10000 });

  page.on("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Remove" }).first().click();
  await expect(page.getByText("Request failed")).not.toBeVisible();
});

// MIN_QUANTITY and OPTION_ALLOWED were the two RuleTypes with no evaluator
// implementation (and so no structured form here, only a raw-JSON fallback)
// until this phase — this exercises the structured editor for both, end to
// end against the real backend evaluator that now backs each one.
test("admin can create MIN_QUANTITY and OPTION_ALLOWED rules with the structured editor", async ({ page }) => {
  await page.goto(STAFF_WEB_URL);
  await page.getByLabel("Username").fill("admin");
  await page.getByLabel("Password").fill("admin123");
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page.getByRole("heading", { name: "Pizzas" })).toBeVisible({ timeout: 10000 });

  await page.getByRole("tab", { name: "Rules" }).click();

  await page.getByRole("button", { name: "New rule" }).click();
  const minRuleCode = `E2E_MIN_${Date.now()}`;
  await page.getByLabel("Rule code").fill(minRuleCode);
  await page.getByRole("combobox", { name: "Rule type" }).click();
  await page.getByRole("option", { name: "MIN_QUANTITY" }).click();
  await expect(page.getByLabel("Parameters (raw JSON)")).toHaveCount(0);
  await page.getByLabel("Ingredient code").fill("E2E_TEST_INGREDIENT_MIN");
  await page.getByLabel("Min quantity").fill("2");
  await page.getByLabel("Violation message").fill("E2E min-quantity rule — safe to ignore.");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByText(minRuleCode)).toBeVisible({ timeout: 10000 });

  await page.getByRole("button", { name: "New rule" }).click();
  const optionRuleCode = `E2E_OPTION_${Date.now()}`;
  await page.getByLabel("Rule code").fill(optionRuleCode);
  await page.getByRole("combobox", { name: "Rule type" }).click();
  await page.getByRole("option", { name: "OPTION_ALLOWED" }).click();
  await expect(page.getByLabel("Parameters (raw JSON)")).toHaveCount(0);
  await page.getByLabel("Ingredient code").fill("E2E_TEST_INGREDIENT_OPTION");
  await page.getByLabel("Violation message").fill("E2E option-allowed rule — safe to ignore.");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByText(optionRuleCode)).toBeVisible({ timeout: 10000 });
});

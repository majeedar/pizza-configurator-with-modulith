// Admin Portal DTOs (agent.md §8.3), mirrored field-for-field from the backend
// admin controllers/records — see catalog/rules/pricing/security/admin *AdminController.java.

export type IngredientType = "BASE" | "EXTRA" | "CHEESE" | "MEAT" | "VEGETABLE" | "SAUCE" | "OTHER";
export const INGREDIENT_TYPES: IngredientType[] = ["BASE", "EXTRA", "CHEESE", "MEAT", "VEGETABLE", "SAUCE", "OTHER"];

export interface PizzaAdmin {
  pizzaId: string;
  code: string;
  name: string;
  description: string | null;
  basePrice: number;
  active: boolean;
  imageUrl: string | null;
  version: number;
}

export interface PizzaAdminRequest {
  code: string;
  name: string;
  description: string | null;
  basePrice: number;
  active: boolean;
}

export interface IngredientAdmin {
  ingredientId: string;
  code: string;
  name: string;
  type: IngredientType;
  active: boolean;
  defaultUnit: string | null;
}

export interface IngredientAdminRequest {
  code: string;
  name: string;
  type: IngredientType;
  active: boolean;
  defaultUnit: string | null;
}

export interface SizeAdmin {
  sizeId: string;
  code: string;
  displayName: string;
  priceModifier: number;
  active: boolean;
}

export interface SizeAdminRequest {
  code: string;
  displayName: string;
  priceModifier: number;
  active: boolean;
}

export interface DoughAdmin {
  doughId: string;
  code: string;
  displayName: string;
  priceModifier: number;
  active: boolean;
}

export interface DoughAdminRequest {
  code: string;
  displayName: string;
  priceModifier: number;
  active: boolean;
}

export interface RecipeLineAdmin {
  pizzaIngredientId: string;
  ingredientCode: string;
  ingredientName: string;
  defaultQuantity: number;
  removable: boolean;
}

export interface RecipeLineAdminRequest {
  ingredientCode: string;
  defaultQuantity: number;
  removable: boolean;
}

export type RuleType =
  | "MAX_QUANTITY"
  | "MIN_QUANTITY"
  | "REMOVAL_ALLOWED"
  | "REQUIRES"
  | "EXCLUDES"
  | "SIZE_COMPATIBILITY"
  | "DOUGH_COMPATIBILITY"
  | "INGREDIENT_COMPATIBILITY"
  | "OPTION_ALLOWED";

export const RULE_TYPES: RuleType[] = [
  "MAX_QUANTITY",
  "MIN_QUANTITY",
  "REMOVAL_ALLOWED",
  "REQUIRES",
  "EXCLUDES",
  "SIZE_COMPATIBILITY",
  "DOUGH_COMPATIBILITY",
  "INGREDIENT_COMPATIBILITY",
  "OPTION_ALLOWED",
];

export type ScopeType = "GLOBAL" | "PIZZA";

export interface RuleAdmin {
  ruleId: string;
  ruleCode: string;
  ruleType: RuleType;
  scopeType: ScopeType;
  scopeId: string | null;
  parameters: Record<string, unknown>;
  message: string;
  active: boolean;
  version: number;
  validFrom: string | null;
  validTo: string | null;
}

export interface RuleAdminRequest {
  ruleCode: string;
  ruleType: RuleType;
  scopeType: ScopeType;
  scopeId: string | null;
  parameters: Record<string, unknown>;
  message: string;
  active: boolean;
  validFrom: string | null;
  validTo: string | null;
}

export type ItemType = "PIZZA" | "INGREDIENT" | "SIZE" | "DOUGH";
export const ITEM_TYPES: ItemType[] = ["PIZZA", "INGREDIENT", "SIZE", "DOUGH"];

export interface PriceAdmin {
  priceId: string;
  itemType: ItemType;
  itemId: string;
  amount: number;
  currency: string;
  active: boolean;
  version: number;
  validFrom: string | null;
  validTo: string | null;
}

export interface PriceAdminRequest {
  itemType: ItemType;
  itemId: string;
  amount: number;
  currency: string;
  active: boolean;
  validFrom: string | null;
  validTo: string | null;
}

export type EmployeeRole = "ADMIN" | "KITCHEN";

export interface StaffAdmin {
  employeeId: string;
  username: string;
  displayName: string;
  role: EmployeeRole;
  enabled: boolean;
}

export interface StaffCreateRequest {
  username: string;
  displayName: string;
  email: string | null;
  password: string;
  role: EmployeeRole;
}

export type Platform = "ANDROID";
export type Audience = "CUSTOMER" | "KITCHEN";

export interface AppLinkAdmin {
  appLinkId: string;
  platform: Platform;
  audience: Audience;
  url: string;
  active: boolean;
  updatedBy: string | null;
  updatedAt: string;
}

export interface AppLinkUpdateRequest {
  url: string;
  active: boolean;
}

export interface AuditEventAdmin {
  eventId: string;
  timestamp: string;
  actorId: string;
  actorRole: string;
  action: string;
  entityType: string;
  entityId: string;
  beforeJson: string | null;
  afterJson: string | null;
  correlationId: string;
}

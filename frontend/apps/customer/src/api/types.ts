// Mirrors the backend's JSON shapes (agent.md §10). Kept as plain types,
// not runtime-validated (Zod was scoped out of this pass — see README).

export interface PizzaSummary {
  pizzaId: string;
  code: string;
  name: string;
  description: string | null;
  basePrice: number;
  imageUrl: string | null;
}

export interface RecipeItem {
  ingredientCode: string;
  ingredientName: string;
  defaultQuantity: number;
  removable: boolean;
}

export interface IngredientOption {
  code: string;
  name: string;
  type: string;
}

export interface SizeOption {
  code: string;
  displayName: string;
  priceModifier: number;
}

export interface DoughOption {
  code: string;
  displayName: string;
  priceModifier: number;
}

export interface ConfigurableOptions {
  pizzaId: string;
  pizzaName: string;
  baseIngredients: RecipeItem[];
  availableExtras: IngredientOption[];
  sizes: SizeOption[];
  doughs: DoughOption[];
}

export interface ExtraSelection {
  ingredientCode: string;
  quantity: number;
}

export type ValidationStatus =
  | "DRAFT"
  | "VALID"
  | "INVALID"
  | "PENDING_REVIEW"
  | "REVIEW_APPROVED"
  | "REVIEW_REJECTED"
  | "EXPIRED";

export type PriceStatus = "UNPRICED" | "PRICED" | "READY_FOR_CHECKOUT" | "EXPIRED";

export interface ConfigurationSessionView {
  configurationId: string;
  customerId: string | null;
  pizzaId: string;
  sizeCode: string;
  doughCode: string;
  removedIngredientCodes: string[];
  extras: ExtraSelection[];
  comment: string | null;
  validationStatus: ValidationStatus;
  ruleVersion: string | null;
  priceStatus: PriceStatus;
  calculatedPrice: number | null;
  priceVersion: string | null;
  currency: string;
  expiresAt: string;
}

export interface Violation {
  code: string;
  field: string;
  ruleCode: string;
  message: string;
}

export interface ConfigurationSuggestion {
  description: string;
  patch: Record<string, unknown>;
}

export interface ValidationResponse {
  session: ConfigurationSessionView;
  violations: Violation[];
  suggestions: ConfigurationSuggestion[];
}

export interface PriceQuote {
  currency: string;
  base: number;
  size: number;
  dough: number;
  extras: number;
  total: number;
  priceVersion: string;
}

export interface PriceResponse {
  session: ConfigurationSessionView;
  quote: PriceQuote;
}

export interface BasketItemView {
  basketItemId: string;
  configurationId: string;
  quantity: number;
  pizzaName: string;
  sizeCode: string;
  doughCode: string;
  unitPrice: number;
  currency: string;
  lineTotal: number;
}

export interface BasketView {
  basketId: string;
  sessionToken: string;
  items: BasketItemView[];
  total: number;
  currency: string;
}

export interface AuthResponse {
  customerId: string;
  name: string;
  email: string;
  token: string;
}

export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
}

export type OrderStatus = "CONFIRMED" | "APPROVED" | "IN_PROCESSING" | "READY" | "COMPLETED" | "CANCELLED" | "REJECTED";

export interface OrderItemView {
  orderItemId: string;
  pizzaId: string;
  pizzaNameSnapshot: string;
  sizeCode: string;
  doughCode: string;
  quantity: number;
  modificationsJson: string | null;
  unitPrice: number;
  subtotal: number;
}

export interface OrderView {
  orderId: string;
  displayNumber: string;
  status: OrderStatus;
  totalPrice: number;
  currency: string;
  customerId: string | null;
  customNotes: string | null;
  pickupToken: string;
  items: OrderItemView[];
  createdAt: string;
}

export interface OrderCheckoutResponse {
  order: OrderView;
  accessToken: string | null;
}

export type ReviewRequestStatus =
  | "OPEN"
  | "ACCEPTED_BY_KITCHEN"
  | "RECOMMENDED_BY_KITCHEN"
  | "REJECTED_BY_KITCHEN"
  | "RECOMMENDATION_ACCEPTED_BY_CUSTOMER"
  | "RECOMMENDATION_REJECTED_BY_CUSTOMER";

export interface ProposedExtra {
  ingredientCode: string;
  quantity: number;
}

export interface ProposedModification {
  removedIngredientCodes: string[];
  extras: ProposedExtra[];
  sizeCode: string;
  doughCode: string;
}

export interface ReviewRequestView {
  reviewRequestId: string;
  configurationId: string;
  status: ReviewRequestStatus;
  reason: string | null;
  originalRequestJson: string;
  proposedModificationJson: string | null;
  reviewedBy: string | null;
  reviewedAt: string | null;
  customerResponse: "ACCEPTED" | "REJECTED" | null;
  customerRespondedAt: string | null;
  createdAt: string;
}

export interface ReviewOutcome {
  reviewRequest: ReviewRequestView;
  session: ConfigurationSessionView;
}

export interface AppLinkView {
  appLinkId: string;
  platform: string;
  audience: string;
  url: string;
  active: boolean;
  updatedBy: string | null;
  updatedAt: string;
}

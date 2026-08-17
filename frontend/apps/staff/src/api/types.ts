export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
}

export type StaffRole = "KITCHEN" | "ADMIN";

export interface StaffLoginResponse {
  employeeId: string;
  username: string;
  displayName: string;
  role: StaffRole;
  token: string;
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

export type ReviewRequestStatus =
  | "OPEN"
  | "ACCEPTED_BY_KITCHEN"
  | "RECOMMENDED_BY_KITCHEN"
  | "REJECTED_BY_KITCHEN"
  | "RECOMMENDATION_ACCEPTED_BY_CUSTOMER"
  | "RECOMMENDATION_REJECTED_BY_CUSTOMER";

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

export interface OriginalConfigurationRequest {
  pizzaId: string;
  sizeCode: string;
  doughCode: string;
  removedIngredientCodes: string[];
  extras: { ingredientCode: string; quantity: number }[];
  comment: string | null;
}

export interface ConfigurationPatchRequest {
  removedIngredientCodes: string[];
  extras: { ingredientCode: string; quantity: number }[];
  sizeCode: string;
  doughCode: string;
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

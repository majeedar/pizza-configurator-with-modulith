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

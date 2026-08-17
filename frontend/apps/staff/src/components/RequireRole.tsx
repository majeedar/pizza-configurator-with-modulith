import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../state/AuthContext";
import type { StaffRole } from "../api/types";

/**
 * Role-level gate on top of RequireAuth (agent.md §8.3 — Admin Portal is
 * ROLE_ADMIN only). Assumes it's rendered inside a RequireAuth already, so
 * `role` is non-null by the time this renders for a real user; a
 * defensively-absent role still redirects rather than rendering.
 */
export default function RequireRole({ role, children }: { role: StaffRole; children: ReactNode }) {
  const { role: currentRole } = useAuth();
  if (currentRole !== role) {
    return <Navigate to="/board" replace />;
  }
  return <>{children}</>;
}

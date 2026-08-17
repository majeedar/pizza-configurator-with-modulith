import { useEffect, useState } from "react";
import { Alert, Box, Button, CircularProgress, Stack, Typography } from "@mui/material";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useAuth } from "../state/AuthContext";
import { getGuestOrderToken } from "../state/guestOrderTokens";
import { ApiError } from "../api/client";
import * as ordersApi from "../api/orders";
import type { OrderView } from "../api/types";

const TERMINAL_STATUSES = new Set(["COMPLETED", "CANCELLED", "REJECTED"]);
const POLL_INTERVAL_MS = 5000;

const STATUS_LABEL: Record<string, string> = {
  CONFIRMED: "Confirmed — waiting for the kitchen to approve it",
  APPROVED: "Approved — the kitchen will start on it shortly",
  IN_PROCESSING: "Being made right now",
  READY: "Ready for pickup!",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
  REJECTED: "Rejected",
};

/**
 * Polls order status (agent.md §7.7) — the same pull-based mechanism the
 * native customer Android app uses, and the only one this web app has,
 * since it has no push notification channel (that's Android-only, §7.9).
 */
export default function OrderStatusPage() {
  const { displayNumber } = useParams<{ displayNumber: string }>();
  const [searchParams] = useSearchParams();
  const { token } = useAuth();
  const navigate = useNavigate();

  const [order, setOrder] = useState<OrderView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!displayNumber) return;
    const guestAccessToken = searchParams.get("token") ?? getGuestOrderToken(displayNumber);
    let cancelled = false;

    async function poll() {
      while (!cancelled) {
        try {
          const result = await ordersApi.fetchOrderStatus(displayNumber!, guestAccessToken, token);
          if (cancelled) return;
          setOrder(result);
          setLoading(false);
          if (TERMINAL_STATUSES.has(result.status)) return;
        } catch (err) {
          if (cancelled) return;
          setError(err instanceof ApiError ? err.message : "Could not load order status.");
          setLoading(false);
          return;
        }
        await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
      }
    }

    poll();
    return () => {
      cancelled = true;
    };
  }, [displayNumber, searchParams, token]);

  if (loading) {
    return <CircularProgress />;
  }

  if (error) {
    return (
      <Box sx={{ maxWidth: 480 }}>
        <Alert severity="error">{error}</Alert>
        <Button sx={{ mt: 2 }} onClick={() => navigate("/")}>
          Back to menu
        </Button>
      </Box>
    );
  }

  if (!order) {
    return null;
  }

  return (
    <Box sx={{ maxWidth: 480 }}>
      <Typography variant="h4" gutterBottom>
        Order {order.displayNumber}
      </Typography>
      <Typography variant="h6" color="primary" gutterBottom>
        {STATUS_LABEL[order.status] ?? order.status}
      </Typography>

      <Stack spacing={1} sx={{ mt: 3 }}>
        <Typography>Pickup code: {order.pickupToken}</Typography>
        <Typography>
          Total: {order.totalPrice.toFixed(2)} {order.currency}
        </Typography>
        {order.customNotes && <Typography color="text.secondary">Notes: {order.customNotes}</Typography>}
      </Stack>

      <Stack spacing={0.5} sx={{ mt: 3 }}>
        {order.items.map((item) => (
          <Typography key={item.orderItemId}>
            {item.quantity}× {item.pizzaNameSnapshot} ({item.sizeCode}, {item.doughCode}) —{" "}
            {item.subtotal.toFixed(2)} {order.currency}
          </Typography>
        ))}
      </Stack>

      {!TERMINAL_STATUSES.has(order.status) && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 3 }}>
          This page updates automatically — no need to refresh.
        </Typography>
      )}

      <Button sx={{ mt: 3 }} onClick={() => navigate("/")}>
        Back to menu
      </Button>
    </Box>
  );
}

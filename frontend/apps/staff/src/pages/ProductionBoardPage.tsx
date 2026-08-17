import { useCallback, useEffect, useState } from "react";
import { Box, Button, Card, CardActions, CardContent, Chip, Grid, Stack, Typography } from "@mui/material";
import { useAuth } from "../state/AuthContext";
import { approveOrder, completeOrder, fetchActiveOrders, markOrderReady, startOrder } from "../api/kitchen";
import { connectKitchenStream } from "../api/kitchenStream";
import type { OrderStatus, OrderView } from "../api/types";

const COLUMNS: { status: OrderStatus; title: string }[] = [
  { status: "CONFIRMED", title: "Confirmed" },
  { status: "APPROVED", title: "Approved" },
  { status: "IN_PROCESSING", title: "In processing" },
  { status: "READY", title: "Ready" },
];

const NEXT_ACTION: Partial<Record<OrderStatus, { label: string; run: (orderId: string, token: string) => Promise<OrderView> }>> = {
  CONFIRMED: { label: "Approve", run: approveOrder },
  APPROVED: { label: "Start", run: startOrder },
  IN_PROCESSING: { label: "Mark ready", run: markOrderReady },
  READY: { label: "Complete", run: completeOrder },
};

export default function ProductionBoardPage() {
  const { token } = useAuth();
  const [orders, setOrders] = useState<OrderView[]>([]);
  const [busyOrderId, setBusyOrderId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    try {
      setOrders(await fetchActiveOrders(token));
    } catch {
      setError("Could not load the production board — retrying automatically.");
    }
  }, [token]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // SSE is purely a "something changed, refetch" signal (agent.md §17) — it
  // never carries state the UI trusts directly.
  useEffect(() => {
    if (!token) return;
    return connectKitchenStream(token, () => refresh());
  }, [token, refresh]);

  async function handleAction(order: OrderView) {
    const action = NEXT_ACTION[order.status];
    if (!action || !token) return;
    setBusyOrderId(order.orderId);
    setError(null);
    try {
      await action.run(order.orderId, token);
      await refresh();
    } catch {
      setError(`Could not update order ${order.displayNumber}.`);
    } finally {
      setBusyOrderId(null);
    }
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Production board
      </Typography>
      {error && (
        <Typography color="error" sx={{ mb: 2 }}>
          {error}
        </Typography>
      )}

      <Grid container spacing={2}>
        {COLUMNS.map((column) => (
          <Grid key={column.status} size={{ xs: 12, sm: 6, md: 3 }}>
            <Typography variant="h6" gutterBottom>
              {column.title}
            </Typography>
            <Stack spacing={2}>
              {orders
                .filter((order) => order.status === column.status)
                .map((order) => (
                  <OrderCard
                    key={order.orderId}
                    order={order}
                    busy={busyOrderId === order.orderId}
                    onAction={() => handleAction(order)}
                  />
                ))}
            </Stack>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}

function OrderCard({ order, busy, onAction }: { order: OrderView; busy: boolean; onAction: () => void }) {
  const action = NEXT_ACTION[order.status];
  return (
    <Card variant="outlined" sx={{ borderWidth: 2 }}>
      <CardContent>
        <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center" }}>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>
            {order.displayNumber}
          </Typography>
          <Chip label={order.pickupToken} size="small" />
        </Stack>
        {order.items.map((item) => (
          <Typography key={item.orderItemId} variant="body1">
            {item.quantity}× {item.pizzaNameSnapshot} ({item.sizeCode}, {item.doughCode})
          </Typography>
        ))}
        {order.customNotes && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            "{order.customNotes}"
          </Typography>
        )}
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          {new Date(order.createdAt).toLocaleTimeString()}
        </Typography>
      </CardContent>
      {action && (
        <CardActions>
          <Button fullWidth variant="contained" size="large" onClick={onAction} disabled={busy}>
            {action.label}
          </Button>
        </CardActions>
      )}
    </Card>
  );
}

import { useMemo, useState } from "react";
import { Alert, Box, Button, CircularProgress, Divider, IconButton, Stack, TextField, Typography } from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import { useNavigate } from "react-router-dom";
import { useBasket } from "../state/BasketContext";
import { useAuth } from "../state/AuthContext";
import { saveGuestOrderToken } from "../state/guestOrderTokens";
import { ApiError } from "../api/client";
import * as ordersApi from "../api/orders";

export default function BasketPage() {
  const { basket, loading, removeItem, startNewBasket } = useBasket();
  const { token } = useAuth();
  const navigate = useNavigate();

  const [customNotes, setCustomNotes] = useState("");
  const [placingOrder, setPlacingOrder] = useState(false);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);

  // Generated once per checkout attempt and reused across retries of *this*
  // attempt (agent.md §7.7/§15.1) — a fresh key per click would defeat the
  // backend's replay protection, so it's derived from the basket id rather
  // than regenerated on every render.
  const idempotencyKey = useMemo(() => crypto.randomUUID(), [basket?.basketId]);

  async function handleCheckout() {
    if (!basket) return;
    setPlacingOrder(true);
    setCheckoutError(null);
    try {
      const response = await ordersApi.createOrder(basket.basketId, customNotes.trim() || null, idempotencyKey, token);
      if (response.accessToken) {
        saveGuestOrderToken(response.order.displayNumber, response.accessToken);
      }
      await startNewBasket();
      const tokenParam = response.accessToken ? `?token=${encodeURIComponent(response.accessToken)}` : "";
      navigate(`/order/${response.order.displayNumber}${tokenParam}`);
    } catch (err) {
      setCheckoutError(err instanceof ApiError ? err.message : "Could not place your order — please try again.");
      setPlacingOrder(false);
    }
  }

  if (loading || !basket) {
    return <CircularProgress />;
  }

  return (
    <Box sx={{ maxWidth: 640 }}>
      <Typography variant="h4" gutterBottom>
        Your basket
      </Typography>

      {basket.items.length === 0 ? (
        <Alert severity="info">
          Your basket is empty. <Button onClick={() => navigate("/")}>Browse pizzas</Button>
        </Alert>
      ) : (
        <Stack spacing={2}>
          {basket.items.map((item) => (
            <Stack key={item.basketItemId} direction="row" spacing={2} sx={{ alignItems: "center" }}>
              <Box sx={{ flexGrow: 1 }}>
                <Typography>
                  {item.pizzaName} ({item.sizeCode}, {item.doughCode}) × {item.quantity}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {item.unitPrice.toFixed(2)} {item.currency} each
                </Typography>
              </Box>
              <Typography sx={{ fontWeight: 600 }}>
                {item.lineTotal.toFixed(2)} {item.currency}
              </Typography>
              <IconButton onClick={() => removeItem(item.basketItemId)} aria-label="Remove item">
                <DeleteIcon />
              </IconButton>
            </Stack>
          ))}

          <Divider />

          <Stack direction="row" sx={{ justifyContent: "space-between" }}>
            <Typography variant="h6">Total</Typography>
            <Typography variant="h6">
              {basket.total.toFixed(2)} {basket.currency}
            </Typography>
          </Stack>

          <TextField
            label="Notes for the kitchen (optional)"
            value={customNotes}
            onChange={(e) => setCustomNotes(e.target.value)}
            multiline
            minRows={2}
          />

          {checkoutError && <Alert severity="error">{checkoutError}</Alert>}

          <Button variant="contained" size="large" onClick={handleCheckout} disabled={placingOrder}>
            {placingOrder ? "Placing order…" : "Confirm & Checkout"}
          </Button>
        </Stack>
      )}
    </Box>
  );
}

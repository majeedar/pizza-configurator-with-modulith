import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import RemoveIcon from "@mui/icons-material/Remove";
import { useAuth } from "../state/AuthContext";
import { ApiError } from "../api/client";
import { fetchPizzaOptions } from "../api/catalog";
import { acceptReview, fetchReviewQueue, recommendReview, rejectReview } from "../api/kitchenReviews";
import type { ConfigurableOptions, ConfigurationPatchRequest, OriginalConfigurationRequest, ReviewRequestView } from "../api/types";

const POLL_INTERVAL_MS = 8000;

export default function ReviewQueuePage() {
  const { token } = useAuth();
  const [queue, setQueue] = useState<ReviewRequestView[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<ReviewRequestView | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    try {
      setQueue(await fetchReviewQueue(token));
      setError(null);
    } catch {
      setError("Could not load the review queue — retrying automatically.");
    }
  }, [token]);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [refresh]);

  return (
    <Box>
      <Typography variant="h5" gutterBottom>
        Comment review queue
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Configurations our AI adapter couldn't fully interpret, or that didn't pass validation, land here for a
        human decision.
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {queue.length === 0 && !error && (
        <Typography variant="body2" color="text.secondary">
          Nothing waiting for review right now.
        </Typography>
      )}

      <Stack spacing={2}>
        {queue.map((review) => (
          <Card key={review.reviewRequestId}>
            <CardContent>
              <Stack direction="row" spacing={2} sx={{ justifyContent: "space-between", alignItems: "center" }}>
                <Box>
                  <Typography variant="subtitle1">Configuration {review.configurationId.slice(0, 8)}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {review.reason ?? "Needs review"}
                  </Typography>
                </Box>
                <Chip size="small" label={review.status.replaceAll("_", " ")} color={review.status === "OPEN" ? "warning" : "info"} />
              </Stack>
            </CardContent>
            <CardActions>
              <Button size="small" onClick={() => setSelected(review)}>
                Review
              </Button>
            </CardActions>
          </Card>
        ))}
      </Stack>

      {selected && (
        <ReviewDetailDialog
          review={selected}
          token={token!}
          onClose={() => setSelected(null)}
          onResolved={() => {
            setSelected(null);
            refresh();
          }}
        />
      )}
    </Box>
  );
}

function parseOriginal(json: string): OriginalConfigurationRequest | null {
  try {
    return JSON.parse(json);
  } catch {
    return null;
  }
}

interface ReviewDetailDialogProps {
  review: ReviewRequestView;
  token: string;
  onClose: () => void;
  onResolved: () => void;
}

function ReviewDetailDialog({ review, token, onClose, onResolved }: ReviewDetailDialogProps) {
  const original = parseOriginal(review.originalRequestJson);
  const [options, setOptions] = useState<ConfigurableOptions | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [mode, setMode] = useState<"view" | "recommend">("view");

  const [sizeCode, setSizeCode] = useState("");
  const [doughCode, setDoughCode] = useState("");
  const [removed, setRemoved] = useState<Set<string>>(new Set());
  const [extras, setExtras] = useState<Record<string, number>>({});

  useEffect(() => {
    if (!original) return;
    fetchPizzaOptions(original.pizzaId)
      .then((opts) => {
        setOptions(opts);
        setSizeCode(original.sizeCode);
        setDoughCode(original.doughCode);
        setRemoved(new Set(original.removedIngredientCodes));
        setExtras(Object.fromEntries(original.extras.map((e) => [e.ingredientCode, e.quantity])));
      })
      .catch(() => setLoadError("Could not load this pizza's options."));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [review.reviewRequestId]);

  async function handleAccept() {
    setBusy(true);
    setActionError(null);
    try {
      await acceptReview(review.reviewRequestId, token);
      onResolved();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Could not accept this configuration.");
    } finally {
      setBusy(false);
    }
  }

  async function handleReject() {
    setBusy(true);
    setActionError(null);
    try {
      await rejectReview(review.reviewRequestId, rejectReason.trim() || null, token);
      onResolved();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Could not reject this configuration.");
    } finally {
      setBusy(false);
    }
  }

  async function handleRecommend() {
    setBusy(true);
    setActionError(null);
    try {
      const patch: ConfigurationPatchRequest = {
        removedIngredientCodes: Array.from(removed),
        extras: Object.entries(extras)
          .filter(([, quantity]) => quantity > 0)
          .map(([ingredientCode, quantity]) => ({ ingredientCode, quantity })),
        sizeCode,
        doughCode,
      };
      await recommendReview(review.reviewRequestId, patch, token);
      onResolved();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Could not send this recommendation.");
    } finally {
      setBusy(false);
    }
  }

  function changeExtraQuantity(code: string, delta: number) {
    setExtras((prev) => {
      const next = { ...prev };
      const value = Math.max(0, (next[code] ?? 0) + delta);
      if (value === 0) delete next[code];
      else next[code] = value;
      return next;
    });
  }

  const canDecide = review.status === "OPEN";

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Review configuration</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Typography variant="body2" color="text.secondary">
            {review.reason}
          </Typography>

          {!original && <Alert severity="error">Could not parse the original request.</Alert>}
          {loadError && <Alert severity="error">{loadError}</Alert>}

          {original && (
            <Box>
              <Typography variant="subtitle2">Customer requested</Typography>
              <Typography variant="body2">Pizza: {options?.pizzaName ?? original.pizzaId}</Typography>
              <Typography variant="body2">Size: {original.sizeCode}, Dough: {original.doughCode}</Typography>
              {original.removedIngredientCodes.length > 0 && (
                <Typography variant="body2">Without: {original.removedIngredientCodes.join(", ")}</Typography>
              )}
              {original.extras.length > 0 && (
                <Typography variant="body2">
                  Extras: {original.extras.map((e) => `${e.ingredientCode} ×${e.quantity}`).join(", ")}
                </Typography>
              )}
              {original.comment && (
                <Typography variant="body2" sx={{ fontStyle: "italic", mt: 1 }}>
                  "{original.comment}"
                </Typography>
              )}
            </Box>
          )}

          {actionError && <Alert severity="error">{actionError}</Alert>}
          {!canDecide && <Alert severity="info">This request has already been resolved ({review.status.replaceAll("_", " ")}).</Alert>}

          {mode === "recommend" && options && (
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Propose instead
              </Typography>
              <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: "wrap", mb: 2 }}>
                {options.baseIngredients.filter((i) => i.removable).map((item) => {
                  const isRemoved = removed.has(item.ingredientCode);
                  return (
                    <Chip
                      key={item.ingredientCode}
                      label={item.ingredientName}
                      color={isRemoved ? "default" : "primary"}
                      variant={isRemoved ? "outlined" : "filled"}
                      onClick={() =>
                        setRemoved((prev) => {
                          const next = new Set(prev);
                          if (next.has(item.ingredientCode)) next.delete(item.ingredientCode);
                          else next.add(item.ingredientCode);
                          return next;
                        })
                      }
                    />
                  );
                })}
              </Stack>
              <Stack spacing={1} sx={{ mb: 2 }}>
                {options.availableExtras.map((extra) => (
                  <Stack key={extra.code} direction="row" spacing={2} sx={{ alignItems: "center" }}>
                    <Typography sx={{ flexGrow: 1 }}>{extra.name}</Typography>
                    <IconButton size="small" onClick={() => changeExtraQuantity(extra.code, -1)} disabled={!extras[extra.code]}>
                      <RemoveIcon fontSize="small" />
                    </IconButton>
                    <Typography sx={{ width: 24, textAlign: "center" }}>{extras[extra.code] ?? 0}</Typography>
                    <IconButton size="small" onClick={() => changeExtraQuantity(extra.code, 1)}>
                      <AddIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                ))}
              </Stack>
              <Stack direction="row" spacing={2}>
                <FormControl fullWidth size="small">
                  <InputLabel id="rec-size-label">Size</InputLabel>
                  <Select labelId="rec-size-label" label="Size" value={sizeCode} onChange={(e) => setSizeCode(e.target.value)}>
                    {options.sizes.map((s) => (
                      <MenuItem key={s.code} value={s.code}>
                        {s.displayName}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <FormControl fullWidth size="small">
                  <InputLabel id="rec-dough-label">Dough</InputLabel>
                  <Select labelId="rec-dough-label" label="Dough" value={doughCode} onChange={(e) => setDoughCode(e.target.value)}>
                    {options.doughs.map((d) => (
                      <MenuItem key={d.code} value={d.code}>
                        {d.displayName}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Stack>
            </Box>
          )}

          {mode === "view" && canDecide && (
            <TextField
              label="Rejection reason (optional)"
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              multiline
              minRows={2}
            />
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
        {canDecide && mode === "view" && (
          <>
            <Button color="error" disabled={busy} onClick={handleReject}>
              Reject
            </Button>
            <Button disabled={busy || !options} onClick={() => setMode("recommend")}>
              Recommend changes
            </Button>
            <Button variant="contained" disabled={busy} onClick={handleAccept}>
              {busy ? <CircularProgress size={20} /> : "Accept as requested"}
            </Button>
          </>
        )}
        {canDecide && mode === "recommend" && (
          <>
            <Button onClick={() => setMode("view")}>Back</Button>
            <Button variant="contained" disabled={busy} onClick={handleRecommend}>
              {busy ? <CircularProgress size={20} /> : "Send recommendation"}
            </Button>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
}

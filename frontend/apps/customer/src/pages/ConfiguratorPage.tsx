import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
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
import { useNavigate, useParams } from "react-router-dom";
import { fetchPizzaOptions } from "../api/catalog";
import { createConfiguration, priceConfiguration, updateConfiguration, validateConfiguration } from "../api/configuration";
import type { ConfigurableOptions, PriceQuote, Violation } from "../api/types";
import { useAuth } from "../state/AuthContext";
import { useBasket } from "../state/BasketContext";
import { ApiError } from "../api/client";

export default function ConfiguratorPage() {
  const { pizzaId } = useParams<{ pizzaId: string }>();
  const { token } = useAuth();
  const { addItem } = useBasket();
  const navigate = useNavigate();

  const [options, setOptions] = useState<ConfigurableOptions | null>(null);
  const [configurationId, setConfigurationId] = useState<string | null>(null);
  const [sizeCode, setSizeCode] = useState("");
  const [doughCode, setDoughCode] = useState("");
  const [removed, setRemoved] = useState<Set<string>>(new Set());
  const [extras, setExtras] = useState<Record<string, number>>({});
  const [comment, setComment] = useState("");

  const [status, setStatus] = useState<"idle" | "checking">("idle");
  const [validationStatus, setValidationStatus] = useState<string | null>(null);
  const [violations, setViolations] = useState<Violation[]>([]);
  const [quote, setQuote] = useState<PriceQuote | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    if (!pizzaId) return;
    fetchPizzaOptions(pizzaId).then((loaded) => {
      setOptions(loaded);
      setSizeCode(loaded.sizes[0]?.code ?? "");
      setDoughCode(loaded.doughs[0]?.code ?? "");
    });
  }, [pizzaId]);

  const removableBaseIngredients = useMemo(
    () => options?.baseIngredients.filter((item) => item.removable) ?? [],
    [options]
  );

  function toggleRemoved(code: string) {
    setRemoved((prev) => {
      const next = new Set(prev);
      if (next.has(code)) next.delete(code);
      else next.add(code);
      return next;
    });
    resetOutcome();
  }

  function changeExtraQuantity(code: string, delta: number) {
    setExtras((prev) => {
      const next = { ...prev };
      const value = Math.max(0, (next[code] ?? 0) + delta);
      if (value === 0) delete next[code];
      else next[code] = value;
      return next;
    });
    resetOutcome();
  }

  function resetOutcome() {
    setValidationStatus(null);
    setViolations([]);
    setQuote(null);
  }

  async function checkAvailabilityAndPrice() {
    if (!pizzaId) return;
    setStatus("checking");
    setError(null);
    resetOutcome();
    try {
      const input = {
        pizzaId,
        sizeCode,
        doughCode,
        removedIngredients: Array.from(removed),
        extras: Object.entries(extras).map(([ingredientCode, quantity]) => ({ ingredientCode, quantity })),
        comment: comment.trim() === "" ? null : comment,
      };

      const session = configurationId
        ? await updateConfiguration(configurationId, input)
        : await createConfiguration(input, token);
      setConfigurationId(session.configurationId);

      const validation = await validateConfiguration(session.configurationId);
      setValidationStatus(validation.session.validationStatus);
      setViolations(validation.violations);

      if (validation.session.validationStatus === "VALID") {
        const priced = await priceConfiguration(session.configurationId);
        setQuote(priced.quote);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong — please try again");
    } finally {
      setStatus("idle");
    }
  }

  async function handleAddToBasket() {
    if (!configurationId) return;
    setAdding(true);
    setError(null);
    try {
      await addItem(configurationId, 1);
      navigate("/basket");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not add to basket");
    } finally {
      setAdding(false);
    }
  }

  if (!options) {
    return <CircularProgress />;
  }

  const readyForCheckout = validationStatus === "VALID" && quote !== null;

  return (
    <Box sx={{ maxWidth: 640 }}>
      <Typography variant="h4" gutterBottom>
        Configure your pizza
      </Typography>

      <Stack spacing={3}>
        <Box>
          <Typography variant="subtitle1" gutterBottom>
            Base ingredients
          </Typography>
          <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: "wrap" }}>
            {options.baseIngredients.map((item) => {
              const isRemovable = item.removable;
              const isRemoved = removed.has(item.ingredientCode);
              return (
                <Chip
                  key={item.ingredientCode}
                  label={item.ingredientName}
                  color={isRemoved ? "default" : "primary"}
                  variant={isRemoved ? "outlined" : "filled"}
                  onClick={isRemovable ? () => toggleRemoved(item.ingredientCode) : undefined}
                  onDelete={isRemovable ? () => toggleRemoved(item.ingredientCode) : undefined}
                  title={isRemovable ? "Click to remove/re-add" : "Cannot be removed"}
                />
              );
            })}
          </Stack>
          {removableBaseIngredients.length === 0 && (
            <Typography variant="body2" color="text.secondary">
              This pizza's base ingredients are fixed.
            </Typography>
          )}
        </Box>

        <Box>
          <Typography variant="subtitle1" gutterBottom>
            Extras
          </Typography>
          <Stack spacing={1}>
            {options.availableExtras.map((extra) => (
              <Stack key={extra.code} direction="row" spacing={2} sx={{ alignItems: "center" }}>
                <Typography sx={{ flexGrow: 1 }}>{extra.name}</Typography>
                <IconButton
                  size="small"
                  onClick={() => changeExtraQuantity(extra.code, -1)}
                  disabled={!extras[extra.code]}
                >
                  <RemoveIcon fontSize="small" />
                </IconButton>
                <Typography sx={{ width: 24, textAlign: "center" }}>
                  {extras[extra.code] ?? 0}
                </Typography>
                <IconButton size="small" onClick={() => changeExtraQuantity(extra.code, 1)}>
                  <AddIcon fontSize="small" />
                </IconButton>
              </Stack>
            ))}
          </Stack>
        </Box>

        <Stack direction="row" spacing={2}>
          <FormControl fullWidth>
            <InputLabel id="size-label">Size</InputLabel>
            <Select
              labelId="size-label"
              label="Size"
              value={sizeCode}
              onChange={(e) => {
                setSizeCode(e.target.value);
                resetOutcome();
              }}
            >
              {options.sizes.map((size) => (
                <MenuItem key={size.code} value={size.code}>
                  {size.displayName} (+{size.priceModifier.toFixed(2)} €)
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl fullWidth>
            <InputLabel id="dough-label">Dough</InputLabel>
            <Select
              labelId="dough-label"
              label="Dough"
              value={doughCode}
              onChange={(e) => {
                setDoughCode(e.target.value);
                resetOutcome();
              }}
            >
              {options.doughs.map((dough) => (
                <MenuItem key={dough.code} value={dough.code}>
                  {dough.displayName} (+{dough.priceModifier.toFixed(2)} €)
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Stack>

        <TextField
          label="Anything else? (optional)"
          multiline
          minRows={2}
          value={comment}
          onChange={(e) => {
            setComment(e.target.value);
            resetOutcome();
          }}
          helperText="Comment-based requests aren't processed automatically yet — a team member will review them."
        />

        <Divider />

        <Button variant="outlined" onClick={checkAvailabilityAndPrice} disabled={status === "checking"}>
          {status === "checking" ? "Checking…" : "Check availability & price"}
        </Button>

        {error && <Alert severity="error">{error}</Alert>}

        {validationStatus === "INVALID" && (
          <Alert severity="warning">
            <Typography sx={{ fontWeight: 600 }}>This configuration isn't available:</Typography>
            {violations.map((v) => (
              <Typography key={v.code + v.field}>• {v.message}</Typography>
            ))}
          </Alert>
        )}

        {validationStatus === "PENDING_REVIEW" && (
          <Alert severity="info">
            Your comment has been noted. Comment-based requests aren't processed automatically yet, so this
            configuration needs manual review before it can be priced — please remove the comment to continue now,
            or check back later.
          </Alert>
        )}

        {quote && (
          <Box>
            <Typography variant="subtitle1">Price breakdown</Typography>
            <Stack spacing={0.5}>
              <Typography variant="body2">Base: {quote.base.toFixed(2)} €</Typography>
              <Typography variant="body2">Size: {quote.size.toFixed(2)} €</Typography>
              <Typography variant="body2">Dough: {quote.dough.toFixed(2)} €</Typography>
              <Typography variant="body2">Extras: {quote.extras.toFixed(2)} €</Typography>
              <Typography variant="h6">Total: {quote.total.toFixed(2)} {quote.currency}</Typography>
            </Stack>
          </Box>
        )}

        <Button
          variant="contained"
          size="large"
          disabled={!readyForCheckout || adding}
          onClick={handleAddToBasket}
        >
          {adding ? "Adding…" : "Add to Basket"}
        </Button>
      </Stack>
    </Box>
  );
}

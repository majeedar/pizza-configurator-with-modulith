import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../../state/AuthContext";
import { ApiError } from "../../api/client";
import * as catalogAdmin from "../../api/catalogAdmin";
import type { IngredientAdmin, PizzaAdmin, RecipeLineAdmin, RecipeLineAdminRequest } from "../../api/adminTypes";
import AdminDataTable, { type AdminColumn } from "../../components/admin/AdminDataTable";
import AdminFormDialog from "../../components/admin/AdminFormDialog";

const EMPTY_FORM: RecipeLineAdminRequest = { ingredientCode: "", defaultQuantity: 1, removable: true };

/** Manages a single pizza's base recipe (agent.md §5.1 PizzaIngredient) — reached from PizzasAdminPage's "Recipe" action. */
export default function PizzaRecipePage() {
  const { pizzaId } = useParams<{ pizzaId: string }>();
  const { token } = useAuth();
  const navigate = useNavigate();

  const [pizza, setPizza] = useState<PizzaAdmin | null>(null);
  const [lines, setLines] = useState<RecipeLineAdmin[]>([]);
  const [ingredients, setIngredients] = useState<IngredientAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<RecipeLineAdmin | null>(null);
  const [form, setForm] = useState<RecipeLineAdminRequest>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!token || !pizzaId) return;
    setLoading(true);
    setLoadError(null);
    try {
      const [pizzaResult, linesResult, ingredientsResult] = await Promise.all([
        catalogAdmin.getPizza(pizzaId, token),
        catalogAdmin.listRecipe(pizzaId, token),
        catalogAdmin.listIngredients(token),
      ]);
      setPizza(pizzaResult);
      setLines(linesResult);
      setIngredients(ingredientsResult);
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "Could not load the recipe.");
    } finally {
      setLoading(false);
    }
  }, [token, pizzaId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function openCreate() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setFormError(null);
    setDialogOpen(true);
  }

  function openEdit(line: RecipeLineAdmin) {
    setEditing(line);
    setForm({ ingredientCode: line.ingredientCode, defaultQuantity: line.defaultQuantity, removable: line.removable });
    setFormError(null);
    setDialogOpen(true);
  }

  async function handleSubmit() {
    if (!token || !pizzaId) return;
    setSubmitting(true);
    setFormError(null);
    try {
      if (editing) {
        await catalogAdmin.updateRecipeLine(pizzaId, editing.pizzaIngredientId, form, token);
      } else {
        await catalogAdmin.addRecipeLine(pizzaId, form, token);
      }
      setDialogOpen(false);
      await refresh();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Could not save the recipe line.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleRemove(line: RecipeLineAdmin) {
    if (!token || !pizzaId) return;
    if (!window.confirm(`Remove ${line.ingredientName} from this recipe?`)) return;
    try {
      await catalogAdmin.removeRecipeLine(pizzaId, line.pizzaIngredientId, token);
      await refresh();
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "Could not remove the recipe line.");
    }
  }

  const columns: AdminColumn<RecipeLineAdmin>[] = [
    { key: "ingredientName", label: "Ingredient" },
    { key: "ingredientCode", label: "Code" },
    { key: "defaultQuantity", label: "Default qty", align: "right" },
    { key: "removable", label: "Removable", render: (row) => (row.removable ? "Yes" : "No") },
  ];

  return (
    <Box>
      <Button onClick={() => navigate("/admin/pizzas")} sx={{ mb: 2 }} size="small">
        ← Back to Pizzas
      </Button>

      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">{pizza ? `${pizza.name} — recipe` : "Recipe"}</Typography>
        <Button variant="contained" size="medium" onClick={openCreate}>
          Add ingredient
        </Button>
      </Stack>

      {loadError && <Alert severity="error" sx={{ mb: 2 }}>{loadError}</Alert>}
      {!loading && (
        <AdminDataTable
          columns={columns}
          rows={lines}
          rowKey={(row) => row.pizzaIngredientId}
          emptyMessage="No base ingredients yet."
          actions={(row) => (
            <Stack direction="row" spacing={1} sx={{ justifyContent: "flex-end" }}>
              <Button size="small" onClick={() => openEdit(row)}>
                Edit
              </Button>
              <Button size="small" color="error" onClick={() => handleRemove(row)}>
                Remove
              </Button>
            </Stack>
          )}
        />
      )}

      <AdminFormDialog
        open={dialogOpen}
        title={editing ? `Edit ${editing.ingredientName}` : "Add ingredient to recipe"}
        submitting={submitting}
        error={formError}
        onSubmit={handleSubmit}
        onClose={() => setDialogOpen(false)}
      >
        <TextField
          select
          label="Ingredient"
          value={form.ingredientCode}
          onChange={(e) => setForm({ ...form, ingredientCode: e.target.value })}
          disabled={!!editing}
          helperText={editing ? "The ingredient itself can't be swapped — remove and re-add instead." : undefined}
          required
        >
          {ingredients
            // Only offer ingredients not already on this recipe — the backend
            // rejects a duplicate with a 409 (catalog.pizza_ingredient has a
            // unique (pizza, ingredient) constraint), but filtering here means
            // an admin can't hit that conflict from the dropdown at all.
            .filter(
              (ingredient) =>
                ingredient.code === editing?.ingredientCode ||
                !lines.some((line) => line.ingredientCode === ingredient.code)
            )
            .map((ingredient) => (
              <MenuItem key={ingredient.code} value={ingredient.code}>
                {ingredient.name} ({ingredient.code})
              </MenuItem>
            ))}
        </TextField>
        <TextField
          label="Default quantity"
          type="number"
          value={form.defaultQuantity}
          onChange={(e) => setForm({ ...form, defaultQuantity: Number(e.target.value) })}
          required
        />
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <Switch checked={form.removable} onChange={(e) => setForm({ ...form, removable: e.target.checked })} />
          <Typography>Customer can remove it</Typography>
        </Stack>
      </AdminFormDialog>
    </Box>
  );
}

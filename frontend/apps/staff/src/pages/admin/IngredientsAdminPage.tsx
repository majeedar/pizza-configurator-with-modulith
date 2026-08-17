import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import { useAuth } from "../../state/AuthContext";
import { ApiError } from "../../api/client";
import * as catalogAdmin from "../../api/catalogAdmin";
import { INGREDIENT_TYPES, type IngredientAdmin, type IngredientAdminRequest } from "../../api/adminTypes";
import AdminDataTable, { type AdminColumn } from "../../components/admin/AdminDataTable";
import AdminFormDialog from "../../components/admin/AdminFormDialog";

const EMPTY_FORM: IngredientAdminRequest = { code: "", name: "", type: "EXTRA", active: true, defaultUnit: null };

export default function IngredientsAdminPage() {
  const { token } = useAuth();
  const [ingredients, setIngredients] = useState<IngredientAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<IngredientAdmin | null>(null);
  const [form, setForm] = useState<IngredientAdminRequest>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setLoadError(null);
    try {
      setIngredients(await catalogAdmin.listIngredients(token));
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "Could not load ingredients.");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function openCreate() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setFormError(null);
    setDialogOpen(true);
  }

  function openEdit(ingredient: IngredientAdmin) {
    setEditing(ingredient);
    setForm({
      code: ingredient.code,
      name: ingredient.name,
      type: ingredient.type,
      active: ingredient.active,
      defaultUnit: ingredient.defaultUnit,
    });
    setFormError(null);
    setDialogOpen(true);
  }

  async function handleSubmit() {
    if (!token) return;
    setSubmitting(true);
    setFormError(null);
    try {
      if (editing) {
        await catalogAdmin.updateIngredient(editing.ingredientId, form, token);
      } else {
        await catalogAdmin.createIngredient(form, token);
      }
      setDialogOpen(false);
      await refresh();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Could not save ingredient.");
    } finally {
      setSubmitting(false);
    }
  }

  const columns: AdminColumn<IngredientAdmin>[] = [
    { key: "code", label: "Code" },
    { key: "name", label: "Name" },
    { key: "type", label: "Type" },
    { key: "defaultUnit", label: "Unit", render: (row) => row.defaultUnit ?? "—" },
    {
      key: "active",
      label: "Status",
      render: (row) => <Chip size="small" label={row.active ? "Active" : "Inactive"} color={row.active ? "success" : "default"} />,
    },
  ];

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Ingredients</Typography>
        <Button variant="contained" size="medium" onClick={openCreate}>
          New ingredient
        </Button>
      </Stack>

      {loadError && <Alert severity="error" sx={{ mb: 2 }}>{loadError}</Alert>}
      {!loading && (
        <AdminDataTable
          columns={columns}
          rows={ingredients}
          rowKey={(row) => row.ingredientId}
          actions={(row) => (
            <Button size="small" onClick={() => openEdit(row)}>
              Edit
            </Button>
          )}
        />
      )}

      <AdminFormDialog
        open={dialogOpen}
        title={editing ? `Edit ${editing.code}` : "New ingredient"}
        submitting={submitting}
        error={formError}
        onSubmit={handleSubmit}
        onClose={() => setDialogOpen(false)}
      >
        <TextField
          label="Code"
          value={form.code}
          onChange={(e) => setForm({ ...form, code: e.target.value })}
          disabled={!!editing}
          helperText={editing ? "Code cannot be changed after creation." : undefined}
          required
        />
        <TextField label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        <TextField
          select
          label="Type"
          value={form.type}
          onChange={(e) => setForm({ ...form, type: e.target.value as IngredientAdminRequest["type"] })}
          required
        >
          {INGREDIENT_TYPES.map((type) => (
            <MenuItem key={type} value={type}>
              {type}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          label="Default unit (optional)"
          value={form.defaultUnit ?? ""}
          onChange={(e) => setForm({ ...form, defaultUnit: e.target.value || null })}
          placeholder="e.g. g, slice, portion"
        />
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <Switch checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
          <Typography>Active</Typography>
        </Stack>
      </AdminFormDialog>
    </Box>
  );
}

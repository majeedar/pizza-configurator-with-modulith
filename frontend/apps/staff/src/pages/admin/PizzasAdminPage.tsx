import { useCallback, useEffect, useState } from "react";
import { Alert, Box, Button, Chip, Stack, Switch, TextField, Typography } from "@mui/material";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../state/AuthContext";
import { ApiError } from "../../api/client";
import * as catalogAdmin from "../../api/catalogAdmin";
import type { PizzaAdmin, PizzaAdminRequest } from "../../api/adminTypes";
import AdminDataTable, { type AdminColumn } from "../../components/admin/AdminDataTable";
import AdminFormDialog from "../../components/admin/AdminFormDialog";

const EMPTY_FORM: PizzaAdminRequest = { code: "", name: "", description: null, basePrice: 0, active: true };

export default function PizzasAdminPage() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [pizzas, setPizzas] = useState<PizzaAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<PizzaAdmin | null>(null);
  const [form, setForm] = useState<PizzaAdminRequest>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setLoadError(null);
    try {
      setPizzas(await catalogAdmin.listPizzas(token));
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "Could not load pizzas.");
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

  function openEdit(pizza: PizzaAdmin) {
    setEditing(pizza);
    setForm({ code: pizza.code, name: pizza.name, description: pizza.description, basePrice: pizza.basePrice, active: pizza.active });
    setFormError(null);
    setDialogOpen(true);
  }

  async function handleSubmit() {
    if (!token) return;
    setSubmitting(true);
    setFormError(null);
    try {
      if (editing) {
        await catalogAdmin.updatePizza(editing.pizzaId, form, token);
      } else {
        await catalogAdmin.createPizza(form, token);
      }
      setDialogOpen(false);
      await refresh();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Could not save pizza.");
    } finally {
      setSubmitting(false);
    }
  }

  const columns: AdminColumn<PizzaAdmin>[] = [
    { key: "code", label: "Code" },
    { key: "name", label: "Name" },
    { key: "basePrice", label: "Base price", align: "right", render: (row) => row.basePrice.toFixed(2) },
    {
      key: "active",
      label: "Status",
      render: (row) => <Chip size="small" label={row.active ? "Active" : "Inactive"} color={row.active ? "success" : "default"} />,
    },
  ];

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Pizzas</Typography>
        <Button variant="contained" size="medium" onClick={openCreate}>
          New pizza
        </Button>
      </Stack>

      {loadError && <Alert severity="error" sx={{ mb: 2 }}>{loadError}</Alert>}
      {!loading && (
        <AdminDataTable
          columns={columns}
          rows={pizzas}
          rowKey={(row) => row.pizzaId}
          actions={(row) => (
            <Stack direction="row" spacing={1} sx={{ justifyContent: "flex-end" }}>
              <Button size="small" onClick={() => navigate(`/admin/pizzas/${row.pizzaId}/recipe`)}>
                Recipe
              </Button>
              <Button size="small" onClick={() => openEdit(row)}>
                Edit
              </Button>
            </Stack>
          )}
        />
      )}

      <AdminFormDialog
        open={dialogOpen}
        title={editing ? `Edit ${editing.code}` : "New pizza"}
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
          label="Description"
          value={form.description ?? ""}
          onChange={(e) => setForm({ ...form, description: e.target.value || null })}
          multiline
          minRows={2}
        />
        <TextField
          label="Base price"
          type="number"
          value={form.basePrice}
          onChange={(e) => setForm({ ...form, basePrice: Number(e.target.value) })}
          required
        />
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <Switch checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
          <Typography>Active</Typography>
        </Stack>
      </AdminFormDialog>
    </Box>
  );
}

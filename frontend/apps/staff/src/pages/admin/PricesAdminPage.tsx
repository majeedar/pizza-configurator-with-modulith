import { useCallback, useEffect, useState } from "react";
import { Alert, Box, Button, Chip, MenuItem, Stack, Switch, TextField, Typography } from "@mui/material";
import { useAuth } from "../../state/AuthContext";
import { ApiError } from "../../api/client";
import * as pricesAdmin from "../../api/pricesAdmin";
import { ITEM_TYPES, type PriceAdmin, type PriceAdminRequest } from "../../api/adminTypes";
import AdminDataTable, { type AdminColumn } from "../../components/admin/AdminDataTable";
import AdminFormDialog from "../../components/admin/AdminFormDialog";

const EMPTY_FORM: PriceAdminRequest = {
  itemType: "INGREDIENT",
  itemId: "",
  amount: 0,
  currency: "EUR",
  active: true,
  validFrom: null,
  validTo: null,
};

export default function PricesAdminPage() {
  const { token } = useAuth();
  const [prices, setPrices] = useState<PriceAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<PriceAdmin | null>(null);
  const [form, setForm] = useState<PriceAdminRequest>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setLoadError(null);
    try {
      setPrices(await pricesAdmin.listPrices(token));
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "Could not load prices.");
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

  function openEdit(price: PriceAdmin) {
    setEditing(price);
    setForm({
      itemType: price.itemType,
      itemId: price.itemId,
      amount: price.amount,
      currency: price.currency,
      active: price.active,
      validFrom: price.validFrom,
      validTo: price.validTo,
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
        await pricesAdmin.updatePrice(editing.priceId, form, token);
      } else {
        await pricesAdmin.createPrice(form, token);
      }
      setDialogOpen(false);
      await refresh();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Could not save price.");
    } finally {
      setSubmitting(false);
    }
  }

  const columns: AdminColumn<PriceAdmin>[] = [
    { key: "itemType", label: "Item type" },
    { key: "itemId", label: "Item code" },
    { key: "amount", label: "Amount", align: "right", render: (row) => `${row.amount.toFixed(2)} ${row.currency}` },
    {
      key: "active",
      label: "Status",
      render: (row) => <Chip size="small" label={row.active ? "Active" : "Inactive"} color={row.active ? "success" : "default"} />,
    },
  ];

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Prices</Typography>
        <Button variant="contained" size="medium" onClick={openCreate}>
          New price
        </Button>
      </Stack>

      {loadError && <Alert severity="error" sx={{ mb: 2 }}>{loadError}</Alert>}
      {!loading && (
        <AdminDataTable
          columns={columns}
          rows={prices}
          rowKey={(row) => row.priceId}
          actions={(row) => (
            <Button size="small" onClick={() => openEdit(row)}>
              Edit
            </Button>
          )}
        />
      )}

      <AdminFormDialog
        open={dialogOpen}
        title={editing ? `Edit price for ${editing.itemId}` : "New price"}
        submitting={submitting}
        error={formError}
        onSubmit={handleSubmit}
        onClose={() => setDialogOpen(false)}
      >
        <TextField
          select
          label="Item type"
          value={form.itemType}
          onChange={(e) => setForm({ ...form, itemType: e.target.value as PriceAdminRequest["itemType"] })}
          disabled={!!editing}
          required
        >
          {ITEM_TYPES.map((type) => (
            <MenuItem key={type} value={type}>
              {type}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          label="Item code"
          value={form.itemId}
          onChange={(e) => setForm({ ...form, itemId: e.target.value })}
          disabled={!!editing}
          helperText="The target's stable code — e.g. a pizza/ingredient/size/dough code, not a UUID."
          required
        />
        <TextField
          label="Amount"
          type="number"
          value={form.amount}
          onChange={(e) => setForm({ ...form, amount: Number(e.target.value) })}
          required
        />
        <TextField
          label="Currency"
          value={form.currency}
          onChange={(e) => setForm({ ...form, currency: e.target.value })}
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

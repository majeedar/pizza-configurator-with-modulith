import { useCallback, useEffect, useState } from "react";
import { Alert, Box, Button, Chip, Stack, Switch, TextField, Typography } from "@mui/material";
import { useAuth } from "../../state/AuthContext";
import { ApiError } from "../../api/client";
import * as catalogAdmin from "../../api/catalogAdmin";
import type { DoughAdmin, DoughAdminRequest } from "../../api/adminTypes";
import AdminDataTable, { type AdminColumn } from "../../components/admin/AdminDataTable";
import AdminFormDialog from "../../components/admin/AdminFormDialog";

const EMPTY_FORM: DoughAdminRequest = { code: "", displayName: "", priceModifier: 0, active: true };

export default function DoughsAdminPage() {
  const { token } = useAuth();
  const [doughs, setDoughs] = useState<DoughAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<DoughAdmin | null>(null);
  const [form, setForm] = useState<DoughAdminRequest>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setLoadError(null);
    try {
      setDoughs(await catalogAdmin.listDoughs(token));
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "Could not load doughs.");
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

  function openEdit(dough: DoughAdmin) {
    setEditing(dough);
    setForm({ code: dough.code, displayName: dough.displayName, priceModifier: dough.priceModifier, active: dough.active });
    setFormError(null);
    setDialogOpen(true);
  }

  async function handleSubmit() {
    if (!token) return;
    setSubmitting(true);
    setFormError(null);
    try {
      if (editing) {
        await catalogAdmin.updateDough(editing.doughId, form, token);
      } else {
        await catalogAdmin.createDough(form, token);
      }
      setDialogOpen(false);
      await refresh();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Could not save dough.");
    } finally {
      setSubmitting(false);
    }
  }

  const columns: AdminColumn<DoughAdmin>[] = [
    { key: "code", label: "Code" },
    { key: "displayName", label: "Display name" },
    { key: "priceModifier", label: "Price modifier", align: "right", render: (row) => row.priceModifier.toFixed(2) },
    {
      key: "active",
      label: "Status",
      render: (row) => <Chip size="small" label={row.active ? "Active" : "Inactive"} color={row.active ? "success" : "default"} />,
    },
  ];

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Doughs</Typography>
        <Button variant="contained" size="medium" onClick={openCreate}>
          New dough
        </Button>
      </Stack>

      {loadError && <Alert severity="error" sx={{ mb: 2 }}>{loadError}</Alert>}
      {!loading && (
        <AdminDataTable
          columns={columns}
          rows={doughs}
          rowKey={(row) => row.doughId}
          actions={(row) => (
            <Button size="small" onClick={() => openEdit(row)}>
              Edit
            </Button>
          )}
        />
      )}

      <AdminFormDialog
        open={dialogOpen}
        title={editing ? `Edit ${editing.code}` : "New dough"}
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
        <TextField
          label="Display name"
          value={form.displayName}
          onChange={(e) => setForm({ ...form, displayName: e.target.value })}
          required
        />
        <TextField
          label="Price modifier"
          type="number"
          value={form.priceModifier}
          onChange={(e) => setForm({ ...form, priceModifier: Number(e.target.value) })}
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

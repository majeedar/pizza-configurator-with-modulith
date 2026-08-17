import { useCallback, useEffect, useState } from "react";
import { Alert, Box, Button, Chip, MenuItem, Stack, TextField, Typography } from "@mui/material";
import { useAuth } from "../../state/AuthContext";
import { ApiError } from "../../api/client";
import * as staffAdmin from "../../api/staffAdmin";
import type { StaffAdmin, StaffCreateRequest } from "../../api/adminTypes";
import AdminDataTable, { type AdminColumn } from "../../components/admin/AdminDataTable";
import AdminFormDialog from "../../components/admin/AdminFormDialog";

const EMPTY_FORM: StaffCreateRequest = { username: "", displayName: "", email: null, password: "", role: "KITCHEN" };

/** Staff accounts are created/enabled/disabled here — there's no general edit (agent.md §8.3/§14.1: username/role are fixed at creation). */
export default function StaffAdminPage() {
  const { token, employeeId: currentEmployeeId } = useAuth();
  const [staff, setStaff] = useState<StaffAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<StaffCreateRequest>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setLoadError(null);
    try {
      setStaff(await staffAdmin.listStaff(token));
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "Could not load staff accounts.");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function openCreate() {
    setForm(EMPTY_FORM);
    setFormError(null);
    setDialogOpen(true);
  }

  async function handleSubmit() {
    if (!token) return;
    setSubmitting(true);
    setFormError(null);
    try {
      await staffAdmin.createStaff(form, token);
      setDialogOpen(false);
      await refresh();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Could not create the account.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggleEnabled(row: StaffAdmin) {
    if (!token) return;
    setTogglingId(row.employeeId);
    setLoadError(null);
    try {
      await staffAdmin.setStaffEnabled(row.employeeId, !row.enabled, token);
      await refresh();
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "Could not update the account.");
    } finally {
      setTogglingId(null);
    }
  }

  const columns: AdminColumn<StaffAdmin>[] = [
    { key: "username", label: "Username" },
    { key: "displayName", label: "Display name" },
    { key: "role", label: "Role" },
    {
      key: "enabled",
      label: "Status",
      render: (row) => <Chip size="small" label={row.enabled ? "Enabled" : "Disabled"} color={row.enabled ? "success" : "default"} />,
    },
  ];

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Staff users</Typography>
        <Button variant="contained" size="medium" onClick={openCreate}>
          New staff account
        </Button>
      </Stack>

      {loadError && <Alert severity="error" sx={{ mb: 2 }}>{loadError}</Alert>}
      {!loading && (
        <AdminDataTable
          columns={columns}
          rows={staff}
          rowKey={(row) => row.employeeId}
          actions={(row) => (
            <Button
              size="small"
              color={row.enabled ? "error" : "success"}
              disabled={togglingId === row.employeeId || row.employeeId === currentEmployeeId}
              onClick={() => handleToggleEnabled(row)}
              title={row.employeeId === currentEmployeeId ? "You can't disable your own account" : undefined}
            >
              {row.enabled ? "Disable" : "Enable"}
            </Button>
          )}
        />
      )}

      <AdminFormDialog
        open={dialogOpen}
        title="New staff account"
        submitting={submitting}
        error={formError}
        onSubmit={handleSubmit}
        onClose={() => setDialogOpen(false)}
      >
        <TextField
          label="Username"
          value={form.username}
          onChange={(e) => setForm({ ...form, username: e.target.value })}
          required
        />
        <TextField
          label="Display name"
          value={form.displayName}
          onChange={(e) => setForm({ ...form, displayName: e.target.value })}
          required
        />
        <TextField
          label="Email (optional)"
          value={form.email ?? ""}
          onChange={(e) => setForm({ ...form, email: e.target.value || null })}
        />
        <TextField
          label="Password"
          type="password"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
          required
        />
        <TextField
          select
          label="Role"
          value={form.role}
          onChange={(e) => setForm({ ...form, role: e.target.value as StaffCreateRequest["role"] })}
          required
        >
          <MenuItem value="KITCHEN">KITCHEN</MenuItem>
          <MenuItem value="ADMIN">ADMIN</MenuItem>
        </TextField>
      </AdminFormDialog>
    </Box>
  );
}

import { useCallback, useEffect, useState } from "react";
import { Alert, Box, Button, Chip, Stack, Switch, TextField, Typography } from "@mui/material";
import { useAuth } from "../../state/AuthContext";
import { apiBaseUrl, ApiError } from "../../api/client";
import * as appLinksAdmin from "../../api/appLinksAdmin";
import type { AppLinkAdmin, AppLinkUpdateRequest } from "../../api/adminTypes";
import AdminDataTable, { type AdminColumn } from "../../components/admin/AdminDataTable";
import AdminFormDialog from "../../components/admin/AdminFormDialog";

/** Edits the Android download URL/QR shown on the customer web footer and the KDS board (agent.md §7.10/§8.6) — no create/delete, just the two fixed (ANDROID, CUSTOMER|KITCHEN) rows. */
export default function AppLinksAdminPage() {
  const { token } = useAuth();
  const [links, setLinks] = useState<AppLinkAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<AppLinkAdmin | null>(null);
  const [form, setForm] = useState<AppLinkUpdateRequest>({ url: "", active: true });
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setLoadError(null);
    try {
      setLinks(await appLinksAdmin.listAppLinks(token));
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "Could not load app links.");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function openEdit(link: AppLinkAdmin) {
    setEditing(link);
    setForm({ url: link.url, active: link.active });
    setFormError(null);
    setDialogOpen(true);
  }

  async function handleSubmit() {
    if (!token || !editing) return;
    setSubmitting(true);
    setFormError(null);
    try {
      await appLinksAdmin.updateAppLink(editing.audience, form, token);
      setDialogOpen(false);
      await refresh();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Could not save the app link.");
    } finally {
      setSubmitting(false);
    }
  }

  const columns: AdminColumn<AppLinkAdmin>[] = [
    { key: "audience", label: "Audience" },
    { key: "platform", label: "Platform" },
    { key: "url", label: "URL", render: (row) => <span style={{ wordBreak: "break-all" }}>{row.url}</span> },
    {
      key: "active",
      label: "Status",
      render: (row) => <Chip size="small" label={row.active ? "Active" : "Inactive"} color={row.active ? "success" : "default"} />,
    },
    { key: "updatedBy", label: "Updated by", render: (row) => row.updatedBy ?? "—" },
  ];

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>
        App Links
      </Typography>

      {loadError && <Alert severity="error" sx={{ mb: 2 }}>{loadError}</Alert>}
      {!loading && (
        <AdminDataTable
          columns={columns}
          rows={links}
          rowKey={(row) => row.appLinkId}
          actions={(row) => (
            <Button size="small" onClick={() => openEdit(row)}>
              Edit
            </Button>
          )}
        />
      )}

      <AdminFormDialog
        open={dialogOpen}
        title={editing ? `Edit ${editing.audience} app link` : "Edit app link"}
        submitting={submitting}
        error={formError}
        onSubmit={handleSubmit}
        onClose={() => setDialogOpen(false)}
      >
        <TextField label="URL" value={form.url} onChange={(e) => setForm({ ...form, url: e.target.value })} required />
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <Switch checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
          <Typography>Active</Typography>
        </Stack>
        {editing && editing.active && (
          <Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Currently active QR (agent.md §8.3) — regenerated from the URL on every
              scan, so it updates automatically once you save a change here.
            </Typography>
            <img
              src={`${apiBaseUrl()}/api/v1/app-links/android/${editing.audience.toLowerCase()}/qr.png`}
              alt="Currently active QR code"
              width={120}
              height={120}
            />
          </Box>
        )}
      </AdminFormDialog>
    </Box>
  );
}

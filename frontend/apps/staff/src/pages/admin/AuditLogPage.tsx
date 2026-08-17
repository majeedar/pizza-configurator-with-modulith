import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Typography,
} from "@mui/material";
import { useAuth } from "../../state/AuthContext";
import { ApiError } from "../../api/client";
import * as auditAdmin from "../../api/auditAdmin";
import type { AuditEventAdmin } from "../../api/adminTypes";
import AdminDataTable, { type AdminColumn } from "../../components/admin/AdminDataTable";

function formatJson(raw: string | null): string {
  if (!raw) return "—";
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

/** Read-only (agent.md §7.10/§14.4/§30) — audit of admin rule/price/app-link changes only, see admin module README notes. */
export default function AuditLogPage() {
  const { token } = useAuth();
  const [events, setEvents] = useState<AuditEventAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [selected, setSelected] = useState<AuditEventAdmin | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setLoadError(null);
    try {
      const result = await auditAdmin.listAuditEvents(token);
      setEvents([...result].sort((a, b) => b.timestamp.localeCompare(a.timestamp)));
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "Could not load the audit log.");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const columns: AdminColumn<AuditEventAdmin>[] = [
    { key: "timestamp", label: "When", render: (row) => new Date(row.timestamp).toLocaleString() },
    { key: "actorId", label: "Actor" },
    { key: "actorRole", label: "Role" },
    { key: "action", label: "Action" },
    { key: "entityType", label: "Entity type" },
  ];

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Audit Log
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Admin rule/price/app-link changes only (agent.md §14.4) — catalog, staff, and order-status changes aren't audited here.
      </Typography>

      {loadError && <Alert severity="error" sx={{ mb: 2 }}>{loadError}</Alert>}
      {!loading && (
        <AdminDataTable
          columns={columns}
          rows={events}
          rowKey={(row) => row.eventId}
          onRowClick={(row) => setSelected(row)}
          emptyMessage="No audited changes yet."
        />
      )}

      <Dialog open={!!selected} onClose={() => setSelected(null)} maxWidth="md" fullWidth>
        <DialogTitle>
          {selected?.action} — {selected?.entityType} {selected?.entityId}
        </DialogTitle>
        <DialogContent>
          {selected && (
            <Stack spacing={2}>
              <Typography variant="body2" color="text.secondary">
                {new Date(selected.timestamp).toLocaleString()} by {selected.actorId} ({selected.actorRole}) —
                correlation {selected.correlationId}
              </Typography>
              <Box>
                <Typography variant="subtitle2">Before</Typography>
                <Box component="pre" sx={{ bgcolor: "grey.100", p: 1.5, borderRadius: 1, overflowX: "auto", fontSize: 13 }}>
                  {formatJson(selected.beforeJson)}
                </Box>
              </Box>
              <Box>
                <Typography variant="subtitle2">After</Typography>
                <Box component="pre" sx={{ bgcolor: "grey.100", p: 1.5, borderRadius: 1, overflowX: "auto", fontSize: 13 }}>
                  {formatJson(selected.afterJson)}
                </Box>
              </Box>
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelected(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

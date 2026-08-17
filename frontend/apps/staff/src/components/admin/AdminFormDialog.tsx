import type { ReactNode } from "react";
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack } from "@mui/material";

interface AdminFormDialogProps {
  open: boolean;
  title: string;
  submitLabel?: string;
  submitting: boolean;
  error: string | null;
  onSubmit: () => void;
  onClose: () => void;
  children: ReactNode;
}

/**
 * Dialog chrome only (title/error/submit-cancel/loading state) — every
 * Admin Portal create/edit form (agent.md §8.3) supplies its own field
 * markup as children, since the fields differ per entity but the
 * submit/error/loading contract doesn't.
 */
export default function AdminFormDialog({
  open,
  title,
  submitLabel = "Save",
  submitting,
  error,
  onSubmit,
  onClose,
  children,
}: AdminFormDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {children}
          {error && <Alert severity="error">{error}</Alert>}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} disabled={submitting} size="medium">
          Cancel
        </Button>
        <Button onClick={onSubmit} variant="contained" disabled={submitting} size="medium">
          {submitting ? "Saving…" : submitLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

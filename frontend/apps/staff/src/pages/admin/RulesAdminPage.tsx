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
import * as rulesAdmin from "../../api/rulesAdmin";
import { RULE_TYPES, type RuleAdmin, type RuleAdminRequest, type RuleType } from "../../api/adminTypes";
import AdminDataTable, { type AdminColumn } from "../../components/admin/AdminDataTable";
import AdminFormDialog from "../../components/admin/AdminFormDialog";

interface ParamField {
  key: string;
  label: string;
  type: "text" | "number" | "boolean";
}

// Mirrors each RuleEvaluator's nested `Params` record (rules/domain/evaluator/*.java).
// Every RuleType now has an evaluator (MIN_QUANTITY/OPTION_ALLOWED were the last
// two gaps, closed alongside this structured editor) — the raw-JSON textarea
// fallback below is now unreachable in practice, kept only as a safety net for
// a future RuleType added without updating this map in lockstep.
const PARAM_SPECS: Partial<Record<RuleType, ParamField[]>> = {
  MAX_QUANTITY: [
    { key: "ingredientCode", label: "Ingredient code", type: "text" },
    { key: "max", label: "Max quantity", type: "number" },
  ],
  MIN_QUANTITY: [
    { key: "ingredientCode", label: "Ingredient code", type: "text" },
    { key: "min", label: "Min quantity", type: "number" },
  ],
  OPTION_ALLOWED: [
    { key: "ingredientCode", label: "Ingredient code", type: "text" },
    { key: "allowed", label: "Allowed", type: "boolean" },
  ],
  REMOVAL_ALLOWED: [
    { key: "ingredientCode", label: "Ingredient code", type: "text" },
    { key: "removable", label: "Removable", type: "boolean" },
  ],
  REQUIRES: [
    { key: "ifIngredientCode", label: "If ingredient code", type: "text" },
    { key: "thenRequiresIngredientCode", label: "Then requires ingredient code", type: "text" },
  ],
  EXCLUDES: [
    { key: "ingredientA", label: "Ingredient A", type: "text" },
    { key: "ingredientB", label: "Ingredient B", type: "text" },
  ],
  SIZE_COMPATIBILITY: [
    { key: "sizeCode", label: "Size code", type: "text" },
    { key: "allowed", label: "Allowed", type: "boolean" },
  ],
  DOUGH_COMPATIBILITY: [
    { key: "doughCode", label: "Dough code", type: "text" },
    { key: "allowed", label: "Allowed", type: "boolean" },
  ],
  INGREDIENT_COMPATIBILITY: [
    { key: "doughCode", label: "Dough code", type: "text" },
    { key: "incompatibleIngredientCode", label: "Incompatible ingredient code", type: "text" },
  ],
};

const EMPTY_FORM: RuleAdminRequest = {
  ruleCode: "",
  ruleType: "MAX_QUANTITY",
  scopeType: "GLOBAL",
  scopeId: null,
  parameters: {},
  message: "",
  active: true,
  validFrom: null,
  validTo: null,
};

export default function RulesAdminPage() {
  const { token } = useAuth();
  const [rules, setRules] = useState<RuleAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<RuleAdmin | null>(null);
  const [form, setForm] = useState<RuleAdminRequest>(EMPTY_FORM);
  const [rawParametersText, setRawParametersText] = useState("{}");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setLoadError(null);
    try {
      setRules(await rulesAdmin.listRules(token));
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "Could not load rules.");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function openCreate() {
    setEditing(null);
    const parameters = defaultParameters(EMPTY_FORM.ruleType);
    setForm({ ...EMPTY_FORM, parameters });
    setRawParametersText(JSON.stringify(parameters, null, 2));
    setFormError(null);
    setDialogOpen(true);
  }

  function openEdit(rule: RuleAdmin) {
    setEditing(rule);
    setForm({
      ruleCode: rule.ruleCode,
      ruleType: rule.ruleType,
      scopeType: rule.scopeType,
      scopeId: rule.scopeId,
      parameters: rule.parameters,
      message: rule.message,
      active: rule.active,
      validFrom: rule.validFrom,
      validTo: rule.validTo,
    });
    setRawParametersText(JSON.stringify(rule.parameters, null, 2));
    setFormError(null);
    setDialogOpen(true);
  }

  function setParam(key: string, value: unknown) {
    setForm((prev) => ({ ...prev, parameters: { ...prev.parameters, [key]: value } }));
  }

  // Every field gets an explicit default the moment a rule type is chosen —
  // not left absent until the admin happens to touch it. Booleans in
  // particular must never be omitted: the backend's Params records declare
  // them as primitive `boolean`, and a real bug this caught was a saved rule
  // with no `allowed` key at all, which 500'd on the *next* evaluation
  // (evaluators run against every configuration, not just at save time) —
  // fixed at the source here, not just handled defensively on the backend.
  function defaultParameters(type: RuleType): Record<string, unknown> {
    const spec = PARAM_SPECS[type];
    if (!spec) return {};
    const defaults: Record<string, unknown> = {};
    for (const field of spec) {
      defaults[field.key] = field.type === "boolean" ? false : field.type === "number" ? 0 : "";
    }
    return defaults;
  }

  function handleRuleTypeChange(nextType: RuleType) {
    const parameters = defaultParameters(nextType);
    setForm((prev) => ({ ...prev, ruleType: nextType, parameters }));
    setRawParametersText(JSON.stringify(parameters, null, 2));
  }

  const paramSpec = PARAM_SPECS[form.ruleType];

  async function handleSubmit() {
    if (!token) return;
    setFormError(null);

    let parameters = form.parameters;
    if (!paramSpec) {
      try {
        parameters = JSON.parse(rawParametersText);
      } catch {
        setFormError("Parameters must be valid JSON.");
        return;
      }
    }

    setSubmitting(true);
    try {
      const request: RuleAdminRequest = { ...form, parameters };
      if (editing) {
        await rulesAdmin.updateRule(editing.ruleId, request, token);
      } else {
        await rulesAdmin.createRule(request, token);
      }
      setDialogOpen(false);
      await refresh();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Could not save rule.");
    } finally {
      setSubmitting(false);
    }
  }

  const columns: AdminColumn<RuleAdmin>[] = [
    { key: "ruleCode", label: "Code" },
    { key: "ruleType", label: "Type" },
    { key: "scopeType", label: "Scope", render: (row) => (row.scopeType === "PIZZA" ? `PIZZA (${row.scopeId})` : "GLOBAL") },
    { key: "message", label: "Message" },
    {
      key: "active",
      label: "Status",
      render: (row) => <Chip size="small" label={row.active ? "Active" : "Inactive"} color={row.active ? "success" : "default"} />,
    },
  ];

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h5">Rules</Typography>
        <Button variant="contained" size="medium" onClick={openCreate}>
          New rule
        </Button>
      </Stack>

      {loadError && <Alert severity="error" sx={{ mb: 2 }}>{loadError}</Alert>}
      {!loading && (
        <AdminDataTable
          columns={columns}
          rows={rules}
          rowKey={(row) => row.ruleId}
          actions={(row) => (
            <Button size="small" onClick={() => openEdit(row)}>
              Edit
            </Button>
          )}
        />
      )}

      <AdminFormDialog
        open={dialogOpen}
        title={editing ? `Edit ${editing.ruleCode}` : "New rule"}
        submitting={submitting}
        error={formError}
        onSubmit={handleSubmit}
        onClose={() => setDialogOpen(false)}
      >
        <TextField
          label="Rule code"
          value={form.ruleCode}
          onChange={(e) => setForm({ ...form, ruleCode: e.target.value })}
          disabled={!!editing}
          helperText={editing ? "Code cannot be changed after creation." : undefined}
          required
        />
        <TextField
          select
          label="Rule type"
          value={form.ruleType}
          onChange={(e) => handleRuleTypeChange(e.target.value as RuleType)}
          required
        >
          {RULE_TYPES.map((type) => (
            <MenuItem key={type} value={type}>
              {type}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          select
          label="Scope"
          value={form.scopeType}
          onChange={(e) => setForm({ ...form, scopeType: e.target.value as RuleAdminRequest["scopeType"] })}
          required
        >
          <MenuItem value="GLOBAL">GLOBAL — applies to every pizza</MenuItem>
          <MenuItem value="PIZZA">PIZZA — applies to one pizza only</MenuItem>
        </TextField>
        {form.scopeType === "PIZZA" && (
          <TextField
            label="Pizza code"
            value={form.scopeId ?? ""}
            onChange={(e) => setForm({ ...form, scopeId: e.target.value || null })}
            helperText="The pizza's stable code (e.g. NAPOLI), not a UUID."
            required
          />
        )}

        <Typography variant="subtitle2">Parameters</Typography>
        {paramSpec ? (
          paramSpec.map((field) =>
            field.type === "boolean" ? (
              <Stack key={field.key} direction="row" spacing={1} sx={{ alignItems: "center" }}>
                <Switch
                  checked={Boolean(form.parameters[field.key])}
                  onChange={(e) => setParam(field.key, e.target.checked)}
                />
                <Typography>{field.label}</Typography>
              </Stack>
            ) : (
              <TextField
                key={field.key}
                label={field.label}
                type={field.type === "number" ? "number" : "text"}
                value={(form.parameters[field.key] as string | number | undefined) ?? ""}
                onChange={(e) =>
                  setParam(field.key, field.type === "number" ? Number(e.target.value) : e.target.value)
                }
              />
            )
          )
        ) : (
          <TextField
            label="Parameters (raw JSON)"
            value={rawParametersText}
            onChange={(e) => setRawParametersText(e.target.value)}
            multiline
            minRows={3}
            helperText={`No structured editor for ${form.ruleType} yet — edit the JSON object directly.`}
          />
        )}

        <TextField
          label="Violation message"
          value={form.message}
          onChange={(e) => setForm({ ...form, message: e.target.value })}
          helperText="Shown to the customer when this rule is violated."
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

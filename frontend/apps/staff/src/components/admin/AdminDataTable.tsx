import type { ReactNode } from "react";
import {
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";

export interface AdminColumn<T> {
  key: string;
  label: string;
  align?: "left" | "right" | "center";
  render?: (row: T) => ReactNode;
}

interface AdminDataTableProps<T> {
  columns: AdminColumn<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  onRowClick?: (row: T) => void;
  actions?: (row: T) => ReactNode;
  emptyMessage?: string;
}

/**
 * The one generic list-rendering shared across the Admin Portal's ~7
 * near-identical catalog/rules/prices/staff CRUD screens (agent.md §8.3) —
 * field shapes differ per entity, so this only owns the table chrome
 * (headers, rows, empty state, an optional trailing actions column), never
 * field-level rendering or form logic.
 */
export default function AdminDataTable<T>({
  columns,
  rows,
  rowKey,
  onRowClick,
  actions,
  emptyMessage = "Nothing here yet.",
}: AdminDataTableProps<T>) {
  if (rows.length === 0) {
    return <Typography color="text.secondary">{emptyMessage}</Typography>;
  }

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table size="small">
        <TableHead>
          <TableRow>
            {columns.map((column) => (
              <TableCell key={column.key} align={column.align ?? "left"}>
                {column.label}
              </TableCell>
            ))}
            {actions && <TableCell align="right">Actions</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow
              key={rowKey(row)}
              hover={!!onRowClick}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              sx={onRowClick ? { cursor: "pointer" } : undefined}
            >
              {columns.map((column) => (
                <TableCell key={column.key} align={column.align ?? "left"}>
                  {column.render ? column.render(row) : String((row as Record<string, unknown>)[column.key] ?? "")}
                </TableCell>
              ))}
              {actions && (
                <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                  {actions(row)}
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

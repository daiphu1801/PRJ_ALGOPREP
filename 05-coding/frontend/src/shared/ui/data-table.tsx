// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Column-spec driven table. 12 of the 13 Admin/shared BD files describe a "bảng".
//
// Renders a real <table>, not a styled grid: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:170-202
// uses thead/tbody/th/td, and native table semantics give row/column announcement for free instead of
// through hand-written ARIA roles. Column widths ride on <colgroup>, so a header cell and its body
// cells cannot drift apart — that drift is the failure this component exists to prevent.
//
// Visual metrics carried over from that mockup: 13.5px body text, 11px uppercase header with 0.09em
// tracking and a bottom border, 9px/12px header padding, 12px cell padding, per-row bottom border,
// row hover on --color-row-hover.
//
// Per-instance loading/empty/error states, not screen-wide, per DEC-2026-0831-admin-overview-ui-decisions
// (Q8): one dead data source must not blank out blocks fed by another Bounded Context.
"use client";

import type { ReactNode } from "react";
import { cn } from "@/shared/lib";
import { EmptyState } from "./empty-state";
import { ErrorState } from "./error-state";
import { Skeleton } from "./skeleton";

const ALIGN = {
  left: "text-left",
  center: "text-center",
  right: "text-right",
} as const;

export type DataTableColumn<T> = {
  key: string;
  header: ReactNode;
  /** CSS width for this column's <col>, e.g. "116px", "28%", "auto". */
  width?: string;
  align?: keyof typeof ALIGN;
  /** Turns the header into a sort control. The screen does the actual sorting. */
  sortable?: boolean;
  render: (row: T) => ReactNode;
};

/**
 * Column sorting. Like selection, the screen owns the state and does the sorting — this component
 * only renders the control and the `aria-sort` announcement, so a server-side sort and a local one
 * look identical from here.
 */
export type DataTableSort = {
  key: string;
  direction: "asc" | "desc";
  onSortChange: (key: string, direction: "asc" | "desc") => void;
};

/**
 * Row selection. Opt-in: passing this adds a leading 34px checkbox column
 * (09-layoutBase/Admin - Người dùng.dc.html:206, :211). The screen owns the selected set — this
 * component never holds it, so bulk actions and the selection bar read one source of truth.
 *
 * Native <input type="checkbox"> rather than the mockup's styled <button>: it gets Space, the
 * indeterminate state and screen-reader semantics for free.
 */
type DataTableSelection<T> = {
  selectedKeys: ReadonlySet<string>;
  onToggleRow: (key: string) => void;
  /** Called with true to select every row currently rendered, false to clear. */
  onToggleAll: (selectAll: boolean) => void;
  selectAllLabel: string;
  /** Accessible label for one row's checkbox, e.g. `Chọn ${user.name}`. */
  rowLabel: (row: T) => string;
};

type DataTableProps<T> = {
  /** Accessible name — rendered as a visually hidden <caption>. */
  caption: string;
  columns: DataTableColumn<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  selection?: DataTableSelection<T>;
  sort?: DataTableSort;
  status?: "ready" | "loading" | "error";
  emptyMessage: ReactNode;
  errorMessage?: ReactNode;
  /** Below this the wrapper scrolls horizontally instead of crushing the columns. */
  minWidth?: number;
  /** Skeleton rows while loading. Match the page size so the layout does not jump. */
  loadingRowCount?: number;
  className?: string;
};

const CHECKBOX_CLASS =
  "h-[18px] w-[18px] cursor-pointer rounded-[5px] accent-[var(--color-admin-teal)]";

export function DataTable<T>({
  caption,
  columns,
  rows,
  rowKey,
  selection,
  sort,
  status = "ready",
  emptyMessage,
  errorMessage,
  minWidth = 720,
  loadingRowCount = 8,
  className,
}: DataTableProps<T>) {
  if (status === "error") {
    return <ErrorState>{errorMessage ?? emptyMessage}</ErrorState>;
  }

  const columnCount = columns.length + (selection ? 1 : 0);
  const allSelected =
    rows.length > 0 && rows.every((row) => selection?.selectedKeys.has(rowKey(row)));
  const someSelected =
    !allSelected && rows.some((row) => selection?.selectedKeys.has(rowKey(row)));

  return (
    <div className={cn("w-full overflow-x-auto", className)}>
      <table className="w-full text-[13.5px]" style={{ minWidth }}>
        <caption className="sr-only">{caption}</caption>
        <colgroup>
          {selection ? <col style={{ width: "34px" }} /> : null}
          {columns.map((column) => (
            <col key={column.key} style={column.width ? { width: column.width } : undefined} />
          ))}
        </colgroup>
        <thead>
          <tr>
            {selection ? (
              <th scope="col" className="border-b border-[var(--color-border)] px-3 py-2.5">
                <input
                  type="checkbox"
                  className={CHECKBOX_CLASS}
                  checked={allSelected}
                  ref={(node) => {
                    // Mixed selection has no HTML attribute — it is a DOM property only.
                    if (node) node.indeterminate = someSelected;
                  }}
                  onChange={(event) => selection.onToggleAll(event.target.checked)}
                  aria-label={selection.selectAllLabel}
                />
              </th>
            ) : null}
            {columns.map((column) => {
              const active = sort?.key === column.key;
              const headerClass = cn(
                "border-b border-[var(--color-border)] px-3 py-2.5 text-[11px] font-semibold tracking-[0.09em] text-[var(--color-text-subtle)] uppercase",
                ALIGN[column.align ?? "left"],
              );

              if (!column.sortable || !sort) {
                return (
                  <th key={column.key} scope="col" className={headerClass}>
                    {column.header}
                  </th>
                );
              }

              return (
                <th
                  key={column.key}
                  scope="col"
                  aria-sort={active ? (sort.direction === "asc" ? "ascending" : "descending") : "none"}
                  className={headerClass}
                >
                  <button
                    type="button"
                    onClick={() =>
                      sort.onSortChange(
                        column.key,
                        active && sort.direction === "asc" ? "desc" : "asc",
                      )
                    }
                    className={cn(
                      "inline-flex items-center gap-1 uppercase hover:text-[var(--color-text)]",
                      "focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)]",
                      active && "text-[var(--color-text)]",
                    )}
                  >
                    {column.header}
                    {/* Arrow, not a pictograph: a plain character carries the direction and stays
                        greppable, per .claude/rules/no-emoji.md. */}
                    <span aria-hidden="true">{active ? (sort.direction === "asc" ? "↑" : "↓") : ""}</span>
                  </button>
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {status === "loading" ? (
            Array.from({ length: loadingRowCount }, (_, index) => (
              <tr key={index}>
                <td colSpan={columnCount} className="px-3 py-2">
                  <Skeleton className="h-8" />
                </td>
              </tr>
            ))
          ) : rows.length === 0 ? (
            <tr>
              <td colSpan={columnCount}>
                <EmptyState>{emptyMessage}</EmptyState>
              </td>
            </tr>
          ) : (
            rows.map((row) => {
              const key = rowKey(row);
              const selected = selection?.selectedKeys.has(key) ?? false;
              return (
                <tr
                  key={key}
                  // Selected fill is an image token because the mockup's dark value is a gradient
                  // (dc.html:39 --active-bg), which background-color cannot carry.
                  className={cn(
                    "border-b border-[var(--color-border)] last:border-b-0 hover:bg-[var(--color-row-hover)]",
                    selected && "bg-[image:var(--color-row-selected)]",
                  )}
                >
                  {selection ? (
                    <td className="p-3">
                      <input
                        type="checkbox"
                        className={CHECKBOX_CLASS}
                        checked={selected}
                        onChange={() => selection.onToggleRow(key)}
                        aria-label={selection.rowLabel(row)}
                      />
                    </td>
                  ) : null}
                  {columns.map((column) => (
                    <td key={column.key} className={cn("p-3", ALIGN[column.align ?? "left"])}>
                      {column.render(row)}
                    </td>
                  ))}
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}

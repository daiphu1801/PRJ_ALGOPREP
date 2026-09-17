import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { DataTable, type DataTableColumn } from "./data-table";

type Row = { id: string; name: string; role: string };

const COLUMNS: DataTableColumn<Row>[] = [
  { key: "name", header: "Người dùng", render: (row) => row.name },
  { key: "role", header: "Vai trò", width: "116px", align: "right", render: (row) => row.role },
];

const ROWS: Row[] = [
  { id: "u1", name: "Nguyen Van A", role: "STUDENT" },
  { id: "u2", name: "Tran Thi B", role: "ADMIN" },
];

function renderTable(props: Partial<Parameters<typeof DataTable<Row>>[0]> = {}) {
  return render(
    <DataTable
      caption="Danh sách người dùng"
      columns={COLUMNS}
      rows={ROWS}
      rowKey={(row) => row.id}
      emptyMessage="Không có tài khoản nào khớp bộ lọc"
      {...props}
    />,
  );
}

describe("DataTable", () => {
  it("renders native table semantics with one column header per column", () => {
    renderTable();
    expect(screen.getByRole("table", { name: "Danh sách người dùng" })).toBeInTheDocument();
    expect(screen.getAllByRole("columnheader")).toHaveLength(COLUMNS.length);
    // Header row + one row per record.
    expect(screen.getAllByRole("row")).toHaveLength(ROWS.length + 1);
    expect(screen.getAllByRole("cell")).toHaveLength(COLUMNS.length * ROWS.length);
  });

  it("drives column width from <colgroup> so header and body cannot drift apart", () => {
    const { container } = renderTable();
    const cols = container.querySelectorAll("colgroup col");
    expect(cols).toHaveLength(COLUMNS.length);
    expect((cols[1] as HTMLElement).style.width).toBe("116px");
    // A column with no declared width must not get an inline one.
    expect((cols[0] as HTMLElement).style.width).toBe("");
  });

  it("shows the empty message instead of rows when there is no data", () => {
    renderTable({ rows: [] });
    expect(screen.getByText("Không có tài khoản nào khớp bộ lọc")).toBeInTheDocument();
    expect(screen.queryByText("Nguyen Van A")).not.toBeInTheDocument();
  });

  it("keeps the header while loading and renders no data rows", () => {
    renderTable({ status: "loading", loadingRowCount: 3 });
    expect(screen.getAllByRole("columnheader")).toHaveLength(COLUMNS.length);
    expect(screen.queryByText("Nguyen Van A")).not.toBeInTheDocument();
    expect(screen.getAllByRole("row")).toHaveLength(4);
  });

  it("replaces the table with an alert on error", () => {
    renderTable({ status: "error", errorMessage: "Không tải được danh sách" });
    expect(screen.getByRole("alert")).toHaveTextContent("Không tải được danh sách");
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  describe("row selection", () => {
    function renderSelectable(selectedKeys: string[]) {
      const onToggleRow = vi.fn();
      const onToggleAll = vi.fn();
      renderTable({
        selection: {
          selectedKeys: new Set(selectedKeys),
          onToggleRow,
          onToggleAll,
          selectAllLabel: "Chọn tất cả",
          rowLabel: (row) => `Chọn ${row.name}`,
        },
      });
      return { onToggleRow, onToggleAll };
    }

    it("adds one checkbox per row plus a select-all in the header", () => {
      renderSelectable([]);
      expect(screen.getByRole("checkbox", { name: "Chọn tất cả" })).toBeInTheDocument();
      expect(screen.getByRole("checkbox", { name: "Chọn Nguyen Van A" })).toBeInTheDocument();
      // Selection adds a column to every row, header included.
      expect(screen.getAllByRole("columnheader")).toHaveLength(COLUMNS.length + 1);
    });

    it("reports the row key, not the row object, when a row is toggled", () => {
      const { onToggleRow } = renderSelectable([]);
      fireEvent.click(screen.getByRole("checkbox", { name: "Chọn Tran Thi B" }));
      expect(onToggleRow).toHaveBeenCalledWith("u2");
    });

    it("marks select-all indeterminate on a partial selection", () => {
      renderSelectable(["u1"]);
      const selectAll = screen.getByRole("checkbox", { name: "Chọn tất cả" }) as HTMLInputElement;
      expect(selectAll.checked).toBe(false);
      expect(selectAll.indeterminate).toBe(true);
    });

    it("checks select-all only when every row is selected", () => {
      renderSelectable(["u1", "u2"]);
      const selectAll = screen.getByRole("checkbox", { name: "Chọn tất cả" }) as HTMLInputElement;
      expect(selectAll.checked).toBe(true);
      expect(selectAll.indeterminate).toBe(false);
    });
  });

  describe("column sorting", () => {
    it("leaves headers as plain text when no sort is passed", () => {
      renderTable();
      expect(screen.queryByRole("button")).not.toBeInTheDocument();
    });

    it("announces the sorted column and leaves non-sortable ones unannotated", () => {
      const onSortChange = vi.fn();
      render(
        <DataTable
          caption="Danh sách"
          columns={[
            { ...COLUMNS[0]!, sortable: true },
            { ...COLUMNS[1]!, sortable: true },
          ]}
          rows={ROWS}
          rowKey={(row) => row.id}
          emptyMessage="rỗng"
          sort={{ key: "role", direction: "asc", onSortChange }}
        />,
      );
      const [name, role] = screen.getAllByRole("columnheader");
      expect(role).toHaveAttribute("aria-sort", "ascending");
      // Sortable but not active: "none" says "you can sort by this, it is not sorted now".
      expect(name).toHaveAttribute("aria-sort", "none");
    });

    it("gives a non-sortable column no aria-sort at all", () => {
      renderTable({
        columns: [COLUMNS[0]!, { ...COLUMNS[1]!, sortable: true }],
        sort: { key: "role", direction: "asc", onSortChange: vi.fn() },
      });
      // Claiming "none" on a column nobody can sort would promise a control that is not there.
      expect(screen.getAllByRole("columnheader")[0]).not.toHaveAttribute("aria-sort");
    });

    it("flips direction on the active column and starts ascending on another", () => {
      const onSortChange = vi.fn();
      render(
        <DataTable
          caption="Danh sách"
          columns={[
            { ...COLUMNS[0]!, sortable: true },
            { ...COLUMNS[1]!, sortable: true },
          ]}
          rows={ROWS}
          rowKey={(row) => row.id}
          emptyMessage="rỗng"
          sort={{ key: "role", direction: "asc", onSortChange }}
        />,
      );

      fireEvent.click(screen.getByRole("button", { name: /Vai trò/ }));
      expect(onSortChange).toHaveBeenCalledWith("role", "desc");

      fireEvent.click(screen.getByRole("button", { name: /Người dùng/ }));
      expect(onSortChange).toHaveBeenCalledWith("name", "asc");
    });
  });
});

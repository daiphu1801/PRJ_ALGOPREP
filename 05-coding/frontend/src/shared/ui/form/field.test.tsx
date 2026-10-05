import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { Pagination } from "../data/pagination";
import { SelectField } from "./select-field";
import { TextField } from "./text-field";

describe("TextField", () => {
  it("links its label to the input and flags an invalid field without drawing any text", () => {
    render(<TextField label="Tìm kiếm" invalid />);
    const input = screen.getByLabelText("Tìm kiếm");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("keeps the label for screen readers when hidden visually", () => {
    render(<TextField label="Tìm kiếm" hideLabel placeholder="Tìm người dùng…" />);
    expect(screen.getByLabelText("Tìm kiếm")).toBeInTheDocument();
  });
});

describe("SelectField", () => {
  it("renders one option per entry and links its label", () => {
    render(
      <SelectField
        label="Vai trò"
        options={[
          { value: "all", label: "Tất cả" },
          { value: "admin", label: "Quản trị viên" },
        ]}
      />,
    );
    expect(screen.getByLabelText("Vai trò")).toBeInTheDocument();
    expect(screen.getAllByRole("option")).toHaveLength(2);
  });
});

describe("Pagination", () => {
  it("disables the previous control on the first page", () => {
    render(
      <Pagination
        page={1}
        pageSize={20}
        total={143}
        onPageChange={() => {}}
        summary="1-20 trên 143"
        previousLabel="Trước"
        nextLabel="Sau"
      />,
    );
    expect(screen.getByRole("button", { name: "Trước" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Sau" })).toBeEnabled();
  });

  it("disables the next control on the last page", () => {
    render(
      <Pagination
        page={8}
        pageSize={20}
        total={143}
        onPageChange={() => {}}
        summary="141-143 trên 143"
        previousLabel="Trước"
        nextLabel="Sau"
      />,
    );
    expect(screen.getByRole("button", { name: "Sau" })).toBeDisabled();
  });

  it("offers a rows-per-page pop-up only when given the options and a handler", () => {
    const onPageSizeChange = vi.fn();
    render(
      <Pagination
        page={1}
        pageSize={20}
        total={143}
        onPageChange={() => {}}
        summary="1-20 trên 143"
        previousLabel="Trước"
        nextLabel="Sau"
        pageSizeOptions={[8, 20, 50]}
        onPageSizeChange={onPageSizeChange}
        pageSizeLabel="Số dòng mỗi trang"
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: /^Số dòng mỗi trang/ }));
    fireEvent.click(screen.getByRole("option", { name: "50" }));
    expect(onPageSizeChange).toHaveBeenCalledWith(50);
  });
});

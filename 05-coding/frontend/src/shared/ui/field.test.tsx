import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Pagination } from "./pagination";
import { SelectField } from "./select-field";
import { TextField } from "./text-field";

describe("TextField", () => {
  it("links its label to the input and exposes the error to assistive tech", () => {
    render(<TextField label="Tìm kiếm" error="Tối thiểu 2 ký tự" />);
    const input = screen.getByLabelText("Tìm kiếm");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByRole("alert")).toHaveTextContent("Tối thiểu 2 ký tự");
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
});

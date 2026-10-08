// Cell toggling goes through two guards (BD ADM0202 Sheet 9 NO 8-9), so it is tested through the
// screen: what the admin sees is a blocked cell, a confirmation, or an immediate change.
import { describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
} from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { AdminPermissionMatrixView } from "./admin-permission-matrix-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, values?: Record<string, string | number>) =>
    values ? `${key}:${Object.values(values).join(",")}` : key,
}));

function cell(action: string, fn: string, role: string) {
  return screen.getByLabelText(
    `cellLabel:action.${action},function.${fn},${role}`,
  );
}

function openRole(role: string) {
  fireEvent.click(screen.getByRole("button", { name: role }));
}

describe("AdminPermissionMatrixView", () => {
  it("has no save button: cells apply immediately", () => {
    render(<AdminPermissionMatrixView />);
    expect(screen.queryByRole("button", { name: "save" })).toBeNull();

    expect(cell("create", "USER_MANAGEMENT", "INSTRUCTOR")).not.toBeChecked();
    fireEvent.click(cell("create", "USER_MANAGEMENT", "INSTRUCTOR"));
    expect(cell("create", "USER_MANAGEMENT", "INSTRUCTOR")).toBeChecked();
  });

  it("refuses to remove the last ADMIN PERMISSION_MATRIX:UPDATE", () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(<AdminPermissionMatrixView />);
    openRole("ADMIN");

    fireEvent.click(cell("update", "PERMISSION_MATRIX", "ADMIN"));

    expect(toasts.result.current.at(-1)?.message).toBe("lastMatrixAdminBody");
    expect(cell("update", "PERMISSION_MATRIX", "ADMIN")).toBeChecked();
  });

  it("asks before the acting admin removes a right from their own role", () => {
    render(<AdminPermissionMatrixView />);
    openRole("ADMIN");

    fireEvent.click(cell("read", "AI_CONFIG", "ADMIN"));
    // Nothing changes until the confirmation is accepted.
    expect(cell("read", "AI_CONFIG", "ADMIN")).toBeChecked();
    expect(screen.getByRole("dialog")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "selfRevokeConfirm" }));
    expect(cell("read", "AI_CONFIG", "ADMIN")).not.toBeChecked();
  });

  it("rejects an over-long role name", () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(<AdminPermissionMatrixView />);
    fireEvent.click(screen.getByRole("button", { name: "addRole" }));
    fireEvent.change(screen.getByLabelText("newRoleLabel"), {
      target: { value: "x".repeat(51) },
    });
    fireEvent.click(screen.getByRole("button", { name: "createRole" }));
    expect(toasts.result.current.at(-1)?.message).toBe(
      "roleNameError.tooLong:50",
    );
  });

  it("creates a custom role in the category picked on the form, STUDENT by default", () => {
    render(<AdminPermissionMatrixView />);
    fireEvent.click(screen.getByRole("button", { name: "addRole" }));
    expect(screen.getByLabelText("newRoleCategoryLabel")).toHaveValue(
      "STUDENT",
    );

    fireEvent.change(screen.getByLabelText("newRoleLabel"), {
      target: { value: "Ops" },
    });
    fireEvent.change(screen.getByLabelText("newRoleCategoryLabel"), {
      target: { value: "ADMIN" },
    });
    fireEvent.click(screen.getByRole("button", { name: "createRole" }));

    // A new ADMIN-category role holds nothing yet, so it is a second admin role but not a second
    // matrix holder: removing the ADMIN role's own PERMISSION_MATRIX:UPDATE is still blocked.
    openRole("ADMIN");
    fireEvent.click(cell("update", "PERMISSION_MATRIX", "ADMIN"));
    expect(cell("update", "PERMISSION_MATRIX", "ADMIN")).toBeChecked();
  });

  it("blocks deleting a custom role that accounts still hold", () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(<AdminPermissionMatrixView />);
    openRole("Trợ giảng");
    fireEvent.click(screen.getByRole("button", { name: "deleteRole" }));
    expect(toasts.result.current.at(-1)?.message).toBe(
      "deleteBlockedBody:Trợ giảng,3",
    );
    expect(screen.queryByRole("dialog")).toBeNull();
  });
});

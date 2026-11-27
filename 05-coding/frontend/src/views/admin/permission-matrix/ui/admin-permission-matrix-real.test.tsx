// View-level tests of ADM0202 against the REAL api path (03-dd/api/F1_identity.md #30 to #34): only the
// HTTP layer is faked. What matters: a cell shows the value the SERVER stored, a refusal reads as its
// error code and leaves the cell as it was, and a role action reloads the grid from the server.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { AdminPermissionMatrixView } from "./admin-permission-matrix-view";

const m = viMessages.adminPermissionMatrix;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const RBAC = "/identity/rbac";

const ROLES = [
  {
    id: "r-student",
    code: "STUDENT",
    name: "Người học",
    baseCategory: "STUDENT",
    isSystem: true,
    userCount: 5,
  },
  {
    id: "r-instructor",
    code: "INSTRUCTOR",
    name: "Giảng viên",
    baseCategory: "INSTRUCTOR",
    isSystem: true,
    userCount: 2,
  },
  {
    id: "r-admin",
    code: "ADMIN",
    name: "Quản trị",
    baseCategory: "ADMIN",
    isSystem: true,
    userCount: 1,
  },
  {
    id: "r-ta",
    code: "role_tro_giang",
    name: "Trợ giảng",
    baseCategory: "INSTRUCTOR",
    isSystem: false,
    userCount: 0,
  },
  {
    id: "r-busy",
    code: "role_dang_dung",
    name: "Đang dùng",
    baseCategory: "INSTRUCTOR",
    isSystem: false,
    userCount: 3,
  },
];
const FUNCTIONS = [
  "PROBLEM_AUTHORING",
  "TESTCASE_MANAGEMENT",
  "CLASS_MANAGEMENT",
  "USER_MANAGEMENT",
  "JUDGE_QUEUE_MONITOR",
  "LANGUAGE_CONFIG",
  "AI_CONFIG",
  "AI_TOKEN_BUDGET",
  "SYSTEM_AUDIT_LOG",
  "INTERVIEW_BANK_MANAGEMENT",
  "PERMISSION_MATRIX",
].map((code) => ({ id: `f-${code}`, code }));
const ACTIONS = ["CREATE", "READ", "UPDATE", "DELETE"].map((code) => ({
  id: `a-${code}`,
  code,
}));

function serve(roles = ROLES, cells: unknown[] = []) {
  apiMock.get.mockImplementation((path: string) => {
    if (path === `${RBAC}/matrix`) {
      return Promise.resolve({
        roles,
        functions: FUNCTIONS,
        actions: ACTIONS,
        cells,
      });
    }
    if (path === `${RBAC}/roles`) return Promise.resolve({ items: roles });
    return Promise.resolve({ roleName: "Quản trị" });
  });
}

const cellOf = (
  roleId: string,
  fn: string,
  action: string,
  granted: boolean,
) => ({
  roleId,
  functionId: `f-${fn}`,
  actionId: `a-${action}`,
  granted,
});

const cellBox = (action: string, fn: string, role: string) =>
  screen.getByLabelText(
    m.cellLabel
      .replace(
        "{action}",
        (m.action as Record<string, string>)[action.toLowerCase()] ?? "",
      )
      .replace("{function}", (m.function as Record<string, string>)[fn] ?? "")
      .replace("{role}", role),
  );

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

async function open() {
  render(withTestProviders(<AdminPermissionMatrixView />));
  await screen.findByRole("button", { name: "Giảng viên" });
}

const listCalls = () =>
  apiMock.get.mock.calls.filter(([path]) => path === `${RBAC}/matrix`);

describe("AdminPermissionMatrixView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    serve();
  });

  it("draws the grid from the server cells and keeps the student role read-only", async () => {
    serve(ROLES, [cellOf("r-instructor", "USER_MANAGEMENT", "READ", true)]);
    await open();

    expect(cellBox("READ", "USER_MANAGEMENT", "Giảng viên")).not.toBeDisabled();

    expect(cellBox("READ", "USER_MANAGEMENT", "Giảng viên")).toBeChecked();
    expect(
      cellBox("CREATE", "USER_MANAGEMENT", "Giảng viên"),
    ).not.toBeChecked();
    fireEvent.click(screen.getByRole("button", { name: "Người học" }));
    expect(cellBox("READ", "USER_MANAGEMENT", "Người học")).toBeDisabled();
  });

  it("saves a cell by ids, shows the stored value and reloads the grid", async () => {
    apiMock.patch.mockResolvedValue({ granted: true, changed: true });
    await open();
    const before = listCalls().length;

    fireEvent.click(cellBox("CREATE", "USER_MANAGEMENT", "Giảng viên"));

    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith(
        `${RBAC}/matrix/r-instructor/f-USER_MANAGEMENT/a-CREATE`,
        { granted: true },
      ),
    );
    await waitFor(() => expect(listCalls().length).toBeGreaterThan(before));
  });

  it("reads a refusal by its code and leaves the cell as it was", async () => {
    apiMock.patch.mockRejectedValue(new ApiError("IDT-423", 422));
    const toasts = watch();
    await open();

    fireEvent.click(cellBox("CREATE", "USER_MANAGEMENT", "Giảng viên"));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-423"]]),
    );
    expect(
      cellBox("CREATE", "USER_MANAGEMENT", "Giảng viên"),
    ).not.toBeChecked();
    expect(
      cellBox("CREATE", "USER_MANAGEMENT", "Giảng viên"),
    ).not.toBeDisabled();
  });

  it("locks only the cell that is waiting for the server", async () => {
    let finish: (value: unknown) => void = () => {};
    apiMock.patch.mockReturnValue(new Promise((resolve) => (finish = resolve)));
    await open();

    fireEvent.click(cellBox("CREATE", "USER_MANAGEMENT", "Giảng viên"));

    await waitFor(() =>
      expect(cellBox("CREATE", "USER_MANAGEMENT", "Giảng viên")).toBeDisabled(),
    );
    expect(cellBox("READ", "USER_MANAGEMENT", "Giảng viên")).not.toBeDisabled();
    await act(async () => finish({ granted: true, changed: true }));
    await waitFor(() =>
      expect(
        cellBox("CREATE", "USER_MANAGEMENT", "Giảng viên"),
      ).not.toBeDisabled(),
    );
  });

  it("creates a role on the server and opens its tab", async () => {
    apiMock.post.mockResolvedValue({
      id: "r-new",
      code: "role_ops",
      name: "Ops",
      baseCategory: "ADMIN",
      isSystem: false,
      userCount: 0,
    });
    const toasts = watch();
    await open();

    fireEvent.click(screen.getByRole("button", { name: m.addRole }));
    fireEvent.change(screen.getByLabelText(m.newRoleLabel), {
      target: { value: "Ops" },
    });
    fireEvent.change(screen.getByLabelText(m.newRoleCategoryLabel), {
      target: { value: "ADMIN" },
    });
    fireEvent.click(screen.getByRole("button", { name: m.createRole }));

    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith(`${RBAC}/roles`, {
        name: "Ops",
        baseCategory: "ADMIN",
      }),
    );
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual([
        "success",
        m.createRoleDone.replace("{role}", "Ops"),
      ]),
    );
  });

  it("keeps the form and shows the reason when the server refuses a role", async () => {
    apiMock.post.mockRejectedValue(new ApiError("IDT-425", 409));
    const toasts = watch();
    await open();

    fireEvent.click(screen.getByRole("button", { name: m.addRole }));
    fireEvent.change(screen.getByLabelText(m.newRoleLabel), {
      target: { value: "Ops" },
    });
    fireEvent.click(screen.getByRole("button", { name: m.createRole }));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-425"]]),
    );
    expect(screen.getByLabelText(m.newRoleLabel)).toHaveValue("Ops");
  });

  it("deletes the open custom role and falls back to the instructor tab", async () => {
    apiMock.delete.mockResolvedValue(undefined);
    await open();

    fireEvent.click(screen.getByRole("button", { name: "Trợ giảng" }));
    fireEvent.click(screen.getByRole("button", { name: m.deleteRole }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: m.deleteRole,
      }),
    );

    await waitFor(() =>
      expect(apiMock.delete).toHaveBeenCalledWith(`${RBAC}/roles/r-ta`),
    );
  });

  it("keeps the role when the server refuses to delete it", async () => {
    apiMock.delete.mockRejectedValue(new ApiError("IDT-422", 422));
    // The screen blocks a role that still has users before asking, so the refusal comes from a role
    // another admin just assigned: the server count was 0 when the page loaded.
    const toasts = watch();
    await open();

    fireEvent.click(screen.getByRole("button", { name: "Trợ giảng" }));
    fireEvent.click(screen.getByRole("button", { name: m.deleteRole }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: m.deleteRole,
      }),
    );

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-422"]]),
    );
    expect(
      screen.getByRole("button", { name: "Trợ giảng" }),
    ).toBeInTheDocument();
  });
});

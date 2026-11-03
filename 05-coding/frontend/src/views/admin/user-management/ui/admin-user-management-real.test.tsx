// View-level tests of ADM0201 against the REAL api path (03-dd/api/F1_identity.md #35 to #38, #50, #62,
// #63): only the HTTP layer is faked, so the real modules, the error-code mapping and the query
// refresh run. What matters here is what the admin reads when the server refuses, and that a
// successful action brings the list back from the server.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
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
vi.mock("next/link", () => ({
  default: ({
    href,
    children,
  }: {
    href: string;
    children: React.ReactNode;
  }) => <a href={href}>{children}</a>,
}));

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { AdminUserManagementView } from "./admin-user-management-view";

const m = viMessages.adminUserManagement;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const USERS = "/identity/admin/users";

const dto = (
  id: string,
  displayName: string,
  baseCategory: string,
  status = "ACTIVE",
) => ({
  id,
  displayName,
  email: `${id}@x.test`,
  baseCategory,
  status,
  lastActiveAt: id === "an" ? "2026-01-05T03:00:00Z" : null,
  hasPassword: true,
});

const ROWS = [
  dto("an", "An Hoc", "STUDENT"),
  dto("binh", "Binh Day", "INSTRUCTOR"),
  dto("chi", "Chi Khoa", "STUDENT", "DEACTIVATED"),
  dto("admin1", "Quan Tri Mot", "ADMIN"),
  dto("admin2", "Quan Tri Hai", "ADMIN"),
];

function serve(rows = ROWS) {
  apiMock.get.mockImplementation((path: string) => {
    if (path === USERS) {
      return Promise.resolve({
        items: rows,
        page: 1,
        pageSize: 20,
        total: 45,
      });
    }
    if (path.endsWith("management-stats")) {
      return Promise.resolve({ pendingVerificationCount: 3 });
    }
    if (path.endsWith("role-distribution")) {
      return Promise.resolve({
        items: [
          { groupCode: "STUDENT", count: 30, percent: 66.7 },
          { groupCode: "INSTRUCTOR", count: 10, percent: 22.2 },
          { groupCode: "ADMIN", count: 2, percent: 4.4 },
          { groupCode: "DEACTIVATED", count: 3, percent: 6.7 },
        ],
      });
    }
    if (path === "/identity/rbac/roles") {
      return Promise.resolve({
        items: [
          { id: "r-student", code: "STUDENT", isSystem: true },
          { id: "r-instructor", code: "INSTRUCTOR", isSystem: true },
          { id: "r-admin", code: "ADMIN", isSystem: true },
        ],
      });
    }
    return Promise.resolve({ email: "admin1@x.test" });
  });
}

function listCalls() {
  return apiMock.get.mock.calls.filter(([path]) => path === USERS);
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

async function open() {
  render(withTestProviders(<AdminUserManagementView />));
  await screen.findByText("An Hoc");
}

const rowAction = (template: string, name: string) =>
  screen.getByRole("button", { name: template.replace("{name}", name) });
const button = (name: string) => screen.getByRole("button", { name });

describe("AdminUserManagementView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    serve();
  });

  it("shows the server rows and the last activity, and hides the counts the backend cannot fill", async () => {
    await open();

    expect(screen.getByText("Binh Day")).toBeInTheDocument();
    expect(screen.getByText("admin1@x.test")).toBeInTheDocument();
    expect(
      screen.getByRole("columnheader", { name: m.columnStatus }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("columnheader", { name: m.columnSolved }),
    ).toBeNull();
    expect(
      screen.queryByRole("columnheader", { name: m.columnSubmissions }),
    ).toBeNull();
    expect(
      screen.getByRole("columnheader", { name: m.columnLastActive }),
    ).toBeInTheDocument();
    // Older than thirty days: the date in UTC+7; an account that never logged in shows a dash.
    expect(screen.getByText("05/01/2026")).toBeInTheDocument();
    expect(listCalls()[0]?.[1]).toEqual({
      status: undefined,
      role: undefined,
      q: undefined,
      page: 1,
      pageSize: 20,
    });
  });

  it("asks the server for the next page", async () => {
    await open();

    fireEvent.click(button(m.next));

    await waitFor(() =>
      expect(listCalls().at(-1)?.[1]).toMatchObject({ page: 2 }),
    );
  });

  it("locks an account, then reloads the list from the server", async () => {
    apiMock.patch.mockResolvedValue({});
    const toasts = watch();
    await open();
    const before = listCalls().length;

    fireEvent.click(rowAction(m.actionLockFor, "An Hoc"));
    fireEvent.change(screen.getByLabelText(m.lockDialog.reasonLabel), {
      target: { value: "Spam" },
    });
    fireEvent.click(button(m.lockDialog.submit));

    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith(`${USERS}/an/status`, {
        targetStatus: "DEACTIVATED",
        reason: "Spam",
        notifyByEmail: true,
      }),
    );
    await waitFor(() => expect(listCalls().length).toBeGreaterThan(before));
    expect(toasts().at(-1)?.[0]).toBe("success");
  });

  it("shows the server reason when a lock is refused and keeps the row active", async () => {
    apiMock.patch.mockRejectedValue(new ApiError("IDT-432", 422));
    const toasts = watch();
    await open();

    fireEvent.click(rowAction(m.actionLockFor, "Quan Tri Mot"));
    fireEvent.change(screen.getByLabelText(m.lockDialog.reasonLabel), {
      target: { value: "Test" },
    });
    // Locking yourself needs the explicit confirmation the dialog already asks for.
    fireEvent.click(button(m.lockDialog.submit));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual([
        "error",
        m.batchFailed
          .replace("{count}", "1")
          .replace("{reason}", apiErrors["IDT-432"] ?? ""),
      ]),
    );
    expect(apiMock.patch).toHaveBeenCalledWith(
      `${USERS}/admin1/status`,
      expect.objectContaining({ acknowledgeSelfImpact: true }),
    );
  });

  it("unlocks a locked account and refuses an anonymised one with its own message", async () => {
    apiMock.patch.mockRejectedValue(new ApiError("IDT-448", 422));
    const toasts = watch();
    await open();

    fireEvent.click(rowAction(m.actionUnlockFor, "Chi Khoa"));
    fireEvent.click(button(m.confirmUnlockAction));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual([
        "error",
        m.batchFailed
          .replace("{count}", "1")
          .replace("{reason}", apiErrors["IDT-448"] ?? ""),
      ]),
    );
    expect(apiMock.patch).toHaveBeenCalledWith(`${USERS}/chi/status`, {
      targetStatus: "ACTIVE",
    });
  });

  it("changes a role with the built-in role id and reports a refusal by code", async () => {
    apiMock.patch.mockRejectedValue(new ApiError("IDT-424", 404));
    const toasts = watch();
    await open();

    fireEvent.click(rowAction(m.actionChangeRoleFor, "An Hoc"));
    fireEvent.change(screen.getByLabelText(m.roleDialog.label), {
      target: { value: "instructor" },
    });
    fireEvent.click(button(m.roleDialog.submit));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-424"]]),
    );
    expect(apiMock.patch).toHaveBeenCalledWith(`${USERS}/an/role`, {
      targetRoleId: "r-instructor",
    });
  });

  it("resets a password and tells the admin when the account is gone", async () => {
    apiMock.post.mockRejectedValue(new ApiError("IDT-450", 404));
    const toasts = watch();
    await open();

    fireEvent.click(rowAction(m.actionResetPasswordFor, "An Hoc"));
    fireEvent.click(button(m.resetAction));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-450"]]),
    );
    expect(apiMock.post).toHaveBeenCalledWith(`${USERS}/an/reset-password`);
  });

  it("keeps the add-account dialog open with its input when the e-mail is taken", async () => {
    apiMock.post.mockRejectedValue(new ApiError("IDT-440", 409));
    const toasts = watch();
    await open();

    fireEvent.click(button(m.addAccount));
    fireEvent.change(screen.getByLabelText(m.addDialog.nameLabel), {
      target: { value: "Nguoi Moi" },
    });
    fireEvent.change(screen.getByLabelText(m.addDialog.emailLabel), {
      target: { value: "moi@x.test" },
    });
    fireEvent.click(button(m.addDialog.submit));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-440"]]),
    );
    expect(screen.getByLabelText(m.addDialog.nameLabel)).toHaveValue(
      "Nguoi Moi",
    );
    expect(apiMock.post).toHaveBeenCalledWith(USERS, {
      displayName: "Nguoi Moi",
      email: "moi@x.test",
      roleId: "r-student",
    });
  });
});

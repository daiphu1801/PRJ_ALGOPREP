// The two surviving bulk actions, tested through the screen because what matters is which subset of
// the selection each button acts on and what it says before it destroys access.
import { describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import type { AdminUserPage } from "../model/types";
import { AdminUserManagementView } from "./admin-user-management-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, values?: Record<string, string | number>) =>
    values ? `${key}:${Object.values(values).join(",")}` : key,
}));

// The shipped mock carries a single admin, which is the acting account — so locking it trips the
// "last active ADMIN" hard block and the dialog never opens. A second admin is added so the
// self-warning path is reachable on its own.
vi.mock("../api", async () => {
  const api = (await vi.importActual<object>("../api")) as {
    fetchAdminUserPage: () => AdminUserPage;
  };
  return {
    ...api,
    fetchAdminUserPage: () => {
      const page = api.fetchAdminUserPage();
      return {
        ...page,
        users: [
          ...page.users,
          {
            name: "Lê Thị Mai",
            email: "ltmai@algoprep.vn",
            role: "admin",
            solvedCount: 0,
            submissionCount: 0,
            lastActiveLabel: "-",
            status: "active",
          },
        ],
      };
    },
  };
});

/** The acting admin in the mock, and the one locked row (admin-user-mocks.ts:16,19). */
const ACTING_ADMIN = "Đặng Phú Đại";
const LOCKED_ROW = "Hoàng Minh Trí";
const ACTIVE_ROW = "Nguyễn Văn An";

/** Row checkboxes are labelled by name, not email (rowLabel in the selection props). */
function tickRow(name: string) {
  fireEvent.click(screen.getByLabelText(`selectRow:${name}`));
}

function clickButton(name: string) {
  fireEvent.click(screen.getByRole("button", { name }));
}

function typeReason(text: string) {
  fireEvent.change(screen.getByLabelText("lockDialog.reasonLabel"), {
    target: { value: text },
  });
}

describe("AdminUserManagementView bulk status actions", () => {
  it("offers only the status actions on the selection bar", () => {
    render(<AdminUserManagementView />);
    tickRow(ACTIVE_ROW);
    expect(
      screen.getByRole("button", { name: "bulkLock" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "bulkUnlock" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "bulkResetPassword" }),
    ).toBeNull();
    expect(screen.queryByRole("button", { name: "bulkChangeRole" })).toBeNull();
  });

  it("disables unlock while the selection holds nothing locked", () => {
    render(<AdminUserManagementView />);
    tickRow(ACTIVE_ROW);
    expect(screen.getByRole("button", { name: "bulkLock" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "bulkUnlock" })).toBeDisabled();
  });

  it("disables lock while the selection holds only locked rows", () => {
    render(<AdminUserManagementView />);
    tickRow(LOCKED_ROW);
    expect(screen.getByRole("button", { name: "bulkLock" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "bulkUnlock" })).toBeEnabled();
  });

  it("names the affected accounts and refuses to lock without a reason", () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(<AdminUserManagementView />);
    tickRow(ACTIVE_ROW);
    clickButton("bulkLock");

    // A count alone cannot be checked against a face — the dialog has to name them, not just the row.
    expect(
      within(screen.getByRole("dialog")).getByText("Nguyễn Văn An"),
    ).toBeInTheDocument();

    clickButton("lockDialog.submit");
    // Notification is on by default, so the reason is required — and the dialog stays open.
    expect(toasts.result.current.at(-1)?.message).toBe(
      "lockDialog.errorReasonRequired",
    );
    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });

  it("locks the rows and reports the count once a reason is given", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(<AdminUserManagementView />);
    tickRow(ACTIVE_ROW);
    clickButton("bulkLock");
    typeReason("spam");
    clickButton("lockDialog.submit");

    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toBe(
        "lockDoneNotified:1,spam",
      ),
    );
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("unlocks a locked row through a confirmation that names it", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(<AdminUserManagementView />);
    tickRow(LOCKED_ROW);
    clickButton("bulkUnlock");

    expect(
      within(screen.getByRole("dialog")).getByText("Hoàng Minh Trí"),
    ).toBeInTheDocument();
    clickButton("confirmUnlockAction");
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toBe("unlockDone:1"),
    );
  });

  it("warns about self-lock instead of silently signing the admin out", () => {
    render(<AdminUserManagementView />);
    tickRow(ACTING_ADMIN);
    clickButton("bulkLock");
    expect(screen.getByText("lockDialog.selfWarning")).toBeInTheDocument();
  });

  it("drops the selection when the filter changes, so the count cannot go stale", () => {
    render(<AdminUserManagementView />);
    tickRow(ACTIVE_ROW);
    expect(
      screen.getByRole("button", { name: "bulkLock" }),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("searchLabel"), {
      target: { value: "Bích" },
    });

    // BulkActionBar floats nothing at zero selected.
    expect(screen.queryByRole("button", { name: "bulkLock" })).toBeNull();
  });
});

describe("AdminUserManagementView per-account actions", () => {
  it("offers the pending status as a filter", () => {
    render(<AdminUserManagementView />);
    fireEvent.click(screen.getByRole("button", { name: /statusFilterLabel/ }));
    fireEvent.click(screen.getByRole("option", { name: "status.pending" }));
    expect(screen.getByText("Đỗ Quốc Bảo")).toBeInTheDocument();
    expect(screen.queryByText("Nguyễn Văn An")).toBeNull();
  });

  it("no longer lists the instructor-request task", () => {
    render(<AdminUserManagementView />);
    expect(screen.queryByText("pending.instructorRequest.label")).toBeNull();
  });

  it("changes one account's role from its row", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(<AdminUserManagementView />);
    fireEvent.click(
      screen.getByRole("button", { name: `actionChangeRoleFor:${ACTIVE_ROW}` }),
    );
    fireEvent.change(screen.getByLabelText("roleDialog.label"), {
      target: { value: "instructor" },
    });
    clickButton("roleDialog.submit");
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toBe(
        `roleDone:${ACTIVE_ROW},role.instructor`,
      ),
    );
  });

  it("blocks demoting the last active admin and keeps the dialog action disabled until a change", () => {
    render(<AdminUserManagementView />);
    fireEvent.click(
      screen.getByRole("button", {
        name: `actionChangeRoleFor:${ACTING_ADMIN}`,
      }),
    );
    expect(
      screen.getByRole("button", { name: "roleDialog.submit" }),
    ).toBeDisabled();
  });

  it("locks and unlocks a single account without a selection", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(<AdminUserManagementView />);

    fireEvent.click(
      screen.getByRole("button", { name: `actionUnlockFor:${LOCKED_ROW}` }),
    );
    expect(
      within(screen.getByRole("dialog")).getByText(LOCKED_ROW),
    ).toBeInTheDocument();
    clickButton("confirmUnlockAction");
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toBe("unlockDone:1"),
    );

    fireEvent.click(
      screen.getByRole("button", { name: `actionLockFor:${ACTIVE_ROW}` }),
    );
    typeReason("spam");
    clickButton("lockDialog.submit");
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toBe(
        "lockDoneNotified:1,spam",
      ),
    );
  });

  it("resets a password only after confirming, naming the address", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(<AdminUserManagementView />);
    fireEvent.click(
      screen.getByRole("button", {
        name: `actionResetPasswordFor:${ACTIVE_ROW}`,
      }),
    );
    clickButton("resetAction");
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toBe(
        "resetDone:nguyenvana@sv.edu.vn",
      ),
    );
  });
});

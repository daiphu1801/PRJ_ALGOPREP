// View-level test of the change-password dialog (BD USR0502 Khu vực E, DD F1 #13), in its normal
// form and in the forced form of the temporary-password flow. Only HTTP is faked.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../messages/vi.json";
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
import { ChangePasswordDialog } from "./change-password-dialog";

const d = viMessages.profile.passwordDialog;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const onClose = vi.fn();
const onSignOut = vi.fn();

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

function fill(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

function submit(options: { hasPassword?: boolean; forced?: boolean } = {}) {
  render(
    withTestProviders(
      <ChangePasswordDialog
        hasPassword={options.hasPassword ?? true}
        onClose={onClose}
        forced={options.forced ? { onSignOut } : undefined}
      />,
    ),
  );
  if (options.hasPassword !== false) fill(d.currentPassword, "OldPass123");
  fill(d.newPassword, "NewPass123");
  fill(d.confirmPassword, "NewPass123");
  fireEvent.click(screen.getByRole("button", { name: d.confirm }));
}

describe("ChangePasswordDialog", () => {
  beforeEach(() => {
    resetApiMock();
    onClose.mockReset();
    onSignOut.mockReset();
    act(() => toast.clear());
  });

  it("sends both passwords, toasts success and closes", async () => {
    const read = watch();
    apiMock.post.mockResolvedValue(undefined);

    submit();

    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    expect(apiMock.post).toHaveBeenCalledWith("/identity/me/password", {
      currentPassword: "OldPass123",
      newPassword: "NewPass123",
    });
    expect(read().at(-1)).toEqual(["success", d.done]);
  });

  it("sets a first password on an OAuth-only account without a current password", async () => {
    const read = watch();
    apiMock.post.mockResolvedValue(undefined);

    submit({ hasPassword: false });

    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    expect(read().at(-1)).toEqual(["success", d.doneSet]);
  });

  it("marks the current-password field for IDT-115 and stays open", async () => {
    const read = watch();
    apiMock.post.mockRejectedValue(new ApiError("IDT-115", 422));

    submit();

    await waitFor(() =>
      expect(read().at(-1)).toEqual([
        "error",
        viMessages.profile.errors.currentPasswordInvalid,
      ]),
    );
    expect(screen.getByLabelText(d.currentPassword)).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(onClose).not.toHaveBeenCalled();
  });

  it.each(["IDT-V01", "IDT-V02", "IDT-V03", "IDT-V04", "IDT-503", "IDT-105"])(
    "shows apiErrors.%s as an error toast and stays open",
    async (code) => {
      const read = watch();
      apiMock.post.mockRejectedValue(new ApiError(code, 422));

      submit();

      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(onClose).not.toHaveBeenCalled();
    },
  );

  it("falls back to UNKNOWN_ERROR for a non-API failure", async () => {
    const read = watch();
    apiMock.post.mockRejectedValue(new Error("boom"));

    submit();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors.UNKNOWN_ERROR]),
    );
  });

  it("forced form: cannot be dismissed, offers sign-out, and closes after a change", async () => {
    apiMock.post.mockResolvedValue(undefined);

    submit({ forced: true });

    expect(screen.getByText(d.forcedHint)).toBeInTheDocument();
    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    expect(onSignOut).not.toHaveBeenCalled();
  });

  it("forced form: a rejected temporary password shows the API message and keeps the lock", async () => {
    const read = watch();
    apiMock.post.mockRejectedValue(new ApiError("IDT-V04", 422));

    submit({ forced: true });

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors["IDT-V04"]]),
    );
    expect(onClose).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: d.signOut }));
    expect(onSignOut).toHaveBeenCalledTimes(1);
  });

  it("never calls the server for a password that is too short", () => {
    render(withTestProviders(<ChangePasswordDialog hasPassword onClose={onClose} />));
    fill(d.currentPassword, "OldPass123");
    fill(d.newPassword, "a1");
    fill(d.confirmPassword, "a1");
    fireEvent.click(screen.getByRole("button", { name: d.confirm }));
    expect(apiMock.post).not.toHaveBeenCalled();
  });
});

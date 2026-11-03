// View-level test of the account deletion dialog (BD USR0503 Khu vực I, DD F1 #18): success leaves
// for the sign-in page, and every failure shows its translated message. Only HTTP is faked.
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
import { ApiError, tokenStore } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { DeleteAccountDialog } from "./delete-account-dialog";

const s = viMessages.settings;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const EMAIL = "learner@example.com";
const onClose = vi.fn();

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

function confirmDeletion(typed = ` ${EMAIL.toUpperCase()} `) {
  render(
    withTestProviders(<DeleteAccountDialog email={EMAIL} onClose={onClose} />),
  );
  fireEvent.change(screen.getByRole("dialog").querySelector("input")!, {
    target: { value: typed },
  });
  fireEvent.click(
    screen.getByRole("button", { name: s.deleteConfirm.confirm }),
  );
}

describe("DeleteAccountDialog", () => {
  beforeEach(() => {
    resetApiMock();
    push.mockReset();
    onClose.mockReset();
    tokenStore.set("token");
    act(() => toast.clear());
  });

  it("sends the trimmed e-mail, drops the session and goes to the sign-in page", async () => {
    const read = watch();
    apiMock.post.mockResolvedValue(undefined);

    confirmDeletion();

    await waitFor(() => expect(push).toHaveBeenCalledWith("/login"));
    expect(apiMock.post).toHaveBeenCalledWith("/identity/me/account/delete", {
      emailConfirmation: EMAIL.toUpperCase(),
    });
    expect(tokenStore.get()).toBeNull();
    expect(read().at(-1)).toEqual(["success", s.deleteConfirm.done]);
  });

  it("shows the field message when the server finds a mismatch (IDT-201) and stays", async () => {
    const read = watch();
    apiMock.post.mockRejectedValue(new ApiError("IDT-201", 422));

    confirmDeletion();

    await waitFor(() =>
      expect(read().at(-1)).toEqual([
        "error",
        s.errors.confirmPhraseMismatch,
      ]),
    );
    expect(push).not.toHaveBeenCalled();
  });

  it.each(["IDT-432", "IDT-503", "IDT-105"])(
    "shows apiErrors.%s as an error toast, keeps the session and stays on the page",
    async (code) => {
      const read = watch();
      apiMock.post.mockRejectedValue(new ApiError(code, 422));

      confirmDeletion();

      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(push).not.toHaveBeenCalled();
      expect(tokenStore.get()).toBe("token");
      expect(
        screen.getByRole("button", { name: s.deleteConfirm.confirm }),
      ).toBeEnabled();
    },
  );

  it("falls back to UNKNOWN_ERROR for a non-API failure", async () => {
    const read = watch();
    apiMock.post.mockRejectedValue(new Error("boom"));

    confirmDeletion();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors.UNKNOWN_ERROR]),
    );
  });

  it("never calls the server while the typed e-mail does not match", () => {
    confirmDeletion("someone@else.com");
    expect(apiMock.post).not.toHaveBeenCalled();
  });
});

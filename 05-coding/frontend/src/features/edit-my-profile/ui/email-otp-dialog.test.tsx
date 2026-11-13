// View-level test of the e-mail change code dialog (BD USR0502 Khu vực G, DD F1 #11 and #12):
// resend button with a 60 s cooldown, 10 minute code countdown, and every error code the two
// endpoints can return. Only HTTP is faked; real-email-change.ts runs for real. Fake timers drive
// the countdowns, so nothing here depends on the wall clock.
import { act, fireEvent, render, renderHook, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
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
import { EmailOtpDialog } from "./email-otp-dialog";

const m = viMessages.profile.emailOtp;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const REQUEST = "/identity/me/email-change/request";
const CONFIRM = "/identity/me/email-change/confirm";

const onCancel = vi.fn();
const onConfirmed = vi.fn();

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

async function flush() {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(0);
  });
}

async function advance(ms: number) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });
}

function renderDialog() {
  render(
    withTestProviders(
      <EmailOtpDialog
        newEmail="new@example.com"
        onCancel={onCancel}
        onConfirmed={onConfirmed}
      />,
    ),
  );
}

const resendLabel = (seconds: number) =>
  m.resendCooldown.replace("{seconds}", String(seconds));

function typeCode(code: string) {
  fireEvent.change(screen.getByLabelText(m.codeLabel), {
    target: { value: code },
  });
}

describe("EmailOtpDialog", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    resetApiMock();
    onCancel.mockReset();
    onConfirmed.mockReset();
    act(() => toast.clear());
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it("starts with the resend button locked for 60 s and the code valid for 10 minutes", () => {
    renderDialog();
    expect(screen.getByRole("button", { name: resendLabel(60) })).toBeDisabled();
    expect(screen.getByRole("timer")).toHaveTextContent("10:00");
  });

  it("counts both timers down and unlocks resend after the cooldown", async () => {
    renderDialog();
    await advance(15_000);
    expect(screen.getByRole("button", { name: resendLabel(45) })).toBeDisabled();
    expect(screen.getByRole("timer")).toHaveTextContent("09:45");

    await advance(45_000);
    expect(screen.getByRole("button", { name: m.resend })).toBeEnabled();
    expect(screen.getByRole("timer")).toHaveTextContent("09:00");
  });

  it("resends to the same address, restarts both timers and clears the typed code", async () => {
    const read = watch();
    apiMock.post.mockResolvedValue({
      pendingEmail: "new@example.com",
      expiresInSeconds: 600,
    });
    renderDialog();
    typeCode("123456");
    await advance(60_000);

    fireEvent.click(screen.getByRole("button", { name: m.resend }));
    await flush();

    expect(apiMock.post).toHaveBeenCalledWith(REQUEST, {
      newEmail: "new@example.com",
    });
    expect(read().at(-1)).toEqual(["success", m.resent]);
    expect(screen.getByRole("button", { name: resendLabel(60) })).toBeDisabled();
    expect(screen.getByRole("timer")).toHaveTextContent("10:00");
    expect(screen.getByLabelText(m.codeLabel)).toHaveValue("");
  });

  it("locks the confirm button and says so once the code has expired", async () => {
    renderDialog();
    typeCode("123456");
    expect(screen.getByRole("button", { name: m.confirm })).toBeEnabled();

    await advance(600_000);

    expect(screen.getByRole("timer")).toHaveTextContent(m.expired);
    expect(screen.getByRole("button", { name: m.confirm })).toBeDisabled();
  });

  it.each(["IDT-V12", "IDT-V05", "IDT-415", "IDT-503"])(
    "shows apiErrors.%s when resend fails and keeps the timers as they were",
    async (code) => {
      const read = watch();
      apiMock.post.mockRejectedValue(new ApiError(code, 429));
      renderDialog();
      await advance(60_000);

      fireEvent.click(screen.getByRole("button", { name: m.resend }));
      await flush();

      expect(read()).toContainEqual(["error", apiErrors[code]]);
      expect(screen.getByRole("button", { name: m.resend })).toBeEnabled();
    },
  );

  it("confirms with the code only and reports success", async () => {
    const read = watch();
    apiMock.post.mockResolvedValue({ email: "new@example.com" });
    renderDialog();
    typeCode("123456");

    fireEvent.click(screen.getByRole("button", { name: m.confirm }));
    await flush();

    expect(apiMock.post).toHaveBeenCalledWith(CONFIRM, { code: "123456" });
    expect(onConfirmed).toHaveBeenCalledTimes(1);
    expect(read().at(-1)).toEqual(["success", m.done]);
  });

  it("marks a wrong or expired code (IDT-416) red with the field message and stays open", async () => {
    const read = watch();
    apiMock.post.mockRejectedValue(new ApiError("IDT-416", 422));
    renderDialog();
    typeCode("000000");

    fireEvent.click(screen.getByRole("button", { name: m.confirm }));
    await flush();

    expect(screen.getByLabelText(m.codeLabel)).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(read().at(-1)).toEqual([
      "error",
      viMessages.profile.errors.otpInvalid,
    ]);
    expect(onConfirmed).not.toHaveBeenCalled();
  });

  it.each(["IDT-415", "IDT-503"])(
    "shows apiErrors.%s as a toast on confirm without marking the code red",
    async (code) => {
      const read = watch();
      apiMock.post.mockRejectedValue(new ApiError(code, 409));
      renderDialog();
      typeCode("123456");

      fireEvent.click(screen.getByRole("button", { name: m.confirm }));
      await flush();

      expect(read()).toContainEqual(["error", apiErrors[code]]);
      expect(screen.getByLabelText(m.codeLabel)).not.toHaveAttribute(
        "aria-invalid",
      );
    },
  );

  it("falls back to UNKNOWN_ERROR for a non-API failure", async () => {
    const read = watch();
    apiMock.post.mockRejectedValue(new Error("boom"));
    renderDialog();
    typeCode("123456");

    fireEvent.click(screen.getByRole("button", { name: m.confirm }));
    await flush();

    expect(read()).toContainEqual(["error", apiErrors.UNKNOWN_ERROR]);
  });

  it("never calls the server for a code that is not six digits", () => {
    renderDialog();
    typeCode("123");
    expect(screen.getByRole("button", { name: m.confirm })).toBeDisabled();
    expect(apiMock.post).not.toHaveBeenCalled();
  });
});

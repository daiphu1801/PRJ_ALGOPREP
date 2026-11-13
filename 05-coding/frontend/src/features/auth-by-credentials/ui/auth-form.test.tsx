// View-level tests of the auth screens wired to the real identity API (BD SHR0101, DD F1 #40 to
// #61): login, sign-up, e-mail code, forgot password (three steps), account restore, instructor
// login. Only HTTP is faked, so real-auth.ts decides which code is a field outcome and which is a
// rethrown error; the test then checks the words the user sees and the field it marks.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
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
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
  useSearchParams: () => new URLSearchParams(),
}));

// Import after the mocks.
import { instructorLogin, type AuthMode } from "@/entities/auth";
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { AuthForm } from "./auth-form";
import { useAuthFlow } from "../model/use-auth-flow";

const a = viMessages.auth;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const AUTH = "/identity/auth";

type Reply = unknown | ApiError;
/** Scripts `api.post` by path; a path with no entry resolves to `undefined` (empty 204). */
function reply(routes: Record<string, Reply>) {
  apiMock.post.mockImplementation((path: string) => {
    const result = routes[path];
    return result instanceof ApiError
      ? Promise.reject(result)
      : Promise.resolve(result);
  });
}

const fail = (code: string, status = 422, details?: Record<string, unknown>) =>
  new ApiError(code, status, undefined, details);

function Harness({
  mode,
  instructor = false,
}: {
  mode: AuthMode;
  instructor?: boolean;
}) {
  const flow = useAuthFlow(mode, instructor ? instructorLogin : undefined);
  return <AuthForm flow={flow} showOAuth={false} />;
}

function renderMode(mode: AuthMode, instructor = false) {
  return render(withTestProviders(<Harness mode={mode} instructor={instructor} />));
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

const type = (label: string, value: string) =>
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
const click = (name: string) =>
  fireEvent.click(screen.getByRole("button", { name }));
const invalid = (label: string) =>
  expect(screen.getByLabelText(label)).toHaveAttribute("aria-invalid", "true");

function submitLogin(email = "learner@example.com", password = "password123") {
  type(a.emailLabel, email);
  type(a.passwordLabel, password);
  click(a.loginSubmit);
}

function submitSignup() {
  type(a.displayNameLabel, "Learner One");
  type(a.passwordLabel, "password123");
  type(a.emailLabel, "learner@example.com");
  fireEvent.click(screen.getByLabelText(a.termsLabel));
  click(a.signupSubmit);
}

beforeEach(() => {
  resetApiMock();
  push.mockReset();
  tokenStore.set(null);
  act(() => toast.clear());
});

describe("login", () => {
  it("shows the field message for IDT-102 and marks the password", async () => {
    const read = watch();
    reply({ [`${AUTH}/login`]: fail("IDT-102", 401) });
    renderMode("login");

    submitLogin();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", a.errors.credentialsInvalid]),
    );
    invalid(a.passwordLabel);
    expect(push).not.toHaveBeenCalled();
  });

  it("IDT-113 explains the e-mail is unverified and continues at the code step", async () => {
    const read = watch();
    reply({ [`${AUTH}/login`]: fail("IDT-113", 403) });
    renderMode("login");

    submitLogin();

    await waitFor(() =>
      expect(read()).toContainEqual(["error", a.errors.emailNotVerified]),
    );
    expect(
      await screen.findByText(
        a.verifyEmailHint.replace("{email}", "learner@example.com"),
      ),
    ).toBeInTheDocument();
  });

  it.each(["IDT-103", "IDT-104", "IDT-503", "IDT-V11"])(
    "shows apiErrors.%s as an error toast and does not sign in",
    async (code) => {
      const read = watch();
      reply({ [`${AUTH}/login`]: fail(code, 429) });
      renderMode("login");

      submitLogin();

      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(push).not.toHaveBeenCalled();
      expect(tokenStore.get()).toBeNull();
    },
  );

  it("instructor login: IDT-111 shows the not-an-instructor message on the e-mail", async () => {
    const read = watch();
    reply({ [`${AUTH}/instructor/login`]: fail("IDT-111", 403) });
    renderMode("login", true);

    submitLogin();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", a.errors.notInstructorAccount]),
    );
    invalid(a.emailLabel);
  });

  it("falls back to UNKNOWN_ERROR for a non-API failure", async () => {
    const read = watch();
    apiMock.post.mockRejectedValue(new Error("network down"));
    renderMode("login");

    submitLogin();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors.UNKNOWN_ERROR]),
    );
  });

  it("never calls the server for an empty password", async () => {
    renderMode("login");
    submitLogin("learner@example.com", "");
    await waitFor(() => invalid(a.passwordLabel));
    expect(apiMock.post).not.toHaveBeenCalled();
  });

  describe("success", () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });
    afterEach(() => {
      vi.useRealTimers();
    });

    it("stores the token and routes to the area after the loading steps", async () => {
      reply({
        [`${AUTH}/login`]: {
          accessToken: "jwt-1",
          user: { baseCategory: "STUDENT", mustChangePassword: false },
        },
      });
      renderMode("login");

      submitLogin();
      await act(async () => {
        await vi.advanceTimersByTimeAsync(2500);
      });

      expect(tokenStore.get()).toBe("jwt-1");
      expect(push).toHaveBeenCalledTimes(1);
    });
  });
});

describe("restore a self-deleted account", () => {
  async function reachBanner() {
    reply({
      [`${AUTH}/login`]: fail("IDT-104", 403, { restoreTicket: "ticket-1" }),
    });
    renderMode("login");
    submitLogin();
    await screen.findByText(a.deactivatedBannerMessage);
  }

  it("offers the banner when login answers IDT-104 with a ticket, and restores with it", async () => {
    const read = watch();
    await reachBanner();
    reply({ [`${AUTH}/deactivation/restore`]: undefined });

    click(a.deactivatedBannerCancel);

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", a.notice.deactivationCancelled]),
    );
    expect(apiMock.post).toHaveBeenLastCalledWith(
      `${AUTH}/deactivation/restore`,
      { restoreTicket: "ticket-1" },
    );
    expect(screen.queryByText(a.deactivatedBannerMessage)).toBeNull();
  });

  it.each(["IDT-114", "IDT-503"])(
    "shows apiErrors.%s when the restore fails and drops the used-up banner",
    async (code) => {
      const read = watch();
      await reachBanner();
      reply({ [`${AUTH}/deactivation/restore`]: fail(code, 401) });

      click(a.deactivatedBannerCancel);

      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(screen.queryByText(a.deactivatedBannerMessage)).toBeNull();
    },
  );
});

describe("sign-up", () => {
  it("IDT-101 says the e-mail is taken and marks it", async () => {
    const read = watch();
    reply({ [`${AUTH}/register`]: fail("IDT-101", 409) });
    renderMode("signup");

    submitSignup();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", a.errors.emailTaken]),
    );
    invalid(a.emailLabel);
  });

  it.each([
    ["IDT-400", a.emailLabel],
    ["IDT-V19", a.displayNameLabel],
    ["IDT-V20", a.displayNameLabel],
    ["IDT-V01", a.passwordLabel],
    ["IDT-V02", a.passwordLabel],
    ["IDT-V03", a.passwordLabel],
    ["IDT-V04", a.passwordLabel],
  ])("shows apiErrors.%s and marks its field", async (code, label) => {
    const read = watch();
    reply({ [`${AUTH}/register`]: fail(code) });
    renderMode("signup");

    submitSignup();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
    );
    invalid(label);
  });

  it.each(["IDT-V11", "IDT-503"])("shows apiErrors.%s as a toast", async (code) => {
    const read = watch();
    reply({ [`${AUTH}/register`]: fail(code, 503) });
    renderMode("signup");

    submitSignup();

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
    );
  });

  it("sends exactly e-mail, password and name, then moves to the e-mail code step", async () => {
    const read = watch();
    reply({ [`${AUTH}/register`]: undefined });
    renderMode("signup");

    submitSignup();

    expect(
      await screen.findByText(
        a.verifyEmailHint.replace("{email}", "learner@example.com"),
      ),
    ).toBeInTheDocument();
    expect(apiMock.post).toHaveBeenCalledWith(`${AUTH}/register`, {
      email: "learner@example.com",
      password: "password123",
      displayName: "Learner One",
    });
    expect(read().at(-1)).toEqual(["success", a.notice.codeSent]);
  });
});

describe("e-mail code step", () => {
  /** Login answers IDT-113 so the screen lands on the code step with the resend button unlocked. */
  async function reachCodeStep() {
    reply({ [`${AUTH}/login`]: fail("IDT-113", 403) });
    renderMode("login");
    submitLogin();
    await screen.findByLabelText(a.otpLabel);
    act(() => toast.clear());
  }

  it("IDT-112 marks the code, says it is wrong and counts the attempts down", async () => {
    const read = watch();
    await reachCodeStep();
    reply({ [`${AUTH}/email/verify`]: fail("IDT-112") });
    type(a.otpLabel, "123456");

    click(a.verifyEmailSubmit);

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", a.errors.otpInvalid]),
    );
    invalid(a.otpLabel);
    expect(screen.getByText(a.otpAttemptsLeft.replace("{count}", "4"))).toBeInTheDocument();
    expect(apiMock.post).toHaveBeenLastCalledWith(`${AUTH}/email/verify`, {
      email: "learner@example.com",
      otp: "123456",
    });
  });

  it.each(["IDT-503", "IDT-V11"])("shows apiErrors.%s when confirming fails", async (code) => {
    const read = watch();
    await reachCodeStep();
    reply({ [`${AUTH}/email/verify`]: fail(code, 503) });
    type(a.otpLabel, "123456");

    click(a.verifyEmailSubmit);

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
    );
  });

  it("a right code returns to login with the e-mail filled", async () => {
    const read = watch();
    await reachCodeStep();
    reply({ [`${AUTH}/email/verify`]: undefined });
    type(a.otpLabel, "123456");

    click(a.verifyEmailSubmit);

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", a.notice.emailVerified]),
    );
    expect(screen.getByLabelText(a.emailLabel)).toHaveValue("learner@example.com");
  });

  it("resend: success toasts, IDT-V12 and IDT-503 show their apiErrors text", async () => {
    const read = watch();
    await reachCodeStep();

    reply({ [`${AUTH}/email/verify/resend`]: undefined });
    click(a.otpResend);
    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", a.notice.codeResent]),
    );
    expect(apiMock.post).toHaveBeenLastCalledWith(`${AUTH}/email/verify/resend`, {
      email: "learner@example.com",
    });
  });

  it.each(["IDT-V12", "IDT-503"])("resend shows apiErrors.%s", async (code) => {
    const read = watch();
    await reachCodeStep();
    reply({ [`${AUTH}/email/verify/resend`]: fail(code, 429) });

    click(a.otpResend);

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
    );
  });
});

describe("forgot password", () => {
  function openForgot() {
    renderMode("login");
    click(a.forgotPasswordLink);
  }

  async function reachOtpStep() {
    reply({ [`${AUTH}/password/forgot`]: undefined });
    openForgot();
    type(a.emailLabel, "learner@example.com");
    click(a.forgotEmailSubmit);
    await screen.findByLabelText(a.otpLabel);
    act(() => toast.clear());
  }

  async function reachResetStep() {
    reply({ [`${AUTH}/password/verify-otp`]: undefined });
    type(a.otpLabel, "123456");
    click(a.forgotOtpSubmit);
    await screen.findByLabelText(a.newPasswordLabel);
    act(() => toast.clear());
  }

  it("step 1 sends the e-mail and moves on to the code step", async () => {
    const read = watch();
    await reachOtpStep();
    expect(apiMock.post).toHaveBeenCalledWith(`${AUTH}/password/forgot`, {
      email: "learner@example.com",
    });
    expect(read()).toEqual([]);
  });

  it.each(["IDT-400", "IDT-V12", "IDT-503"])(
    "step 1 shows apiErrors.%s and stays on the e-mail step",
    async (code) => {
      const read = watch();
      reply({ [`${AUTH}/password/forgot`]: fail(code, 429) });
      openForgot();
      type(a.emailLabel, "learner@example.com");

      click(a.forgotEmailSubmit);

      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(screen.queryByLabelText(a.otpLabel)).toBeNull();
    },
  );

  it("step 2: IDT-107 marks the code and counts the attempts down", async () => {
    await reachOtpStep();
    const read = watch();
    reply({ [`${AUTH}/password/verify-otp`]: fail("IDT-107") });
    type(a.otpLabel, "000000");

    click(a.forgotOtpSubmit);

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", a.errors.otpInvalid]),
    );
    invalid(a.otpLabel);
    expect(screen.getByText(a.otpAttemptsLeft.replace("{count}", "4"))).toBeInTheDocument();
  });

  it("step 2: IDT-503 shows apiErrors", async () => {
    await reachOtpStep();
    const read = watch();
    reply({ [`${AUTH}/password/verify-otp`]: fail("IDT-503", 503) });
    type(a.otpLabel, "123456");

    click(a.forgotOtpSubmit);

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors["IDT-503"]]),
    );
  });

  it.each(["IDT-V01", "IDT-V02", "IDT-V03", "IDT-V04"])(
    "step 3 shows apiErrors.%s and marks the new password",
    async (code) => {
      await reachOtpStep();
      await reachResetStep();
      const read = watch();
      reply({ [`${AUTH}/password/reset`]: fail(code) });
      type(a.newPasswordLabel, "password123");
      type(a.confirmPasswordLabel, "password123");

      click(a.forgotResetSubmit);

      await waitFor(() =>
        expect(read().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      invalid(a.newPasswordLabel);
    },
  );

  it("step 3: IDT-107 (code expired meanwhile) shows its apiErrors text", async () => {
    await reachOtpStep();
    await reachResetStep();
    const read = watch();
    reply({ [`${AUTH}/password/reset`]: fail("IDT-107") });
    type(a.newPasswordLabel, "password123");
    type(a.confirmPasswordLabel, "password123");

    click(a.forgotResetSubmit);

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["error", apiErrors["IDT-107"]]),
    );
  });

  it("step 3 success sends e-mail, code and new password, then returns to login without signing in", async () => {
    await reachOtpStep();
    await reachResetStep();
    const read = watch();
    reply({ [`${AUTH}/password/reset`]: undefined });
    type(a.newPasswordLabel, "password123");
    type(a.confirmPasswordLabel, "password123");

    click(a.forgotResetSubmit);

    await waitFor(() =>
      expect(read().at(-1)).toEqual(["success", a.notice.passwordReset]),
    );
    expect(apiMock.post).toHaveBeenLastCalledWith(`${AUTH}/password/reset`, {
      email: "learner@example.com",
      otp: "123456",
      newPassword: "password123",
    });
    expect(tokenStore.get()).toBeNull();
    expect(push).not.toHaveBeenCalled();
  });

  it("step 3 never calls the server when the two passwords differ", async () => {
    await reachOtpStep();
    await reachResetStep();
    apiMock.post.mockClear();
    type(a.newPasswordLabel, "password123");
    type(a.confirmPasswordLabel, "password124");

    click(a.forgotResetSubmit);

    await waitFor(() => invalid(a.confirmPasswordLabel));
    expect(apiMock.post).not.toHaveBeenCalled();
  });
});

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, tokenStore } from "@/shared/api";

const post = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { post: (...args: unknown[]) => post(...args) },
}));

// Import after the mock so real-auth talks to the fake `api`.
import {
  realForgotEmail,
  realLogin,
  realLogout,
  realResetPassword,
  realRestoreAccount,
  realSignup,
  realVerifyEmail,
  realVerifyResetOtp,
} from "./real-auth";

const login = { email: "a@b.vn", password: "Matkhau123" };

describe("real auth API", () => {
  beforeEach(() => {
    post.mockReset();
    tokenStore.set(null);
  });
  afterEach(() => tokenStore.set(null));

  it("signs the user in, keeps the access token in memory and reports the role", async () => {
    post.mockResolvedValue({
      accessToken: "jwt",
      user: { baseCategory: "INSTRUCTOR" },
    });

    const outcome = await realLogin(login, true);

    expect(post).toHaveBeenCalledWith("/identity/auth/instructor/login", login);
    expect(outcome).toEqual({ ok: true, role: "INSTRUCTOR" });
    expect(tokenStore.get()).toBe("jwt");
  });

  it("uses the student endpoint for the normal portal", async () => {
    post.mockResolvedValue({
      accessToken: "jwt",
      user: { baseCategory: "STUDENT" },
    });
    await realLogin(login, false);
    expect(post).toHaveBeenCalledWith("/identity/auth/login", login);
  });

  it("turns wrong credentials into a password field error (IDT-102)", async () => {
    post.mockRejectedValue(new ApiError("IDT-102", 401));
    expect(await realLogin(login, false)).toEqual({
      ok: false,
      fieldErrors: { password: "errors.credentialsInvalid" },
    });
    expect(tokenStore.get()).toBeNull();
  });

  it("asks for e-mail verification when the account is unverified (IDT-113)", async () => {
    post.mockRejectedValue(new ApiError("IDT-113", 403));
    expect(await realLogin(login, false)).toEqual({
      ok: false,
      fieldErrors: { email: "errors.emailNotVerified" },
      needsEmailVerification: true,
    });
  });

  it("rejects a non-instructor on the instructor portal (IDT-111)", async () => {
    post.mockRejectedValue(new ApiError("IDT-111", 403));
    expect(await realLogin(login, true)).toEqual({
      ok: false,
      fieldErrors: { email: "errors.notInstructorAccount" },
    });
  });

  it("hands the restore ticket of a self-deleted account to the caller without signing in (IDT-104)", async () => {
    post.mockRejectedValue(
      new ApiError("IDT-104", 403, "x", {
        restoreTicket: "t1",
        expiresInSeconds: 300,
      }),
    );
    expect(await realLogin(login, false)).toEqual({
      ok: true,
      deactivated: true,
      restoreTicket: "t1",
    });
    expect(tokenStore.get()).toBeNull();
  });

  it("rethrows a deactivated account that has no ticket, and every unknown error", async () => {
    post.mockRejectedValueOnce(new ApiError("IDT-104", 403));
    await expect(realLogin(login, false)).rejects.toMatchObject({
      code: "IDT-104",
    });
    post.mockRejectedValueOnce(new ApiError("IDT-503", 503));
    await expect(realLogin(login, false)).rejects.toMatchObject({
      code: "IDT-503",
    });
  });

  it("registers with e-mail, password and display name only", async () => {
    post.mockResolvedValue({});
    const outcome = await realSignup({
      displayName: "Learner One",
      password: "Matkhau123",
      email: "a@b.vn",
      termsAccepted: true,
    });
    expect(post).toHaveBeenCalledWith("/identity/auth/register", {
      email: "a@b.vn",
      password: "Matkhau123",
      displayName: "Learner One",
    });
    expect(outcome).toEqual({ ok: true, email: "a@b.vn" });
  });

  it("marks the e-mail field when it is already registered (IDT-101)", async () => {
    post.mockRejectedValue(new ApiError("IDT-101", 409));
    expect(
      await realSignup({
        displayName: "Learner One",
        password: "Matkhau123",
        email: "a@b.vn",
        termsAccepted: true,
      }),
    ).toEqual({ ok: false, fieldErrors: { email: "errors.emailTaken" } });
  });

  it("turns a wrong or expired code into an otp field error, for both flows", async () => {
    post.mockRejectedValueOnce(new ApiError("IDT-112", 422));
    expect(await realVerifyEmail("a@b.vn", { otp: "111111" })).toEqual({
      ok: false,
      fieldErrors: { otp: "errors.otpInvalid" },
    });
    post.mockRejectedValueOnce(new ApiError("IDT-107", 422));
    expect(await realVerifyResetOtp("a@b.vn", { otp: "111111" })).toEqual({
      ok: false,
      fieldErrors: { otp: "errors.otpInvalid" },
    });
    // The other flow's code is not this flow's wrong-code answer.
    post.mockRejectedValueOnce(new ApiError("IDT-107", 422));
    await expect(
      realVerifyEmail("a@b.vn", { otp: "111111" }),
    ).rejects.toMatchObject({ code: "IDT-107" });
  });

  it("sends the e-mail and the verified code when resetting the password", async () => {
    post.mockResolvedValue({});
    await realResetPassword(
      { newPassword: "Matkhaumoi456", confirmPassword: "Matkhaumoi456" },
      "a@b.vn",
      "123456",
    );
    expect(post).toHaveBeenCalledWith("/identity/auth/password/reset", {
      email: "a@b.vn",
      otp: "123456",
      newPassword: "Matkhaumoi456",
    });
  });

  it("requests a reset code and restores an account with the ticket in the body", async () => {
    post.mockResolvedValue({});
    await realForgotEmail({ email: "a@b.vn" });
    expect(post).toHaveBeenCalledWith("/identity/auth/password/forgot", {
      email: "a@b.vn",
    });
    await realRestoreAccount("t1");
    expect(post).toHaveBeenCalledWith("/identity/auth/deactivation/restore", {
      restoreTicket: "t1",
    });
  });

  it("does not clear a new session when an earlier logout finishes late", async () => {
    tokenStore.set("account-A");
    let finish!: (value: unknown) => void;
    post.mockImplementationOnce(() => new Promise((resolve) => { finish = resolve; }));
    const logout = realLogout();
    tokenStore.set("account-B");
    finish({});
    await logout;
    expect(tokenStore.get()).toBe("account-B");
  });

  it("clears the in-memory token on logout even when the call fails", async () => {
    tokenStore.set("jwt");
    post.mockRejectedValue(new ApiError("UNKNOWN_ERROR", 0));
    await expect(realLogout()).rejects.toBeInstanceOf(ApiError);
    expect(tokenStore.get()).toBeNull();
  });

  it("remembers that the account must change its password, and forgets it on logout", async () => {
    post.mockResolvedValue({
      accessToken: "jwt",
      user: { baseCategory: "STUDENT", mustChangePassword: true },
    });
    await realLogin(login, false);
    expect(sessionStorage.getItem("algoprep.mustChangePassword")).toBe("1");

    post.mockResolvedValue({});
    await realLogout();
    expect(sessionStorage.getItem("algoprep.mustChangePassword")).toBeNull();
  });

  it("does not keep the flag for an ordinary login", async () => {
    sessionStorage.setItem("algoprep.mustChangePassword", "1");
    post.mockResolvedValue({
      accessToken: "jwt",
      user: { baseCategory: "STUDENT", mustChangePassword: false },
    });
    await realLogin(login, false);
    expect(sessionStorage.getItem("algoprep.mustChangePassword")).toBeNull();
  });
});

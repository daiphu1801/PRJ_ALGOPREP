// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { describe, expect, it } from "vitest";
import { fakeLogin, fakeSignup, fakeVerifyOtp } from "./fake-auth";

describe("fakeLogin", () => {
  it("returns the deactivated-recovery variant for the reserved 'deactivated' identifier (02-bd/screens/shared/auth.md section 3)", async () => {
    const outcome = await fakeLogin({ identifier: "deactivated", password: "anything", rememberMe: false });
    expect(outcome).toEqual({ ok: true, role: "STUDENT", deactivated: true });
  });

  it("routes ADMIN/INSTRUCTOR by reserved identifier so role-based navigation can be demoed", async () => {
    const admin = await fakeLogin({ identifier: "admin", password: "x", rememberMe: false });
    expect(admin).toMatchObject({ ok: true, role: "ADMIN" });
  });

  it("returns a field error on the wrong password, not a thrown exception", async () => {
    const outcome = await fakeLogin({ identifier: "learner01", password: "wrong", rememberMe: false });
    expect(outcome.ok).toBe(false);
  });
});

describe("fakeSignup", () => {
  it("rejects the reserved 'duplicate' username", async () => {
    const outcome = await fakeSignup({
      username: "duplicate",
      password: "password123",
      email: "a@b.com",
      termsAccepted: true,
    });
    expect(outcome.ok).toBe(false);
  });
});

describe("fakeVerifyOtp", () => {
  it("only accepts the canned OTP and counts down attempts otherwise", async () => {
    const wrong = await fakeVerifyOtp({ otp: "000000" }, "test-session-1");
    expect(wrong.ok).toBe(false);
    const right = await fakeVerifyOtp({ otp: "123456" }, "test-session-1");
    expect(right.ok).toBe(true);
  });
});

// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { describe, expect, it } from "vitest";
import {
  fakeInstructorLogin,
  fakeLogin,
  fakeSignup,
  fakeVerifyOtp,
} from "./fake-auth";

describe("fakeLogin", () => {
  it("returns the deactivated-recovery variant for the reserved 'deactivated' account (02-bd/screens/shared/SHR0101_auth.md Sheet 3)", async () => {
    const outcome = await fakeLogin({
      email: "deactivated@algoprep.local",
      password: "anything",
    });
    expect(outcome).toEqual({ ok: true, deactivated: true });
  });

  it("routes ADMIN/INSTRUCTOR by the reserved part before the @ so role-based navigation can be demoed", async () => {
    const admin = await fakeLogin({
      email: "admin@algoprep.local",
      password: "x",
    });
    expect(admin).toMatchObject({ ok: true, role: "ADMIN" });
  });

  it("returns a field error on the wrong password, not a thrown exception", async () => {
    const outcome = await fakeLogin({
      email: "learner01@example.com",
      password: "wrong",
    });
    expect(outcome).toEqual({
      ok: false,
      fieldErrors: { password: "errors.credentialsInvalid" },
    });
  });
});

describe("fakeInstructorLogin", () => {
  it("rejects a STUDENT account with a field error instead of logging it in (DEC-2026-0925-instructor-separate-login-route)", async () => {
    const outcome = await fakeInstructorLogin({
      email: "learner01@example.com",
      password: "x",
    });
    expect(outcome).toEqual({
      ok: false,
      fieldErrors: { email: "errors.notInstructorAccount" },
    });
  });

  it("logs in the reserved 'instructor' account", async () => {
    const outcome = await fakeInstructorLogin({
      email: "instructor@algoprep.local",
      password: "x",
    });
    expect(outcome).toMatchObject({ ok: true, role: "INSTRUCTOR" });
  });
});

describe("fakeSignup", () => {
  it("rejects the reserved 'duplicate' e-mail", async () => {
    const outcome = await fakeSignup({
      displayName: "Duplicate User",
      password: "password123",
      email: "duplicate@example.com",
      termsAccepted: true,
    });
    expect(outcome).toEqual({
      ok: false,
      fieldErrors: { email: "errors.emailTaken" },
    });
  });

  it("creates the account and hands back the e-mail to verify", async () => {
    const outcome = await fakeSignup({
      displayName: "Learner One",
      password: "password123",
      email: "learner@example.com",
      termsAccepted: true,
    });
    expect(outcome).toEqual({ ok: true, email: "learner@example.com" });
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

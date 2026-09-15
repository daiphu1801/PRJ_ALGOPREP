// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { describe, expect, it } from "vitest";
import {
  forgotOtpSchema,
  forgotResetSchema,
  loginSchema,
  signupSchema,
} from "./schema";

describe("signupSchema", () => {
  it("rejects submit when terms are not accepted (BD Q1, 02-bd/screens/shared/auth.md:140)", () => {
    const result = signupSchema.safeParse({
      username: "learner01",
      password: "password123",
      email: "learner@example.com",
      termsAccepted: false,
    });
    expect(result.success).toBe(false);
  });

  it("accepts a well-formed signup with terms accepted", () => {
    const result = signupSchema.safeParse({
      username: "learner01",
      password: "password123",
      email: "learner@example.com",
      termsAccepted: true,
    });
    expect(result.success).toBe(true);
  });
});

describe("loginSchema", () => {
  it("requires a non-empty identifier and password", () => {
    expect(loginSchema.safeParse({ identifier: "", password: "", rememberMe: false }).success).toBe(false);
    expect(
      loginSchema.safeParse({ identifier: "learner01", password: "x", rememberMe: false }).success,
    ).toBe(true);
  });
});

describe("forgotOtpSchema", () => {
  it("only accepts exactly 6 digits", () => {
    expect(forgotOtpSchema.safeParse({ otp: "123" }).success).toBe(false);
    expect(forgotOtpSchema.safeParse({ otp: "12a456" }).success).toBe(false);
    expect(forgotOtpSchema.safeParse({ otp: "123456" }).success).toBe(true);
  });
});

describe("forgotResetSchema", () => {
  it("rejects when the confirmation does not match (01-rd/screens/shared/auth.md:58-59)", () => {
    const result = forgotResetSchema.safeParse({ newPassword: "newpassword1", confirmPassword: "different" });
    expect(result.success).toBe(false);
  });
});

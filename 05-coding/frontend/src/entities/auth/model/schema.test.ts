// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { describe, expect, it } from "vitest";
import {
  forgotOtpSchema,
  forgotResetSchema,
  loginSchema,
  signupSchema,
} from "./schema";

describe("signupSchema", () => {
  it("rejects submit when terms are not accepted (BD Q1, closed — 02-bd/screens/shared/SHR0101_auth.md, Câu hỏi mở)", () => {
    const result = signupSchema.safeParse({
      displayName: "Learner One",
      password: "password123",
      email: "learner@example.com",
      termsAccepted: false,
    });
    expect(result.success).toBe(false);
  });

  it("accepts a well-formed signup with terms accepted", () => {
    const result = signupSchema.safeParse({
      displayName: "Learner One",
      password: "password123",
      email: "learner@example.com",
      termsAccepted: true,
    });
    expect(result.success).toBe(true);
  });
});

describe("loginSchema", () => {
  it("requires a valid e-mail and a non-empty password", () => {
    expect(loginSchema.safeParse({ email: "", password: "" }).success).toBe(
      false,
    );
    expect(
      loginSchema.safeParse({ email: "learner01", password: "x" }).success,
    ).toBe(false);
    expect(
      loginSchema.safeParse({ email: "learner@example.com", password: "x" })
        .success,
    ).toBe(true);
  });
});

describe("signupSchema display name", () => {
  const withName = (displayName: string) =>
    signupSchema.safeParse({
      displayName,
      password: "password123",
      email: "learner@example.com",
      termsAccepted: true,
    });

  it("keeps the backend range of 2 to 100 characters after trimming", () => {
    expect(withName("A").success).toBe(false);
    expect(withName("  A  ").success).toBe(false);
    expect(withName("Ab").success).toBe(true);
    expect(withName("x".repeat(100)).success).toBe(true);
    expect(withName("x".repeat(101)).success).toBe(false);
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
    const result = forgotResetSchema.safeParse({
      newPassword: "newpassword1",
      confirmPassword: "different",
    });
    expect(result.success).toBe(false);
  });
});

describe("password rule (mirrors the backend, DEC-2026-1030-bcrypt-password-byte-limit)", () => {
  const signup = (password: string) =>
    signupSchema.safeParse({
      displayName: "Learner One",
      password,
      email: "learner@example.com",
      termsAccepted: true,
    });
  const messages = (password: string) =>
    signup(password).error?.issues.map((issue) => issue.message) ?? [];

  it("needs a letter and a digit", () => {
    expect(messages("12345678")).toContain("errors.passwordNeedsLetter");
    expect(messages("abcdefgh")).toContain("errors.passwordNeedsDigit");
    expect(signup("abcdefg1").success).toBe(true);
  });

  it("accepts a Vietnamese letter as the letter", () => {
    expect(signup("ệệệệệệệ1").success).toBe(true);
  });

  it("limits the password to 72 UTF-8 bytes, not 72 characters", () => {
    expect(signup(`${"a".repeat(71)}1`).success).toBe(true);
    expect(messages(`${"a".repeat(72)}1`)).toContain("errors.passwordTooLong");
    // 24 x 3 bytes = 72 bytes plus a digit is 73 bytes, although it is only 25 characters.
    expect(messages(`${"ệ".repeat(24)}1`)).toContain("errors.passwordTooLong");
  });

  it("applies the same rule to the reset form", () => {
    const result = forgotResetSchema.safeParse({
      newPassword: "abcdefgh",
      confirmPassword: "abcdefgh",
    });
    expect(result.error?.issues.map((i) => i.message)).toContain(
      "errors.passwordNeedsDigit",
    );
  });
});

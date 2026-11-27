import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const post = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { post: (...args: unknown[]) => post(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realConfirmEmailChange,
  realRequestEmailChange,
} from "./real-email-change";

describe("real e-mail change API", () => {
  beforeEach(() => {
    post.mockReset();
  });

  it("asks for a code for the new address", async () => {
    post.mockResolvedValue({ pendingEmail: "n@b.vn", expiresInSeconds: 600 });
    expect(await realRequestEmailChange("n@b.vn")).toEqual({ ok: true });
    expect(post).toHaveBeenCalledWith("/identity/me/email-change/request", {
      newEmail: "n@b.vn",
    });
  });

  it("lets a taken address (IDT-415) and a rate limit reach the caller", async () => {
    post.mockRejectedValue(new ApiError("IDT-415", 409));
    await expect(realRequestEmailChange("n@b.vn")).rejects.toMatchObject({
      code: "IDT-415",
    });
  });

  it("confirms with the six-digit code only", async () => {
    post.mockResolvedValue({ email: "n@b.vn" });
    expect(await realConfirmEmailChange("123456")).toEqual({ ok: true });
    expect(post).toHaveBeenCalledWith("/identity/me/email-change/confirm", {
      code: "123456",
    });
  });

  it("turns a wrong or expired code (IDT-416) into a field message", async () => {
    post.mockRejectedValue(new ApiError("IDT-416", 422));
    expect(await realConfirmEmailChange("000000")).toEqual({
      ok: false,
      message: "errors.otpInvalid",
    });
  });

  it("rethrows every other confirm failure", async () => {
    post.mockRejectedValue(new ApiError("IDT-415", 409));
    await expect(realConfirmEmailChange("123456")).rejects.toMatchObject({
      code: "IDT-415",
    });
  });
});

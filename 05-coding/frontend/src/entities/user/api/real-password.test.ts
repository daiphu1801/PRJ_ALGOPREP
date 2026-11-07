import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const post = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { post: (...args: unknown[]) => post(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import { realChangeMyPassword } from "./real-password";

const input = {
  currentPassword: "Tamthoi123",
  newPassword: "Matkhaumoi456",
  confirmPassword: "Matkhaumoi456",
};

describe("realChangeMyPassword", () => {
  beforeEach(() => {
    post.mockReset();
  });

  it("sends only the current and the new password", async () => {
    post.mockResolvedValue({});
    expect(await realChangeMyPassword(input)).toEqual({ ok: true });
    expect(post).toHaveBeenCalledWith("/identity/me/password", {
      currentPassword: "Tamthoi123",
      newPassword: "Matkhaumoi456",
    });
  });

  it("passes on that the notice e-mail failed while the password changed", async () => {
    post.mockResolvedValue({ noticeEmailFailed: true });
    expect(await realChangeMyPassword(input)).toEqual({
      ok: true,
      noticeEmailFailed: true,
    });
  });

  it("turns a wrong current password (IDT-115) into a field message", async () => {
    post.mockRejectedValue(new ApiError("IDT-115", 422));
    expect(await realChangeMyPassword(input)).toEqual({
      ok: false,
      message: "errors.currentPasswordInvalid",
    });
  });

  it("rethrows every other failure", async () => {
    post.mockRejectedValue(new ApiError("IDT-V04", 422));
    await expect(realChangeMyPassword(input)).rejects.toMatchObject({
      code: "IDT-V04",
    });
  });
});

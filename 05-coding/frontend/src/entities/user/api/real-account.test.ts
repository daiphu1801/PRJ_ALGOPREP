import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, tokenStore } from "@/shared/api";

const post = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { post: (...args: unknown[]) => post(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import { realDeleteMyAccount } from "./real-account";

describe("realDeleteMyAccount", () => {
  beforeEach(() => {
    post.mockReset();
    tokenStore.set("jwt");
  });

  it("sends the typed e-mail and drops the access token, since every session is revoked", async () => {
    post.mockResolvedValue({});
    expect(await realDeleteMyAccount("a@b.vn")).toEqual({ ok: true });
    expect(post).toHaveBeenCalledWith("/identity/me/account/delete", {
      emailConfirmation: "a@b.vn",
    });
    expect(tokenStore.get()).toBeNull();
  });

  it("turns a mismatch (IDT-201) into a field message and keeps the session", async () => {
    post.mockRejectedValue(new ApiError("IDT-201", 422));
    expect(await realDeleteMyAccount("x@b.vn")).toEqual({
      ok: false,
      message: "errors.confirmPhraseMismatch",
    });
    expect(tokenStore.get()).toBe("jwt");
  });

  it("rethrows every other failure", async () => {
    post.mockRejectedValue(new ApiError("IDT-503", 503));
    await expect(realDeleteMyAccount("a@b.vn")).rejects.toMatchObject({
      code: "IDT-503",
    });
  });
});

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, tokenStore } from "@/shared/api";

const get = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import { realOAuthCallback, realOAuthStart } from "./real-auth";

describe("real OAuth API", () => {
  beforeEach(() => {
    get.mockReset();
    tokenStore.set(null);
  });
  afterEach(() => tokenStore.set(null));

  it("asks the backend for the provider URL of the chosen provider (#5)", async () => {
    get.mockResolvedValue({
      authorizationUrl: "https://idp.test/auth?state=s",
      state: "s",
    });

    const url = await realOAuthStart("github");

    expect(get).toHaveBeenCalledWith("/identity/auth/oauth/github/redirect");
    expect(url).toBe("https://idp.test/auth?state=s");
  });

  it("finishes the sign-in with code and state, keeps the token in memory and reports the role (#6)", async () => {
    get.mockResolvedValue({
      accessToken: "jwt",
      user: { baseCategory: "STUDENT" },
    });

    const outcome = await realOAuthCallback("google", "the-code", "the-state");

    expect(get).toHaveBeenCalledWith("/identity/auth/oauth/google/callback", {
      code: "the-code",
      state: "the-state",
    });
    expect(outcome).toEqual({ ok: true, role: "STUDENT" });
    expect(tokenStore.get()).toBe("jwt");
  });

  it("hands the restore ticket of a self-deleted account over without signing in (IDT-104)", async () => {
    get.mockRejectedValue(
      new ApiError("IDT-104", 403, "x", { restoreTicket: "tk" }),
    );

    const outcome = await realOAuthCallback("github", "c", "s");

    expect(outcome).toEqual({
      ok: true,
      deactivated: true,
      restoreTicket: "tk",
    });
    expect(tokenStore.get()).toBeNull();
  });

  it.each(["IDT-109", "IDT-110", "IDT-505"])(
    "rethrows %s for the screen to translate",
    async (code) => {
      get.mockRejectedValue(new ApiError(code, 400, "x"));

      await expect(realOAuthCallback("github", "c", "s")).rejects.toMatchObject(
        {
          code,
        },
      );
      expect(tokenStore.get()).toBeNull();
    },
  );

  it("rethrows a deactivated account that carries no ticket", async () => {
    get.mockRejectedValue(new ApiError("IDT-104", 403, "x"));

    await expect(realOAuthCallback("github", "c", "s")).rejects.toMatchObject({
      code: "IDT-104",
    });
  });
});

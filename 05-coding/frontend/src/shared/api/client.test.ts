import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import { tokenStore } from "./token-store";

afterEach(() => {
  vi.restoreAllMocks();
  tokenStore.set(null);
});

describe("API contract", () => {
  it("shares concurrent refresh calls and sends the required cookie/header", async () => {
    const post = vi
      .spyOn(axios, "post")
      .mockResolvedValue({ data: { accessToken: "restored" } });
    const { refreshAccessToken } = await import("./client");
    expect(
      await Promise.all([refreshAccessToken(), refreshAccessToken()]),
    ).toEqual(["restored", "restored"]);
    expect(post).toHaveBeenCalledTimes(1);
    expect(post).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/identity/auth/refresh",
      null,
      {
        withCredentials: true,
        headers: { "X-Requested-With": "XMLHttpRequest" },
      },
    );
  });

  it("returns an anonymous session when refresh fails", async () => {
    vi.spyOn(axios, "post").mockRejectedValue(new Error("expired"));
    const { refreshAccessToken } = await import("./client");
    expect(await refreshAccessToken()).toBeNull();
  });

  it("reads nested DD error codes and retries a 401 only once", async () => {
    vi.resetModules();
    const create = axios.create.bind(axios);
    const client = create();
    const adapter = vi.fn(async (config: InternalAxiosRequestConfig) => {
      throw new AxiosError(
        "unauthorized",
        "ERR_BAD_REQUEST",
        config,
        undefined,
        {
          data: { error: { code: "IDT-105" } },
          status: 401,
          statusText: "Unauthorized",
          headers: {},
          config,
        },
      );
    });
    client.defaults.adapter = adapter;
    vi.spyOn(axios, "create").mockReturnValue(client);
    const post = vi
      .spyOn(axios, "post")
      .mockResolvedValue({ data: { accessToken: "renewed" } });
    const { api } = await import("./client");
    await expect(api.get("/identity/me/profile")).rejects.toMatchObject({
      code: "IDT-105",
      status: 401,
    });
    expect(adapter).toHaveBeenCalledTimes(2);
    expect(post).toHaveBeenCalledTimes(1);
    expect(adapter.mock.calls[1]?.[0].headers.Authorization).toBe(
      "Bearer renewed",
    );
  });
});

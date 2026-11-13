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
    const post = vi.spyOn(axios, "post").mockResolvedValue({
      data: {
        accessToken: `h.${btoa(JSON.stringify({ sub: "A", revision: 2 }))}.s`,
      },
    });
    const { api } = await import("./client");
    const { tokenStore: store } = await import("./token-store");
    store.set(`h.${btoa(JSON.stringify({ sub: "A", revision: 1 }))}.s`);
    await expect(api.get("/identity/me/profile")).rejects.toMatchObject({
      code: "IDT-105",
      status: 401,
    });
    expect(adapter).toHaveBeenCalledTimes(2);
    expect(post).toHaveBeenCalledTimes(1);
    expect(adapter.mock.calls[1]?.[0].headers.Authorization).toBe(
      `Bearer h.${btoa(JSON.stringify({ sub: "A", revision: 2 }))}.s`,
    );
  });
  async function failingClient(url: string, status: number, data: unknown) {
    vi.resetModules();
    const client = axios.create();
    const adapter = vi.fn(async (config: InternalAxiosRequestConfig) => {
      throw new AxiosError("failed", "ERR_BAD_REQUEST", config, undefined, {
        data,
        status,
        statusText: "error",
        headers: {},
        config,
      });
    });
    client.defaults.adapter = adapter;
    vi.spyOn(axios, "create").mockReturnValue(client);
    const refresh = vi.spyOn(axios, "post");
    const { api } = await import("./client");
    return { api, adapter, refresh, url };
  }

  it("does not try to refresh after a 401 from an auth endpoint (wrong password)", async () => {
    const { api, adapter, refresh } = await failingClient(
      "/identity/auth/login",
      401,
      { errorCode: "IDT-102" },
    );
    await expect(api.post("/identity/auth/login", {})).rejects.toMatchObject({
      code: "IDT-102",
      status: 401,
    });
    expect(adapter).toHaveBeenCalledTimes(1);
    expect(refresh).not.toHaveBeenCalled();
  });

  it("keeps the session on a 503 and reports the stable code", async () => {
    tokenStore.set("still-valid");
    const { api, adapter, refresh } = await failingClient(
      "/identity/me/profile",
      503,
      { errorCode: "IDT-503" },
    );
    await expect(api.get("/identity/me/profile")).rejects.toMatchObject({
      code: "IDT-503",
      status: 503,
    });
    expect(adapter).toHaveBeenCalledTimes(1);
    expect(refresh).not.toHaveBeenCalled();
    expect(tokenStore.get()).toBe("still-valid");
  });

  function refreshFailsWith(status: number) {
    const config = { headers: {} } as InternalAxiosRequestConfig;
    return new AxiosError(
      "refresh failed",
      "ERR_BAD_RESPONSE",
      config,
      undefined,
      {
        data: {},
        status,
        statusText: "error",
        headers: {},
        config,
      },
    );
  }

  it("keeps the session when the refresh itself fails with a 503", async () => {
    const { api, refresh } = await failingClient("/identity/me/profile", 401, {
      errorCode: "IDT-105",
    });
    // The client was loaded afresh, so its token store is the one to look at.
    const { tokenStore: store } = await import("./token-store");
    store.set("still-valid");
    refresh.mockRejectedValue(refreshFailsWith(503));
    await expect(api.get("/identity/me/profile")).rejects.toMatchObject({
      status: 401,
    });
    expect(store.get()).toBe("still-valid");
  });

  it("ends the session when the refresh is refused with a 401", async () => {
    const { api, refresh } = await failingClient("/identity/me/profile", 401, {
      errorCode: "IDT-105",
    });
    const { tokenStore: store } = await import("./token-store");
    store.set("old");
    refresh.mockRejectedValue(refreshFailsWith(401));
    await expect(api.get("/identity/me/profile")).rejects.toMatchObject({
      status: 401,
    });
    expect(store.get()).toBeNull();
  });

  it("reads the error code out of a blob error body (file download)", async () => {
    vi.resetModules();
    const client = axios.create();
    client.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
      throw new AxiosError("bad", "ERR_BAD_REQUEST", config, undefined, {
        data: new Blob([JSON.stringify({ errorCode: "IDT-443" })]),
        status: 400,
        statusText: "Bad Request",
        headers: {},
        config,
      });
    };
    vi.spyOn(axios, "create").mockReturnValue(client);
    const { api } = await import("./client");
    await expect(api.download("/x")).rejects.toMatchObject({
      code: "IDT-443",
      status: 400,
    });
  });

  it("downloadFile returns the blob and lets a response header be read, whatever its case", async () => {
    vi.resetModules();
    const client = axios.create();
    client.defaults.adapter = async (config: InternalAxiosRequestConfig) => ({
      data: new Blob(["a,b"]),
      status: 200,
      statusText: "OK",
      headers: { "x-export-truncated": "true" },
      config,
    });
    vi.spyOn(axios, "create").mockReturnValue(client);
    const { api } = await import("./client");
    const file = await api.downloadFile("/x", { params: { scope: "s" } });
    expect(file.blob.size).toBe(3);
    expect(file.header("X-Export-Truncated")).toBe("true");
    expect(file.header("X-Missing")).toBeNull();
    // The old call keeps answering with the bare Blob.
    expect(await api.download("/x")).toBeInstanceOf(Blob);
  });

  it("downloadPost sends the body and returns the file with the name from Content-Disposition", async () => {
    vi.resetModules();
    const client = axios.create();
    let seen: { url?: string; data?: unknown; responseType?: string } = {};
    client.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
      seen = {
        url: config.url,
        data: config.data,
        responseType: config.responseType,
      };
      return {
        data: new Blob(["a,b"]),
        status: 200,
        statusText: "OK",
        headers: { "content-disposition": 'attachment; filename="p-1.csv"' },
        config,
      };
    };
    vi.spyOn(axios, "create").mockReturnValue(client);
    const { api } = await import("./client");
    const file = await api.downloadPost("/x/export", { problemIds: ["a"] });
    expect(file.filename).toBe("p-1.csv");
    expect(seen.url).toBe("/x/export");
    expect(seen.responseType).toBe("blob");
    expect(JSON.parse(String(seen.data))).toEqual({ problemIds: ["a"] });
  });

  it("downloadPost reads the error code out of a blob error body", async () => {
    vi.resetModules();
    const client = axios.create();
    client.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
      throw new AxiosError("bad", "ERR_BAD_REQUEST", config, undefined, {
        data: new Blob([JSON.stringify({ errorCode: "PRB-001" })]),
        status: 400,
        statusText: "Bad Request",
        headers: {},
        config,
      });
    };
    vi.spyOn(axios, "create").mockReturnValue(client);
    const { api } = await import("./client");
    await expect(api.downloadPost("/x", {})).rejects.toMatchObject({
      code: "PRB-001",
      status: 400,
    });
  });
});

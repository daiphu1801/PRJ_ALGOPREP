import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { afterEach, expect, it, vi } from "vitest";

afterEach(async () => {
  (await import("./token-store")).tokenStore.set(null);
  vi.restoreAllMocks();
});

async function pendingRequest() {
  vi.resetModules();
  const client = axios.create();
  let dispatch!: (response: "ok" | "unauthorized") => void;
  let started!: () => void;
  const ready = new Promise<void>((resolve) => {
    started = resolve;
  });
  client.defaults.adapter = (config: InternalAxiosRequestConfig) =>
    new Promise((resolve, reject) => {
      dispatch = (kind) => {
        const response = {
          data: "private A",
          status: kind === "ok" ? 200 : 401,
          statusText: "result",
          headers: {},
          config,
        };
        if (kind === "ok") resolve(response);
        else
          reject(
            new AxiosError(
              "unauthorized",
              "ERR_BAD_REQUEST",
              config,
              undefined,
              response,
            ),
          );
      };
      started();
    });
  vi.spyOn(axios, "create").mockReturnValue(client);
  const { api, refreshAccessToken } = await import("./client");
  const { tokenStore } = await import("./token-store");
  tokenStore.set("token-A");
  return {
    api,
    tokenStore,
    ready,
    dispatch: (kind: "ok" | "unauthorized") => dispatch(kind),
    refreshAccessToken,
  };
}

it("rejects a late successful HTTP response after the active session changed", async () => {
  const task = await pendingRequest();
  const request = task.api.get("/identity/me/profile");
  const rejected = expect(request).rejects.toMatchObject({
    code: "ERR_CANCELED",
  });
  await task.ready;
  task.tokenStore.set("token-B");
  task.dispatch("ok");
  await rejected;
  expect(task.tokenStore.get()).toBe("token-B");
});

it("captures the original session before a same-turn account switch", async () => {
  const task = await pendingRequest();
  const request = task.api.post("/identity/me/profile", { displayName: "private A edit" });
  const rejected = expect(request).rejects.toMatchObject({ code: "ERR_CANCELED" });
  task.tokenStore.set("token-B");
  await task.ready;
  task.dispatch("ok");
  await rejected;
  expect(task.tokenStore.get()).toBe("token-B");
});

it("does not refresh A's expired request under B's session", async () => {
  const task = await pendingRequest();
  const refresh = vi.spyOn(axios, "post");
  const request = task.api.get("/judge/me/submissions");
  const rejected = expect(request).rejects.toMatchObject({
    code: "ERR_CANCELED",
  });
  await task.ready;
  task.tokenStore.set("token-B");
  task.dispatch("unauthorized");
  await rejected;
  expect(refresh).not.toHaveBeenCalled();
  expect(task.tokenStore.get()).toBe("token-B");
});

it("cannot restore an old refresh token into the new session", async () => {
  const task = await pendingRequest();
  let finish!: (value: unknown) => void;
  vi.spyOn(axios, "post").mockImplementationOnce(
    () =>
      new Promise((resolve) => {
        finish = resolve;
      }),
  );
  const pending = task.refreshAccessToken();
  task.tokenStore.set(null);
  task.tokenStore.set("token-B");
  finish({ data: { accessToken: "renewed-A" } });
  expect(await pending).toBeNull();
  expect(task.tokenStore.get()).toBe("token-B");
});

it("does not replay a mutation if the refresh cookie now belongs to a different account", async () => {
  const task = await pendingRequest();
  const refreshedB = `header.${btoa(JSON.stringify({ sub: "B" }))}.signature`;
  vi.spyOn(axios, "post").mockResolvedValue({ data: { accessToken: refreshedB } });
  const request = task.api.post("/identity/me/profile", { displayName: "private A edit" });
  const rejected = expect(request).rejects.toMatchObject({ code: "ERR_CANCELED" });
  await task.ready;
  task.dispatch("unauthorized");
  await rejected;
  expect(task.tokenStore.get()).toBe(refreshedB);
});

it("forwards AbortSignal for preview and preserves the cancellation error", async () => {
  const task = await pendingRequest();
  const abort = new AbortController();
  const request = task.api.post(
    "/harness/function-signature/preview",
    {},
    { signal: abort.signal },
  );
  const rejected = expect(request).rejects.toMatchObject({
    code: "ERR_CANCELED",
  });
  await task.ready;
  abort.abort();
  task.dispatch("ok");
  await rejected;
});

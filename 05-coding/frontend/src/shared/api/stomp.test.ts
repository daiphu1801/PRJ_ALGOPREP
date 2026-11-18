import { beforeEach, expect, it, vi } from "vitest";
type Configuration = {
  beforeConnect: () => Promise<void>;
  onConnect: () => void;
  onStompError: () => void;
  onWebSocketClose: () => void;
};
let configuration: Configuration;
let headers: Record<string, string>;
const activate = vi.fn();
const deactivate = vi.fn();
const subscribe = vi.fn();
const refresh = vi.fn();
vi.mock("@stomp/stompjs", () => ({
  Client: class {
    constructor(config: Configuration) {
      configuration = config;
    }
    set connectHeaders(value: Record<string, string>) {
      headers = value;
    }
    activate = activate;
    deactivate = deactivate;
    subscribe = subscribe;
  },
}));
vi.mock("./client", () => ({ refreshAccessToken: () => refresh() }));
import { tokenStore } from "./token-store";
import { subscribeJudge } from "./stomp";
beforeEach(() => {
  activate.mockReset();
  deactivate.mockReset().mockResolvedValue(undefined);
  subscribe.mockReset();
  refresh.mockReset().mockResolvedValue("new-token");
  tokenStore.set("token");
});
it("subscribes again before the reconnect snapshot and attaches the token to CONNECT", async () => {
  const order: string[] = [];
  subscribe.mockImplementation(() => order.push("subscribe"));
  const stop = subscribeJudge("/user/queue/submissions", {
    message: vi.fn(),
    connected: () => order.push("snapshot"),
    disconnected: vi.fn(),
  });
  await configuration.beforeConnect();
  expect(headers).toEqual({ Authorization: "Bearer token" });
  configuration.onConnect();
  configuration.onConnect();
  expect(order).toEqual(["subscribe", "snapshot", "subscribe", "snapshot"]);
  stop();
  expect(deactivate).toHaveBeenCalledWith({ force: true });
});
it("refreshes a rejected token before reconnect and never dispatches after disposal", async () => {
  const connected = vi.fn();
  const disconnected = vi.fn();
  const stop = subscribeJudge("/topic/admin/queue", {
    message: vi.fn(),
    connected,
    disconnected,
  });
  configuration.onStompError();
  await configuration.beforeConnect();
  expect(refresh).toHaveBeenCalledTimes(1);
  expect(headers).toEqual({ Authorization: "Bearer new-token" });
  stop();
  configuration.onConnect();
  configuration.onWebSocketClose();
  expect(connected).not.toHaveBeenCalled();
  expect(disconnected).toHaveBeenCalledTimes(1);
});

import { Client } from "@stomp/stompjs";
import { env } from "./config";
import { refreshAccessToken } from "./client";
import { tokenStore } from "./token-store";

/** Each reconnect registers the destination again before requesting a REST snapshot. */
export function subscribeJudge(
  destination: "/user/queue/submissions" | "/topic/admin/queue",
  callbacks: {
    message: (value: unknown) => void;
    connected: () => void;
    disconnected: () => void;
  },
) {
  let stopped = false;
  let refresh = false;
  const client = new Client({
    brokerURL: env.wsBaseUrl,
    reconnectDelay: 3000,
    connectionTimeout: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    // Never enable protocol debug: CONNECT includes a bearer token.
    debug: () => {},
    beforeConnect: async () => {
      const token =
        refresh || !tokenStore.get()
          ? await refreshAccessToken()
          : tokenStore.get();
      refresh = false;
      if (stopped || !token) {
        await client.deactivate();
        return;
      }
      client.connectHeaders = { Authorization: `Bearer ${token}` };
    },
    onConnect: () => {
      if (stopped) return;
      client.subscribe(destination, (frame) => {
        if (stopped) return;
        try {
          callbacks.message(JSON.parse(frame.body));
        } catch {
          /* malformed hint: REST remains authoritative */
        }
      });
      callbacks.connected();
    },
    onWebSocketClose: () => {
      if (!stopped) callbacks.disconnected();
    },
    onStompError: () => {
      refresh = true;
      if (!stopped) callbacks.disconnected();
    },
  });
  client.activate();
  return () => {
    stopped = true;
    void client.deactivate({ force: true });
  };
}

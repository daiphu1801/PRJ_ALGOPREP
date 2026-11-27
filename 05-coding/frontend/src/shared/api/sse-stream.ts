// Server-Sent Events over `fetch`, because the native EventSource cannot send the Bearer header
// (03-dd/api/F1_identity.md #66, DEC-2026-1101-audit-stream-sse). One call opens one stream and
// keeps it open: it reconnects with backoff, sends `Last-Event-ID` itself, refreshes the token once
// on a 401, and stops for good on any other refusal (403, 429 ...), reporting it as an ApiError.
import { env } from "@/shared/api/config";
import { toApiError, type ApiError, type ErrorBody } from "./api-error";
import { refreshAccessToken } from "./client";
import { tokenStore } from "./token-store";

export type SseMessage = { event: string; data: string; id?: string };
export type SseStatus = "open" | "reconnecting";

type Options = {
  onMessage: (message: SseMessage) => void;
  onStatus?: (status: SseStatus) => void;
  /** The server refused the stream (not a network drop); no further attempt is made. */
  onRefused?: (error: ApiError) => void;
};

const BACKOFF_START_MS = 1_000;
const BACKOFF_MAX_MS = 30_000;
// The server sends a heartbeat every 30 s; twice that with nothing received means a dead connection.
const IDLE_LIMIT_MS = 65_000;

/** Splits the text stream into messages; a message ends at a blank line, `:` lines are comments. */
export function createSseParser(dispatch: (message: SseMessage) => void) {
  let buffer = "";
  return (chunk: string) => {
    buffer += chunk;
    for (;;) {
      const end = /\r?\n\r?\n/.exec(buffer);
      if (!end) return;
      const block = buffer.slice(0, end.index);
      buffer = buffer.slice(end.index + end[0].length);
      let event = "";
      let id: string | undefined;
      const data: string[] = [];
      for (const line of block.split(/\r?\n/)) {
        if (line === "" || line.startsWith(":")) continue;
        const colon = line.indexOf(":");
        const field = colon === -1 ? line : line.slice(0, colon);
        const raw = colon === -1 ? "" : line.slice(colon + 1);
        const value = raw.startsWith(" ") ? raw.slice(1) : raw;
        if (field === "event") event = value;
        else if (field === "id") id = value;
        else if (field === "data") data.push(value);
      }
      if (event !== "" || data.length > 0) {
        dispatch({ event: event || "message", data: data.join("\n"), id });
      }
    }
  };
}

/** Opens the stream; the returned function closes it (unmount, toggle off). */
export function openSseStream(path: string, options: Options): () => void {
  const stop = new AbortController();
  void run(path, options, stop.signal);
  return () => stop.abort();
}

function sleep(ms: number, signal: AbortSignal) {
  return new Promise<void>((resolve) => {
    const timer = setTimeout(resolve, ms);
    signal.addEventListener(
      "abort",
      () => {
        clearTimeout(timer);
        resolve();
      },
      { once: true },
    );
  });
}

async function run(path: string, options: Options, signal: AbortSignal) {
  const url = `${env.apiBaseUrl.replace(/\/$/, "")}${path}`;
  let lastId: string | undefined;
  let delay = BACKOFF_START_MS;
  let refreshed = false;

  while (!signal.aborted) {
    const connection = new AbortController();
    const stopConnection = () => connection.abort();
    signal.addEventListener("abort", stopConnection, { once: true });
    let idle: ReturnType<typeof setTimeout> | undefined;
    try {
      const headers: Record<string, string> = {
        Accept: "text/event-stream",
      };
      const token = tokenStore.get();
      if (token) headers.Authorization = `Bearer ${token}`;
      if (lastId) headers["Last-Event-ID"] = lastId;

      const response = await fetch(url, {
        headers,
        credentials: "include",
        signal: connection.signal,
      });
      if (response.status === 401 && !refreshed) {
        refreshed = true;
        const renewed = await refreshAccessToken();
        if (renewed) {
          tokenStore.set(renewed);
          continue;
        }
        tokenStore.set(null);
      }
      if (!response.ok || !response.body) {
        const body = (await response.json().catch(() => undefined)) as
          ErrorBody | undefined;
        options.onRefused?.(toApiError(response.status, body));
        return;
      }

      refreshed = false;
      delay = BACKOFF_START_MS;
      options.onStatus?.("open");
      const parse = createSseParser((message) => {
        if (message.id) lastId = message.id;
        options.onMessage(message);
      });
      const decoder = new TextDecoder("utf-8");
      const reader = response.body.getReader();
      for (;;) {
        clearTimeout(idle);
        idle = setTimeout(stopConnection, IDLE_LIMIT_MS);
        const { done, value } = await reader.read();
        if (done) break;
        parse(decoder.decode(value, { stream: true }));
      }
    } catch {
      // A network drop, an idle timeout or an abort: all end in the check below.
    } finally {
      clearTimeout(idle);
      signal.removeEventListener("abort", stopConnection);
    }
    if (signal.aborted) return;
    options.onStatus?.("reconnecting");
    await sleep(delay, signal);
    delay = Math.min(delay * 2, BACKOFF_MAX_MS);
  }
}

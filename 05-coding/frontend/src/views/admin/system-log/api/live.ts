// The live toggle of the log (F1 #66): while it is on, the stream puts new rows at the top of the
// list the filters describe. The list is the React Query cache; the stream only prepends to its first
// page (dedupe by UUID). A `resync` event, or coming back after a drop, reloads the list instead of
// guessing what was missed (03-dd/screens/admin/ADM0403_system_log.md BR-SC-01).
import { useEffect, useRef, useState } from "react";
import { useQueryClient, type InfiniteData } from "@tanstack/react-query";
import {
  isMockMode,
  openSseStream,
  type ApiError,
  type SseStatus,
} from "@/shared/api";
import { matchesFilters } from "../model/filters";
import type { AuditLogParams, AuditLogResultPage } from "../model/types";
import { AUDIT_LOG_LIST_KEY, auditLogKey } from "./queries";
import { toAuditEvent, type AuditLogItemDto } from "./real-audit-log";

type Cache = InfiniteData<AuditLogResultPage, string | null>;

// A long-open tab keeps the newest rows only; older ones come back with "Tải thêm" after a reload.
const MAX_LIVE_ROWS = 500;

/** `onRefused` is told once when the server will not open the stream (403, 429); the toggle stays as it is. */
export function useAuditLive(
  enabled: boolean,
  params: AuditLogParams,
  onRefused: (error: ApiError) => void,
) {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<SseStatus>("open");
  const [received, setReceived] = useState(0);
  // The stream is opened once per toggle; the callbacks read the filters of the moment.
  const paramsRef = useRef(params);
  const refusedRef = useRef(onRefused);
  useEffect(() => {
    paramsRef.current = params;
    refusedRef.current = onRefused;
  }, [params, onRefused]);

  useEffect(() => {
    // No stream in mock mode: there is no backend to hold it.
    if (!enabled || isMockMode()) return;
    let dropped = false;
    const reload = () =>
      void queryClient.invalidateQueries({ queryKey: AUDIT_LOG_LIST_KEY });
    const close = openSseStream("/identity/admin/audit-logs/stream", {
      onStatus: (next) => {
        setStatus(next);
        if (next === "reconnecting") dropped = true;
        else if (dropped) {
          dropped = false;
          reload();
        }
      },
      onRefused: (error) => refusedRef.current(error),
      onMessage: ({ event, data }) => {
        if (event === "resync") return reload();
        if (event !== "audit-log") return;
        const row = toAuditEvent(JSON.parse(data) as AuditLogItemDto);
        if (!matchesFilters(row, paramsRef.current)) return;
        queryClient.setQueryData<Cache>(
          auditLogKey(paramsRef.current),
          (old) => {
            const first = old?.pages[0];
            if (!old || !first) return old;
            if (old.pages.some((p) => p.events.some((e) => e.id === row.id))) {
              return old;
            }
            return {
              ...old,
              pages: [
                {
                  ...first,
                  events: [row, ...first.events].slice(0, MAX_LIVE_ROWS),
                },
                ...old.pages.slice(1),
              ],
            };
          },
        );
        setReceived((count) => count + 1);
      },
    });
    return () => {
      close();
      setStatus("open");
    };
  }, [enabled, queryClient]);

  return { status, received };
}

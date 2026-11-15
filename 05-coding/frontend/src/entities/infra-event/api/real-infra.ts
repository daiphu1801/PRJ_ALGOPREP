import { api } from "@/shared/api";
import type { InfraEvent, InfraLevel } from "../model/types";

export type InfraFilters = {
  q: string;
  level: InfraLevel | "all";
  service: string;
  /** "YYYY-MM-DD" or "". */
  from: string;
  to: string;
};

type RawPage = {
  events: {
    occurredAt: string;
    level: string;
    message: string;
    service: string;
  }[];
  nextCursor: string | null;
  services: string[];
};

// en-CA gives "YYYY-MM-DD", en-GB with hour12 off gives "HH:mm:ss".
const ZONE = "Asia/Ho_Chi_Minh";
const DAY = new Intl.DateTimeFormat("en-CA", { timeZone: ZONE });
const CLOCK = new Intl.DateTimeFormat("en-GB", {
  timeZone: ZONE,
  hour12: false,
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
});

export async function readInfraEvents(filters: InfraFilters, cursor?: string) {
  const page = await api.get<RawPage>("/judge/admin/infra-events", {
    q: filters.q,
    level: filters.level === "all" ? undefined : filters.level.toUpperCase(),
    service: filters.service === "all" ? undefined : filters.service,
    from: filters.from || undefined,
    to: filters.to || undefined,
    cursor,
  });
  return {
    nextCursor: page.nextCursor,
    services: page.services,
    // The server sends no id; the caller numbers rows across pages.
    events: page.events.map((raw): Omit<InfraEvent, "id"> => {
      const at = new Date(raw.occurredAt);
      return {
        date: DAY.format(at),
        time: CLOCK.format(at),
        level: raw.level.toLowerCase() as InfraLevel,
        message: raw.message,
        service: raw.service,
      };
    }),
  };
}

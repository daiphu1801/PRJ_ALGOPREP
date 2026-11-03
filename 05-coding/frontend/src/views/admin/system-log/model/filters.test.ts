import { describe, expect, it } from "vitest";
import { matchesFilters } from "./filters";
import type { AuditEvent } from "./types";

const row: AuditEvent = {
  id: "9f2a53c1-0000",
  shortId: "9f2a53c",
  date: "2026-11-02",
  time: "10:00:00",
  category: "auth",
  service: "auth",
  actor: "Phú Đại",
};
const open = { query: "", category: "all", from: "", to: "" } as const;

describe("matchesFilters", () => {
  it("accepts a row when no filter is set", () => {
    expect(matchesFilters(row, open)).toBe(true);
  });

  it("checks the category, and the day range inclusive on both ends", () => {
    expect(matchesFilters(row, { ...open, category: "config" })).toBe(false);
    expect(
      matchesFilters(row, { ...open, from: "2026-11-02", to: "2026-11-02" }),
    ).toBe(true);
    expect(matchesFilters(row, { ...open, from: "2026-11-03" })).toBe(false);
    expect(matchesFilters(row, { ...open, to: "2026-11-01" })).toBe(false);
  });

  it("searches the actor's e-mail, the event code as an id prefix and the service, not the message", () => {
    const real = { ...row, actorEmail: "dai@example.com" };
    expect(matchesFilters(real, { ...open, query: " DAI@exa " })).toBe(true);
    // The display name is not searched once an e-mail is known: the server does not either.
    expect(matchesFilters(real, { ...open, query: "Phú Đại" })).toBe(false);
    expect(matchesFilters(real, { ...open, query: "9f2a5" })).toBe(true);
    expect(matchesFilters(real, { ...open, query: "evt_9f2a5" })).toBe(true);
    expect(matchesFilters(real, { ...open, query: "f2a5" })).toBe(false);
    expect(matchesFilters(real, { ...open, query: "AUT" })).toBe(true);
    expect(matchesFilters(real, { ...open, query: "nobody" })).toBe(false);
  });

  it("falls back to the actor's name for rows without an e-mail (the mock data)", () => {
    expect(matchesFilters(row, { ...open, query: "đại" })).toBe(true);
  });
});

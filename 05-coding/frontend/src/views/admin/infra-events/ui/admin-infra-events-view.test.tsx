// ADM0404: filters combine with AND, "load more" reveals the next page, a reversed day range is empty.
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, values?: Record<string, string | number>) =>
    values ? `${key}:${Object.values(values).join(",")}` : key,
}));

import { AdminInfraEventsView } from "./admin-infra-events-view";

const rows = () =>
  screen
    .getAllByRole("listitem")
    .filter((item) => item.className.includes("border-l-2"));

describe("AdminInfraEventsView", () => {
  it("shows 20 rows first and 20 more per click", () => {
    render(<AdminInfraEventsView />);
    expect(rows()).toHaveLength(20);
    fireEvent.click(screen.getByRole("button", { name: "loadMore:20" }));
    expect(rows()).toHaveLength(22);
    expect(screen.getByRole("button", { name: "loadMore:20" })).toBeDisabled();
  });

  it("filters by day range, inclusive on both ends", () => {
    render(<AdminInfraEventsView />);
    fireEvent.change(screen.getByLabelText("dateFrom"), {
      target: { value: "2026-10-09" },
    });
    fireEvent.change(screen.getByLabelText("dateTo"), {
      target: { value: "2026-10-09" },
    });
    expect(rows()).toHaveLength(6);
  });

  it("combines the search box with the day range", () => {
    render(<AdminInfraEventsView />);
    fireEvent.change(screen.getByLabelText("searchLabel"), {
      target: { value: "outbox" },
    });
    expect(rows()).toHaveLength(9);
    fireEvent.change(screen.getByLabelText("dateFrom"), {
      target: { value: "2026-10-10" },
    });
    expect(rows()).toHaveLength(4);
  });

  it("starts on the level given by initialLevel", () => {
    render(<AdminInfraEventsView initialLevel="error" />);
    expect(rows().length).toBeGreaterThan(0);
    expect(
      rows().every((row) => row.textContent?.includes("level.error")),
    ).toBe(true);
  });

  it("only lists the two real sources", () => {
    render(<AdminInfraEventsView />);
    fireEvent.click(screen.getByRole("button", { name: "loadMore:20" }));
    const text = rows()
      .map((row) => row.textContent)
      .join(" ");
    expect(text).not.toMatch(/ai-gateway|redis|minio|\bdb\b/);
  });

  it("returns nothing when the range is reversed", () => {
    render(<AdminInfraEventsView />);
    fireEvent.change(screen.getByLabelText("dateTo"), {
      target: { value: "2026-10-01" },
    });
    fireEvent.change(screen.getByLabelText("dateFrom"), {
      target: { value: "2026-10-05" },
    });
    expect(screen.getByText("emptyFiltered")).toBeInTheDocument();
  });
});

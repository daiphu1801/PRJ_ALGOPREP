import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AgendaList } from "./agenda-list";

describe("AgendaList", () => {
  it("draws only the days that have entries and marks today", () => {
    render(
      <AgendaList
        emptyLabel="Chưa có lịch"
        days={[
          {
            key: "1",
            label: "T2",
            highlighted: true,
            entries: [
              { id: "a", time: "19:00", title: "Lớp Java", meta: "B203" },
            ],
          },
          { key: "2", label: "T3", entries: [] },
        ]}
      />,
    );

    expect(screen.getByText("Lớp Java")).toBeInTheDocument();
    expect(screen.getByText("19:00")).toBeInTheDocument();
    expect(screen.getByText("T2")).toHaveAttribute("aria-current", "date");
    expect(screen.queryByText("T3")).not.toBeInTheDocument();
  });

  it("shows the empty label when no day has an entry", () => {
    render(
      <AgendaList
        emptyLabel="Chưa có lịch"
        days={[{ key: "1", label: "T2", entries: [] }]}
      />,
    );
    expect(screen.getByText("Chưa có lịch")).toBeInTheDocument();
  });
});

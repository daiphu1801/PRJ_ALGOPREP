// Queue screen after the 2026-10-10 redesign: count tabs, pagination by the saved rows-per-page,
// and the confirmation before the two risky cluster changes. `useT` returns the key.
import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string) => key,
}));

import { AdminQueueMonitorView } from "./admin-queue-monitor-view";

const bodyRows = () =>
  within(screen.getByRole("table", { name: "jobsTitle" }))
    .getAllByRole("row")
    .slice(1);

describe("AdminQueueMonitorView", () => {
  it("opens on the waiting tab, one page at a time", () => {
    render(<AdminQueueMonitorView />);
    // 21 waiting jobs in the mock, 20 rows per page by default.
    expect(bodyRows()).toHaveLength(20);
    fireEvent.click(screen.getByRole("button", { name: "pageNext" }));
    expect(bodyRows()).toHaveLength(1);
  });

  it("the failed tab shows only failed jobs", () => {
    render(<AdminQueueMonitorView />);
    fireEvent.click(screen.getByRole("button", { name: "tabFailed" }));
    expect(bodyRows()).toHaveLength(1);
    expect(bodyRows()[0]).toHaveTextContent("jobStatus.sandboxError");
  });

  it("asks before pausing, and only pauses once confirmed", () => {
    render(<AdminQueueMonitorView />);
    const pause = screen.getByRole("switch", { name: "control.paused.label" });

    fireEvent.click(pause);
    expect(pause).toHaveAttribute("aria-checked", "false");
    fireEvent.click(screen.getByRole("button", { name: "confirm.no" }));
    expect(pause).toHaveAttribute("aria-checked", "false");

    fireEvent.click(pause);
    fireEvent.click(screen.getByRole("button", { name: "confirm.yes" }));
    expect(pause).toHaveAttribute("aria-checked", "true");
    expect(screen.getByText("state.paused")).toBeInTheDocument();

    // Resuming is the safe direction and applies at once.
    fireEvent.click(pause);
    expect(pause).toHaveAttribute("aria-checked", "false");
  });

  it("asks before turning autoscale off, not before turning it on", () => {
    render(<AdminQueueMonitorView />);
    const autoscale = screen.getByRole("switch", {
      name: "control.autoscale.label",
    });
    fireEvent.click(autoscale);
    expect(autoscale).toHaveAttribute("aria-checked", "true");
    fireEvent.click(screen.getByRole("button", { name: "confirm.yes" }));
    expect(autoscale).toHaveAttribute("aria-checked", "false");

    fireEvent.click(autoscale);
    expect(autoscale).toHaveAttribute("aria-checked", "true");
  });

  it("links to the infrastructure events screen instead of listing them", () => {
    render(<AdminQueueMonitorView />);
    expect(screen.getByRole("link", { name: "infraLink" })).toHaveAttribute(
      "href",
      "/admin/infra-events?level=error",
    );
    expect(screen.queryByRole("list", { name: "infraTitle" })).toBeNull();
  });
});

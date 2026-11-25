import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { toast } from "@/shared/lib/toast-store";
import { Toaster } from "./toaster";

const labels = {
  region: "Notifications",
  dismiss: "Close",
  tone: {
    success: "Success",
    error: "Error",
    warning: "Warning",
    info: "Info",
  },
};

beforeEach(() => vi.useFakeTimers());
afterEach(() => {
  act(() => toast.clear());
  vi.useRealTimers();
});

describe("Toaster", () => {
  it("shows a toast in a labelled region, errors as alerts and successes as status", () => {
    render(<Toaster labels={labels} />);
    act(() => {
      toast.success("Saved");
      toast.error("Failed");
    });
    expect(
      screen.getByRole("region", { name: "Notifications" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Saved");
    expect(screen.getByRole("alert")).toHaveTextContent("Failed");
  });

  it("closes from the X button", () => {
    render(<Toaster labels={labels} />);
    act(() => {
      toast.info("Hello");
    });
    fireEvent.click(screen.getByRole("button", { name: "Close" }));
    // The slide-out plays first; the card is removed once it has finished.
    expect(screen.getByText("Hello")).toBeInTheDocument();
    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(screen.queryByText("Hello")).not.toBeInTheDocument();
  });

  it("dismisses itself after its duration, later for errors", () => {
    render(<Toaster labels={labels} />);
    act(() => {
      toast.success("Quick");
      toast.error("Slow");
    });
    // Two steps: React only schedules the slide-out timer once the first state update has flushed.
    act(() => {
      vi.advanceTimersByTime(4100);
    });
    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(screen.queryByText("Quick")).not.toBeInTheDocument();
    expect(screen.getByText("Slow")).toBeInTheDocument();
    act(() => {
      vi.advanceTimersByTime(3000);
    });
    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(screen.queryByText("Slow")).not.toBeInTheDocument();
  });

  it("lets clicks through its body, so it never sits over a button", () => {
    render(<Toaster labels={labels} />);
    act(() => {
      toast.success("Through");
    });
    expect(screen.getByRole("status")).toHaveClass("pointer-events-none");
    expect(screen.getByRole("button", { name: "Close" })).toHaveClass(
      "pointer-events-auto",
    );
  });

  it("a repeated message does not restart the card's timer", () => {
    render(<Toaster labels={labels} />);
    act(() => {
      toast.warning("Typing");
    });
    for (let step = 0; step < 6; step++) {
      act(() => {
        vi.advanceTimersByTime(1000);
      });
      act(() => {
        toast.warning("Typing");
      });
    }
    act(() => {
      vi.advanceTimersByTime(1100);
    });
    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(screen.queryByText("Typing")).not.toBeInTheDocument();
  });

  it("holds a toast open while its X is pointed at", () => {
    render(<Toaster labels={labels} />);
    act(() => {
      toast.success("Held");
    });
    fireEvent.mouseEnter(screen.getByRole("button", { name: "Close" }));
    act(() => {
      vi.advanceTimersByTime(10000);
    });
    expect(screen.getByText("Held")).toBeInTheDocument();
    fireEvent.mouseLeave(screen.getByRole("button", { name: "Close" }));
    // Two steps: React only schedules the slide-out timer once the first state update has flushed.
    act(() => {
      vi.advanceTimersByTime(4100);
    });
    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(screen.queryByText("Held")).not.toBeInTheDocument();
  });
});

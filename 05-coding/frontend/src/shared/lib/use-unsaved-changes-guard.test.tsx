// The guard intercepts in-app link clicks only while `dirty`, and `leave` navigates after the user
// confirms. Browser Back / tab close (popstate, beforeunload) are not exercised in jsdom.
import { describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { useUnsavedChangesGuard } from "./use-unsaved-changes-guard";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

function Harness({ dirty }: { dirty: boolean }) {
  const guard = useUnsavedChangesGuard(dirty);
  return (
    <div>
      <a href="/elsewhere">go</a>
      <a href="https://example.com/x">external</a>
      <a href="/new-tab" target="_blank">
        blank
      </a>
      <p data-testid="pending">{String(guard.pending)}</p>
      <button onClick={guard.stay}>stay</button>
      <button onClick={guard.leave}>leave</button>
    </div>
  );
}

describe("useUnsavedChangesGuard", () => {
  it("lets a link through when nothing is unsaved", () => {
    render(<Harness dirty={false} />);
    const click = fireEvent.click(screen.getByText("go"));
    expect(click).toBe(true);
    expect(screen.getByTestId("pending")).toHaveTextContent("false");
  });

  it("holds an in-app link while dirty, then navigates on leave", () => {
    push.mockClear();
    render(<Harness dirty />);
    const notPrevented = fireEvent.click(screen.getByText("go"));
    expect(notPrevented).toBe(false);
    expect(screen.getByTestId("pending")).toHaveTextContent("true");
    expect(push).not.toHaveBeenCalled();

    act(() => screen.getByText("leave").click());
    expect(push).toHaveBeenCalledWith("/elsewhere");
    expect(screen.getByTestId("pending")).toHaveTextContent("false");
  });

  it("stay clears the prompt without navigating", () => {
    push.mockClear();
    render(<Harness dirty />);
    fireEvent.click(screen.getByText("go"));
    act(() => screen.getByText("stay").click());
    expect(screen.getByTestId("pending")).toHaveTextContent("false");
    expect(push).not.toHaveBeenCalled();
  });

  it("ignores external and new-tab links", () => {
    render(<Harness dirty />);
    expect(fireEvent.click(screen.getByText("external"))).toBe(true);
    expect(fireEvent.click(screen.getByText("blank"))).toBe(true);
    expect(screen.getByTestId("pending")).toHaveTextContent("false");
  });
});

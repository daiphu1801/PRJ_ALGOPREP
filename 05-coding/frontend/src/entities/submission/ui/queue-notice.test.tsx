// SR-AC-11 / PD-AC-14: the three display states of the F4-14 queue hint.
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { QueueNotice } from "./queue-notice";

const t = (
  key: string,
  values: { ahead: number; seconds: number },
) => `${key}:${values.ahead}:${values.seconds}`;

describe("QueueNotice", () => {
  it("shows the ahead count and the estimate", () => {
    render(<QueueNotice queue={{ ahead: 18, estimatedWaitSeconds: 9 }} t={t} />);
    expect(screen.getByRole("status")).toHaveTextContent(
      "waitingWithEstimate:18:9",
    );
  });
  it("shows only the count when the estimate is null", () => {
    render(<QueueNotice queue={{ ahead: 4, estimatedWaitSeconds: null }} t={t} />);
    expect(screen.getByRole("status")).toHaveTextContent("waiting:4");
  });
  it("shows the about-to-start line when nobody is ahead", () => {
    render(<QueueNotice queue={{ ahead: 0, estimatedWaitSeconds: 0 }} t={t} />);
    expect(screen.getByRole("status")).toHaveTextContent("aboutToStart");
  });
  it("renders nothing when queue is null or missing", () => {
    const { container, rerender } = render(<QueueNotice queue={null} t={t} />);
    expect(container).toBeEmptyDOMElement();
    rerender(<QueueNotice queue={undefined} t={t} />);
    expect(container).toBeEmptyDOMElement();
  });
});

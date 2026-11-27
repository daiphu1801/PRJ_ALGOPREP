// The real "Luồng chấm" block (DD ADM0401 BR-SC-03, BR-SC-04): numbers from the worker report, the
// throughput from submissions, and no number at all when the worker has not reported.
// `useT` returns the key plus its params so the assertions can see what was interpolated.
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key}|${JSON.stringify(params)}` : key,
}));

import { GradingCard as GradingRows } from "./grading-card";
import type { GradingBlock } from "../model/types";

const at = Date.parse("2026-11-22T11:24:28Z");
const block = (patch: Partial<GradingBlock> = {}): GradingBlock => ({
  consumers: {
    target: 5,
    active: 4,
    max: 8,
    lastScaling: { at, from: 4, to: 5, reason: "BACKLOG", pendingCount: 22 },
  },
  finishedLastMinute: 31,
  estimatedClearSeconds: 42,
  ...patch,
});
const value = (label: string) =>
  screen.getByText(label).nextElementSibling?.textContent ?? "";

function GradingCard({ grading }: { grading: GradingBlock }) {
  return (
    <dl>
      <GradingRows grading={grading} />
      <GradingRows grading={grading} details />
    </dl>
  );
}

describe("GradingCard", () => {
  it("shows running over maximum, the target, the last change and the throughput", () => {
    render(<GradingCard grading={block()} />);
    expect(value("grading.running")).toContain('"active":4,"max":8');
    expect(value("grading.target")).toBe("5");
    const last = value("grading.lastScaling");
    expect(last).toContain("grading.scaledUp");
    expect(last).toContain('"from":4,"to":5');
    expect(last).toContain('"reason":"grading.reason.BACKLOG"');
    expect(last).toContain('"pending":22');
    expect(value("grading.throughput")).toContain('"count":31');
    expect(screen.queryByText("grading.noReport")).toBeNull();
  });

  it("words a decrease as scaled down with its own reason", () => {
    render(
      <GradingCard
        grading={block({
          consumers: {
            target: 4,
            active: 4,
            max: 8,
            lastScaling: {
              at,
              from: 5,
              to: 4,
              reason: "IDLE",
              pendingCount: 0,
            },
          },
        })}
      />,
    );
    const last = value("grading.lastScaling");
    expect(last).toContain("grading.scaledDown");
    expect(last).toContain('"reason":"grading.reason.IDLE"');
  });

  it("names the turned-off reason when autoscale was disabled", () => {
    render(
      <GradingCard
        grading={block({
          consumers: {
            target: 1,
            active: 1,
            max: 8,
            lastScaling: {
              at,
              from: 2,
              to: 1,
              reason: "DISABLED",
              pendingCount: 3,
            },
          },
        })}
      />,
    );
    expect(value("grading.lastScaling")).toContain(
      '"reason":"grading.reason.DISABLED"',
    );
  });

  it("says there has been no change yet instead of an empty line", () => {
    render(
      <GradingCard
        grading={block({
          consumers: { target: 1, active: 1, max: 8, lastScaling: null },
        })}
      />,
    );
    expect(value("grading.lastScaling")).toBe("grading.noScaling");
  });

  it("drops every thread figure when the worker has not reported, but keeps the throughput", () => {
    render(<GradingCard grading={block({ consumers: null })} />);
    expect(value("grading.running")).toBe("grading.noReport");
    expect(screen.queryByText("grading.target")).toBeNull();
    expect(screen.queryByText("grading.lastScaling")).toBeNull();
    expect(value("grading.throughput")).toContain('"count":31');
    expect(value("grading.clear")).toContain("grading.clearSeconds");
  });

  it("gives the drain estimate in seconds under a minute, in rounded-up minutes from a minute, and no number when unknown", () => {
    const { rerender } = render(
      <GradingCard grading={block({ estimatedClearSeconds: 59 })} />,
    );
    expect(value("grading.clear")).toBe('grading.clearSeconds|{"count":59}');
    rerender(<GradingCard grading={block({ estimatedClearSeconds: 60 })} />);
    expect(value("grading.clear")).toBe('grading.clearMinutes|{"count":1}');
    rerender(<GradingCard grading={block({ estimatedClearSeconds: 61 })} />);
    expect(value("grading.clear")).toBe('grading.clearMinutes|{"count":2}');
    rerender(<GradingCard grading={block({ estimatedClearSeconds: 3600 })} />);
    expect(value("grading.clear")).toBe('grading.clearMinutes|{"count":60}');
    rerender(<GradingCard grading={block({ estimatedClearSeconds: null })} />);
    expect(value("grading.clear")).toBe("grading.clearUnknown");
  });

  it("shows zero throughput as a real number, not a dash", () => {
    render(<GradingCard grading={block({ finishedLastMinute: 0 })} />);
    expect(value("grading.throughput")).toContain('"count":0');
  });
});

// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Two panes with a draggable divider (02-bd/screens/users/USR0102_problem_detail.md calls this
// `shared/ui/split-pane`). The ratio is state, not a constant in the view: drag, arrow keys, or
// double-click to reset; it is remembered per `storageKey`.
//
// `horizontal` = side by side. Below `lg` that direction stacks the panes and hides the divider
// (a drag handle is useless on a phone); `vertical` = one above the other at every width and needs a
// parent with a definite height.
"use client";

import {
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type KeyboardEvent,
  type PointerEvent,
  type ReactNode,
} from "react";
import { cn } from "@/shared/lib";

const KEY_STEP = 0.02;

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

export function SplitPane({
  direction,
  first,
  second,
  label,
  defaultRatio = 0.5,
  min = 0.2,
  max = 0.8,
  storageKey,
  className,
}: {
  direction: "horizontal" | "vertical";
  first: ReactNode;
  second: ReactNode;
  /** Accessible name of the divider. */
  label: string;
  /** Share of the container the first pane takes, 0..1. */
  defaultRatio?: number;
  min?: number;
  max?: number;
  /** localStorage key; omit to not remember the ratio. */
  storageKey?: string;
  className?: string;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [ratio, setRatio] = useState(defaultRatio);
  const horizontal = direction === "horizontal";

  // Read after mount so server and first client render agree.
  useEffect(() => {
    if (!storageKey) return;
    try {
      const saved = Number(window.localStorage.getItem(storageKey));
      // eslint-disable-next-line react-hooks/set-state-in-effect -- one-time restore after hydration
      if (saved > 0) setRatio(clamp(saved, min, max));
    } catch {
      // storage blocked: keep the default
    }
  }, [storageKey, min, max]);

  function commit(next: number) {
    const value = clamp(next, min, max);
    setRatio(value);
    if (!storageKey) return;
    try {
      window.localStorage.setItem(storageKey, String(value));
    } catch {
      // storage blocked: the ratio still applies for this session
    }
  }

  function onPointerDown(event: PointerEvent<HTMLDivElement>) {
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function onPointerMove(event: PointerEvent<HTMLDivElement>) {
    if (!event.currentTarget.hasPointerCapture(event.pointerId)) return;
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;
    commit(
      horizontal
        ? (event.clientX - rect.left) / rect.width
        : (event.clientY - rect.top) / rect.height,
    );
  }

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const back = horizontal ? "ArrowLeft" : "ArrowUp";
    const forward = horizontal ? "ArrowRight" : "ArrowDown";
    if (event.key === back) commit(ratio - KEY_STEP);
    else if (event.key === forward) commit(ratio + KEY_STEP);
    else if (event.key === "Home") commit(min);
    else if (event.key === "End") commit(max);
    else return;
    event.preventDefault();
  }

  return (
    <div
      ref={containerRef}
      style={{ "--split": `${ratio * 100}%` } as CSSProperties}
      className={cn(
        "flex min-h-0 min-w-0",
        horizontal ? "flex-col lg:flex-row" : "flex-col",
        className,
      )}
    >
      <div
        className={cn(
          "flex min-h-0 min-w-0 flex-col",
          horizontal
            ? "lg:shrink-0 lg:[flex-basis:var(--split)]"
            : "shrink-0 [flex-basis:var(--split)]",
        )}
      >
        {first}
      </div>

      <div
        role="separator"
        tabIndex={0}
        aria-label={label}
        aria-orientation={horizontal ? "vertical" : "horizontal"}
        aria-valuemin={Math.round(min * 100)}
        aria-valuemax={Math.round(max * 100)}
        aria-valuenow={Math.round(ratio * 100)}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onKeyDown={onKeyDown}
        onDoubleClick={() => commit(defaultRatio)}
        className={cn(
          "group flex shrink-0 touch-none items-center justify-center outline-none",
          horizontal
            ? "hidden w-3 cursor-col-resize lg:flex"
            : "h-3 cursor-row-resize",
        )}
      >
        <span
          aria-hidden="true"
          className={cn(
            "rounded-full bg-[var(--color-border)] transition-colors group-hover:bg-[var(--color-primary)] group-focus-visible:bg-[var(--color-primary)]",
            horizontal ? "h-10 w-1" : "h-1 w-10",
          )}
        />
      </div>

      <div className="flex min-h-0 min-w-0 flex-1 flex-col">{second}</div>
    </div>
  );
}

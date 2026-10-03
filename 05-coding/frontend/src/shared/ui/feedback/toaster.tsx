// Draws the toast store (DEC-2026-1003-toast-feedback-channel). Mounted once in app/providers, so
// the Admin, Instructor and Student areas and the sign-in screens all share it.
//
// Top right, by owner choice (2026-10-03). It was first built bottom right to keep the PageHeader
// actions (Save, Publish, Add...) uncovered; the owner wants it up top, so a card can sit over those
// buttons for its few seconds. It auto-dismisses, the X closes it, and hovering holds it, never
// blocks: `pointer-events-none` on the column lets clicks pass through the gaps between cards.
//
// Motion (classes in globals.css, all behind prefers-reduced-motion): a card slides in from the
// right with a small overshoot; closing slides it out and collapses its row so the cards below glide
// up instead of jumping; a thin bar along the bottom runs down to the moment it dismisses itself.
//
// Dismiss timing: a card closes after its `durationMs`, and hovering or focusing it holds it open.
// Leaving restarts the full duration instead of resuming the remainder — simpler, and a card the
// user reached for should stay readable a little longer anyway. The progress bar is remounted on
// every hold/release so it restarts in step with that timer.
"use client";

import { useEffect, useState } from "react";
import { cn } from "@/shared/lib";
import { toast, useToasts, type ToastItem, type ToastTone } from "@/shared/lib/toast-store";

export type ToasterLabels = {
  /** Accessible name of the whole region. */
  region: string;
  /** Accessible name of the X button. */
  dismiss: string;
  /** Short word per tone, shown on the card because colour alone is not enough. */
  tone: Record<ToastTone, string>;
};

/** Slide-out time; keep in step with `.toast-wrap` in globals.css. */
const LEAVE_MS = 220;

const TONE_CLASS: Record<ToastTone, { edge: string; label: string; bar: string }> = {
  success: {
    edge: "border-l-[var(--color-success)]",
    label: "text-[var(--color-success-text)]",
    bar: "bg-[var(--color-success)]",
  },
  error: {
    edge: "border-l-[var(--color-danger)]",
    label: "text-[var(--color-danger)]",
    bar: "bg-[var(--color-danger)]",
  },
  warning: {
    edge: "border-l-[var(--color-admin-warn)]",
    label: "text-[var(--color-admin-warn)]",
    bar: "bg-[var(--color-admin-warn)]",
  },
  info: {
    edge: "border-l-[var(--color-accent-blue)]",
    label: "text-[var(--color-accent-blue)]",
    bar: "bg-[var(--color-accent-blue)]",
  },
};

function ToastCard({ item, labels }: { item: ToastItem; labels: ToasterLabels }) {
  const [held, setHeld] = useState(false);
  const [leaving, setLeaving] = useState(false);

  // Timer and X both go through `leaving`, so the slide-out always plays before the card is removed.
  useEffect(() => {
    if (held || leaving) return;
    const timer = setTimeout(() => setLeaving(true), item.durationMs);
    return () => clearTimeout(timer);
  }, [held, leaving, item.durationMs]);

  useEffect(() => {
    if (!leaving) return;
    const timer = setTimeout(() => toast.dismiss(item.id), LEAVE_MS);
    return () => clearTimeout(timer);
  }, [leaving, item.id]);

  const style = TONE_CLASS[item.tone];
  const urgent = item.tone === "error" || item.tone === "warning";

  return (
    // The wrapper collapses its row (1fr to 0fr) while leaving; the inner box clips only then, so
    // the card's shadow is not cut off the rest of the time. `pb-2` is the gap between cards.
    <div
      className={cn("toast-wrap grid", leaving && "toast-leaving")}
      style={{ gridTemplateRows: leaving ? "0fr" : "1fr" }}
    >
      <div className={cn("min-h-0 pb-2", leaving && "overflow-hidden")}>
        <div
          role={urgent ? "alert" : "status"}
          onMouseEnter={() => setHeld(true)}
          onMouseLeave={() => setHeld(false)}
          onFocus={() => setHeld(true)}
          onBlur={() => setHeld(false)}
          className={cn(
            // Opaque surface: the page content scrolls under the toast, and a see-through card made it unreadable.
            "toast-enter glass-card pointer-events-auto relative flex items-start gap-3 overflow-hidden border border-l-4 border-[var(--color-border)] !bg-[var(--color-surface)] px-4 py-3 shadow-lg",
            style.edge,
          )}
        >
          <div className="min-w-0 flex-1">
            <p className={cn("text-[11px] font-semibold tracking-[0.08em] uppercase", style.label)}>
              {labels.tone[item.tone]}
            </p>
            <p className="mt-0.5 text-[13px] text-pretty break-words text-[var(--color-text)]">
              {item.message}
            </p>
          </div>
          <button
            type="button"
            aria-label={labels.dismiss}
            onClick={() => setLeaving(true)}
            className="-mr-1 shrink-0 cursor-pointer rounded-md px-2 text-lg leading-none text-[var(--color-text-muted)] hover:text-[var(--color-text)] focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)]"
          >
            {"×"}
          </button>
          <span
            // Remounted on hold/release so the bar restarts together with the dismiss timer.
            key={held ? "held" : "running"}
            aria-hidden="true"
            className={cn(
              "toast-progress pointer-events-none absolute right-0 bottom-0 left-0 h-[3px] origin-left opacity-60 motion-reduce:hidden",
              style.bar,
            )}
            style={{
              animationDuration: `${item.durationMs}ms`,
              animationPlayState: held || leaving ? "paused" : "running",
            }}
          />
        </div>
      </div>
    </div>
  );
}

export function Toaster({ labels }: { labels: ToasterLabels }) {
  const items = useToasts();

  return (
    <div
      role="region"
      aria-label={labels.region}
      className="pointer-events-none fixed top-4 right-4 z-[60] flex w-[min(24rem,calc(100vw-2rem))] flex-col-reverse"
    >
      {items.map((item) => (
        <ToastCard key={item.id} item={item} labels={labels} />
      ))}
    </div>
  );
}

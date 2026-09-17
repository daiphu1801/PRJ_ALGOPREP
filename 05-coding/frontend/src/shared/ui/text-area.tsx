// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Labelled multi-line input. The authoring screens need one for Markdown bodies, follow-up probes
// and criterion descriptions; TextField's single-line control cannot stand in for those.
//
// Mirrors TextField's label/error wiring exactly so the two read as one family.
"use client";

import { useId, type ComponentPropsWithoutRef } from "react";
import { cn } from "@/shared/lib";
import { InlineFieldError } from "./inline-field-error";

type TextAreaProps = Omit<ComponentPropsWithoutRef<"textarea">, "id"> & {
  label: string;
  hideLabel?: boolean;
  error?: string;
  wrapperClassName?: string;
};

export function TextArea({
  label,
  hideLabel = false,
  error,
  className,
  wrapperClassName,
  rows = 4,
  ...props
}: TextAreaProps) {
  const id = useId();
  const errorId = `${id}-error`;

  return (
    <div className={cn("w-full", wrapperClassName)}>
      <label
        htmlFor={id}
        className={cn(
          "mb-1 block text-xs font-medium text-[var(--color-text-muted)]",
          hideLabel && "sr-only",
        )}
      >
        {label}
      </label>
      <textarea
        id={id}
        rows={rows}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={cn(
          "w-full rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-[13px] text-[var(--color-text)] focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)] disabled:opacity-50",
          error && "border-[var(--color-danger)]",
          className,
        )}
        {...props}
      />
      <span id={errorId}>
        <InlineFieldError message={error} />
      </span>
    </div>
  );
}

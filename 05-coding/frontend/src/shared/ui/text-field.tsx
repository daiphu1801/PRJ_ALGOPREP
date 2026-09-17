// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Labelled text input for the Admin filter bars and authoring forms. Reuses the existing
// InlineFieldError so error presentation stays identical to features/auth-by-credentials.
"use client";

import { useId, type ComponentPropsWithoutRef, type ReactNode } from "react";
import { cn } from "@/shared/lib";
import { InlineFieldError } from "./inline-field-error";

export const FIELD_CONTROL =
  "h-9 w-full rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 text-[13px] text-[var(--color-text)] focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)] disabled:opacity-50";

type TextFieldProps = Omit<ComponentPropsWithoutRef<"input">, "id"> & {
  label: string;
  /** Hides the label visually but keeps it for screen readers — filter bars label by placeholder. */
  hideLabel?: boolean;
  /** Decorative glyph inside the control, before the text (search boxes carry one). */
  leadingIcon?: ReactNode;
  error?: string;
  wrapperClassName?: string;
};

export function TextField({
  label,
  hideLabel = false,
  leadingIcon,
  error,
  className,
  wrapperClassName,
  ...props
}: TextFieldProps) {
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
      {leadingIcon ? (
        // The icon sits inside the bordered box, so the box moves to the wrapper and the input
        // itself goes borderless — otherwise the icon would sit outside a second border.
        <div
          className={cn(
            FIELD_CONTROL,
            "flex items-center gap-2",
            error && "border-[var(--color-danger)]",
            className,
          )}
        >
          <span aria-hidden="true" className="shrink-0 text-[var(--color-text-subtle)]">
            {leadingIcon}
          </span>
          <input
            id={id}
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? errorId : undefined}
            className="min-w-0 flex-1 bg-transparent text-[13px] text-[var(--color-text)] outline-none"
            {...props}
          />
        </div>
      ) : (
        <input
          id={id}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className={cn(FIELD_CONTROL, error && "border-[var(--color-danger)]", className)}
          {...props}
        />
      )}
      <span id={errorId}>
        <InlineFieldError message={error} />
      </span>
    </div>
  );
}

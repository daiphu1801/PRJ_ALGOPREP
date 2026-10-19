// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Labelled text input for the Admin filter bars and authoring forms. A failed field only turns its
// border red and sets aria-invalid; the message itself is a toast (DEC-2026-1003-toast-feedback-channel),
// raised by whoever validated, e.g. with `toastFirstError`.
"use client";

import { useId, type ComponentPropsWithoutRef, type ReactNode } from "react";
import { cn } from "@/shared/lib";

export const FIELD_CONTROL =
  "h-9 w-full rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 text-[13px] text-[var(--color-text)] focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)] disabled:opacity-50";

type TextFieldProps = Omit<ComponentPropsWithoutRef<"input">, "id"> & {
  label: string;
  /** Hides the label visually but keeps it for screen readers — filter bars label by placeholder. */
  hideLabel?: boolean;
  /** Decorative glyph inside the control, before the text (search boxes carry one). */
  leadingIcon?: ReactNode;
  /** Marks the field as failed validation. Draws no text: the message goes to a toast. */
  invalid?: boolean;
  wrapperClassName?: string;
};

export function TextField({
  label,
  hideLabel = false,
  leadingIcon,
  invalid = false,
  className,
  wrapperClassName,
  ...props
}: TextFieldProps) {
  const id = useId();

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
            // The wrapper never takes focus itself, so FIELD_CONTROL's focus-visible ring never
            // fired here and the borderless input had none either (admin UX review F2). Draw the
            // same ring when the input inside is focused.
            "flex items-center gap-2 focus-within:outline-2 focus-within:outline-offset-1 focus-within:outline-[var(--color-primary)]",
            invalid && "border-[var(--color-danger)]",
            className,
          )}
        >
          <span
            aria-hidden="true"
            className="shrink-0 text-[var(--color-text-subtle)]"
          >
            {leadingIcon}
          </span>
          {/* self-stretch: the input fills the box height, so a click anywhere right of the icon
              lands in it instead of on a 20px strip in the middle. */}
          <input
            id={id}
            aria-invalid={invalid || undefined}
            className="min-w-0 flex-1 self-stretch bg-transparent text-[13px] text-[var(--color-text)] outline-none"
            {...props}
          />
        </div>
      ) : (
        <input
          id={id}
          aria-invalid={invalid || undefined}
          className={cn(
            FIELD_CONTROL,
            invalid && "border-[var(--color-danger)]",
            className,
          )}
          {...props}
        />
      )}
    </div>
  );
}

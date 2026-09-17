// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Labelled native <select> for the Admin filter bars (role / status / time-range filters named in
// 02-bd/screens/admin/admin_user_management.md section 4 and admin_system_log.md).
//
// Native <select> on purpose: it gets keyboard behaviour, mobile pickers and screen-reader support
// for free, none of which a custom dropdown would have at prototype stage.
"use client";

import { useId, type ComponentPropsWithoutRef } from "react";
import { cn } from "@/shared/lib";
import { FIELD_CONTROL } from "./text-field";

type SelectFieldProps = Omit<ComponentPropsWithoutRef<"select">, "id" | "children"> & {
  label: string;
  hideLabel?: boolean;
  options: { value: string; label: string }[];
  wrapperClassName?: string;
};

export function SelectField({
  label,
  hideLabel = false,
  options,
  className,
  wrapperClassName,
  ...props
}: SelectFieldProps) {
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
      <select id={id} className={cn(FIELD_CONTROL, className)} {...props}>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  );
}

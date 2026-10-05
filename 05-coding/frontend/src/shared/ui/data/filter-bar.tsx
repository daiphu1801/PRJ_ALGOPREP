// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// The toolbar above a list: a search box that takes the free width, the filter controls (FilterMenu)
// after it, and the "n / total" result count pushed to the right edge. It was hand-written, identically,
// in about ten list screens (09-layoutBase/Admin - Quản lý bài tập.dc.html:176-190 is the original).
//
// Search stays silent while typing; a screen that wants to announce the count on Enter passes `onSubmit`.
"use client";

import type { ReactNode } from "react";
import { Search } from "lucide-react";
import { TextField } from "../form/text-field";

type FilterBarProps = {
  search: {
    /** Accessible name of the box (it has no visible label). */
    label: string;
    placeholder: string;
    value: string;
    onChange: (value: string) => void;
    /** Called when Enter is pressed in the box. */
    onSubmit?: () => void;
  };
  /** The filter controls, normally FilterMenu elements. */
  children?: ReactNode;
  /** "n / total" text, shown at the right edge. */
  resultCount?: ReactNode;
};

export function FilterBar({ search, children, resultCount }: FilterBarProps) {
  return (
    <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
      <TextField
        label={search.label}
        hideLabel
        leadingIcon={<Search className="h-3.5 w-3.5" />}
        placeholder={search.placeholder}
        value={search.value}
        onChange={(event) => search.onChange(event.target.value)}
        onKeyDown={
          search.onSubmit
            ? (event) => {
                if (event.key === "Enter") search.onSubmit?.();
              }
            : undefined
        }
        wrapperClassName="min-w-[220px] flex-1"
      />
      {children}
      {resultCount ? (
        <span className="ml-auto text-[12.5px] whitespace-nowrap text-[var(--color-text-muted)]">
          {resultCount}
        </span>
      ) : null}
    </div>
  );
}

// Presentation of the infra-events screen: the timeline row and the day-range fields.
"use client";

import { type InfraEvent, type InfraLevel } from "@/entities/infra-event";
import { Badge, TextField, type BadgeVariant } from "@/shared/ui";

const LEVEL_VARIANT: Record<InfraLevel, BadgeVariant> = {
  error: "negative",
  warn: "warn",
  info: "blue",
};

const LEVEL_COLOR_VAR: Record<InfraLevel, string> = {
  error: "--color-admin-negative",
  warn: "--color-admin-warn",
  info: "--color-accent-blue",
};

/** "2026-10-08" -> "08/10". */
function formatDay(date: string): string {
  const [, month, day] = date.split("-");
  return `${day}/${month}`;
}

/** One timeline entry. The left rule only repeats the badge, so the badge carries the accessible text. */
export function EventRow({
  event,
  levelLabel,
}: {
  event: Pick<InfraEvent, "date" | "time" | "level" | "message" | "service">;
  levelLabel: string;
}) {
  return (
    <li
      className="grid grid-cols-[66px_84px_minmax(180px,1fr)] items-start gap-3 rounded-xl border-l-2 px-2.5 py-2.5 hover:bg-[var(--color-row-hover)]"
      style={{ borderLeftColor: `var(${LEVEL_COLOR_VAR[event.level]})` }}
    >
      <span className="pt-px font-mono text-xs text-[var(--color-text-subtle)]">
        {event.time}
        <span className="block text-[10.5px]">{formatDay(event.date)}</span>
      </span>
      <Badge
        variant={LEVEL_VARIANT[event.level]}
        className="w-full justify-center font-mono"
      >
        {levelLabel}
      </Badge>
      <span className="min-w-0">
        <span className="block text-[13px] font-semibold text-pretty break-words whitespace-pre-wrap">
          {event.message}
        </span>
        <span className="mt-0.5 block font-mono text-[11.5px] text-[var(--color-text-subtle)]">
          {event.service}
        </span>
      </span>
    </li>
  );
}

/** "From" and "to" day pickers as one unit, so the two ends wrap to the next line together. */
export function DateRangeFields({
  from,
  to,
  fromLabel,
  toLabel,
  onFromChange,
  onToChange,
  invalid,
}: {
  from: string;
  to: string;
  fromLabel: string;
  toLabel: string;
  onFromChange: (value: string) => void;
  onToChange: (value: string) => void;
  invalid: boolean;
}) {
  return (
    <span className="flex flex-wrap items-center gap-x-4 gap-y-2">
      <span className="flex items-center gap-1.5 text-[12.5px] font-medium whitespace-nowrap text-[var(--color-text-muted)]">
        {fromLabel}:
        <TextField
          label={fromLabel}
          hideLabel
          type="date"
          value={from}
          max={to || undefined}
          onChange={(event) => onFromChange(event.target.value)}
          wrapperClassName="w-[140px]"
          className="h-[34px]"
        />
      </span>
      <span className="flex items-center gap-1.5 text-[12.5px] font-medium whitespace-nowrap text-[var(--color-text-muted)]">
        {toLabel}:
        <TextField
          label={toLabel}
          hideLabel
          type="date"
          value={to}
          min={from || undefined}
          onChange={(event) => onToChange(event.target.value)}
          invalid={invalid}
          wrapperClassName="w-[140px]"
          className="h-[34px]"
        />
      </span>
    </span>
  );
}

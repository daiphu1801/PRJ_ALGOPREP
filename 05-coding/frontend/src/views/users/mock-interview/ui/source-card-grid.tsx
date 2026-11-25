// The "pick what to be interviewed on" cards of the entry screen: two columns, so a long list reads in half the
// height, and each card tells apart two submissions of the same problem (language, level, date). Past `LIMIT`
// cards the rest sit behind "Xem thêm".
"use client";

import { useState, type ReactNode } from "react";
import { Check, type LucideIcon } from "lucide-react";
import { useT } from "@/shared/i18n";

const LIMIT = 6;

export type SourceCardData = {
  id: string;
  title: string;
  /** Small chips under the title: language, level, ... */
  chips?: ReactNode;
  /** "Nộp 22/11/2026" and the like. */
  caption?: string;
};

function SourceCard({
  icon: Icon,
  card,
  selected,
  onPick,
}: {
  icon: LucideIcon;
  card: SourceCardData;
  selected: boolean;
  onPick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onPick}
      aria-pressed={selected}
      className={
        "flex h-full w-full items-start gap-3 rounded-xl border px-3.5 py-3 text-left transition-colors hover:bg-[var(--color-surface-hover)] " +
        (selected
          ? "border-[var(--color-primary)] bg-[color-mix(in_srgb,var(--color-primary)_8%,transparent)]"
          : "border-[var(--color-border)]")
      }
    >
      <span
        className={
          "mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg " +
          (selected
            ? "bg-[var(--color-primary)] text-[var(--color-on-primary)]"
            : "bg-[var(--color-surface-hover)] text-[var(--color-text-muted)]")
        }
      >
        <Icon className="h-4 w-4" aria-hidden="true" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="line-clamp-2 text-[13.5px] font-semibold">
          {card.title}
        </span>
        {card.chips ? (
          <span className="mt-1.5 flex flex-wrap items-center gap-1.5">
            {card.chips}
          </span>
        ) : null}
        {card.caption ? (
          <span className="mt-1 block text-[12px] text-[var(--color-text-muted)]">
            {card.caption}
          </span>
        ) : null}
      </span>
      {selected ? (
        <Check
          className="mt-1 h-4 w-4 shrink-0 text-[var(--color-primary)]"
          aria-hidden="true"
        />
      ) : null}
    </button>
  );
}

export function SourceCardGrid({
  icon,
  cards,
  selectedId,
  onPick,
}: {
  icon: LucideIcon;
  cards: SourceCardData[];
  selectedId: string | null;
  onPick: (id: string) => void;
}) {
  const t = useT("mockInterview");
  const [expanded, setExpanded] = useState(false);
  // A chosen card is never hidden behind "Xem thêm": the choice stays visible after a reload of the list.
  const selectedIndex = cards.findIndex((card) => card.id === selectedId);
  const open = expanded || selectedIndex >= LIMIT;
  const shown = open ? cards : cards.slice(0, LIMIT);
  const hidden = cards.length - shown.length;
  return (
    <div>
      <ul className="grid gap-2.5 sm:grid-cols-2">
        {shown.map((card) => (
          <li key={card.id}>
            <SourceCard
              icon={icon}
              card={card}
              selected={selectedId === card.id}
              onPick={() => onPick(card.id)}
            />
          </li>
        ))}
      </ul>
      {cards.length > LIMIT ? (
        <button
          type="button"
          onClick={() => setExpanded((value) => !value)}
          disabled={selectedIndex >= LIMIT}
          className="mt-3 text-[13px] font-semibold text-[var(--color-primary)] hover:underline disabled:opacity-60"
        >
          {open
            ? t("source.showLess")
            : t("source.showMore", { count: hidden })}
        </button>
      ) : null}
    </div>
  );
}

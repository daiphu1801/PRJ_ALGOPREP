// A failed block of ADM0302: the reason in words (the AIR-* text, never the server message) and a retry
// that refreshes only this block. shared QueryError shows a generic line only; see the report for the
// shared upgrade this duplicates.
"use client";

import { useApiErrorMessage, useT } from "@/shared/i18n";
import { Button } from "@/shared/ui";

export function BlockError({
  query,
}: {
  query: { error: unknown; refetch: () => unknown };
}) {
  const t = useT("common");
  const toMessage = useApiErrorMessage();
  return (
    <div className="flex flex-col items-start gap-2 text-[13px] text-[var(--color-danger)]">
      <p>{toMessage(query.error)}</p>
      <Button
        variant="ghost"
        size="sm"
        className="border border-[var(--color-border)]"
        onClick={() => void query.refetch()}
      >
        {t("retry")}
      </Button>
    </div>
  );
}

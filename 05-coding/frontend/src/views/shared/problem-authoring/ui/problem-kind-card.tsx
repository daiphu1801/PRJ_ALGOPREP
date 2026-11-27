// "Kiểu bài": how the learner submits. Asked first because everything on the Spec tab follows from it: a
// stdin/stdout program has no function signature, a function problem must have one. Before this card the
// kind was only implied by whether "Tên hàm" had been filled in, and a stdin problem that got a signature
// by mistake failed at the reference run (PRB-060).
"use client";

import { useT } from "@/shared/i18n";
import { Card, SegmentedTabs } from "@/shared/ui";

export type ProblemKind = "stdio" | "function";

export function ProblemKindCard({
  value,
  onChange,
}: {
  value: ProblemKind;
  onChange: (kind: ProblemKind) => void;
}) {
  const t = useT("problemAuthoring");
  return (
    <Card title={t("kind.title")} description={t("kind.subtitle")}>
      <SegmentedTabs
        label={t("kind.title")}
        value={value}
        onValueChange={onChange}
        className="w-fit flex-wrap"
        options={[
          { value: "stdio" as const, label: t("kind.stdio") },
          { value: "function" as const, label: t("kind.function") },
        ]}
      />
      <p className="mt-2.5 text-[12.5px] text-[var(--color-text-muted)]">
        {t(value === "stdio" ? "kind.stdioHint" : "kind.functionHint")}
      </p>
    </Card>
  );
}

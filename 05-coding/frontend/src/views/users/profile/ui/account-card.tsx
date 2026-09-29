// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực D. No "Mã người dùng" row — Sheet 6 Khu vực D NO 2 says to drop it until the UUID-vs-
// display-code format question (Q5) is resolved, rather than show a raw UUID to a learner.
import type { ReactNode } from "react";
import { useT } from "@/shared/i18n";
import { Card, Skeleton } from "@/shared/ui";
import type { UserProfile } from "@/entities/user";

export function AccountCard({ profile }: { profile: UserProfile | undefined }) {
  const t = useT("profile");

  return (
    <Card title={t("account.title")}>
      <dl className="flex flex-col gap-2 text-sm">
        <Row label={t("account.joinedAt")}>
          {profile ? new Date(profile.joinedAt).toLocaleDateString("vi-VN") : <Skeleton className="h-4 w-20" />}
        </Row>
        <Row label={t("account.role")}>{profile ? profile.roleName : <Skeleton className="h-4 w-20" />}</Row>
      </dl>
    </Card>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 border-b border-[var(--color-border)] py-2 last:border-0">
      <dt className="text-[var(--color-text-muted)]">{label}</dt>
      <dd className="font-mono text-[var(--color-text)]">{children}</dd>
    </div>
  );
}

// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực D. "Mã người dùng" is the first 8 characters of the user id, uppercased (BD Q5, closed
// 2026-10-18: no extra column, the full UUID is never shown to the learner).
import type { ReactNode } from "react";
import { IdCard } from "lucide-react";
import { useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import { Badge, Card, Skeleton } from "@/shared/ui";
import type { UserProfile } from "@/entities/user";

export function AccountCard({ profile }: { profile: UserProfile | undefined }) {
  const t = useT("profile");

  return (
    <Card
      title={
        <span className="flex items-center gap-2">
          <IdCard
            size={18}
            aria-hidden="true"
            className="text-[var(--color-primary)]"
          />
          {t("account.title")}
        </span>
      }
    >
      <dl className="divide-y divide-[var(--color-border)] text-sm">
        <Row label={t("account.role")}>
          {profile ? (
            <Badge variant="blue" className="max-w-full whitespace-normal">
              {profile.roleName}
            </Badge>
          ) : (
            <Skeleton className="h-4 w-20" />
          )}
        </Row>
        <Row label={t("account.userCode")}>
          {profile ? (
            <span className="font-mono tracking-wide">
              {profile.userId.slice(0, 8).toUpperCase()}
            </span>
          ) : (
            <Skeleton className="h-4 w-20" />
          )}
        </Row>
        <Row label={t("account.joinedAt")}>
          {profile ? (
            formatDateVN(profile.joinedAt)
          ) : (
            <Skeleton className="h-4 w-20" />
          )}
        </Row>
      </dl>
    </Card>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 py-3 first:pt-1 last:pb-0">
      <dt className="text-[var(--color-text-muted)]">{label}</dt>
      <dd className="min-w-0 text-end font-medium wrap-anywhere">{children}</dd>
    </div>
  );
}

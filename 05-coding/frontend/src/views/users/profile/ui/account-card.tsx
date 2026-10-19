// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực D. "Mã người dùng" is the first 8 characters of the user id, uppercased (BD Q5, closed
// 2026-10-18: no extra column, the full UUID is never shown to the learner).
import type { ReactNode } from "react";
import { useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import { Card, Skeleton } from "@/shared/ui";
import type { UserProfile } from "@/entities/user";

export function AccountCard({
  profile,
  roleKey,
}: {
  profile: UserProfile | undefined;
  /** Overrides the mock learner role for the instructor and admin shells. */
  roleKey?: "instructor" | "admin";
}) {
  const t = useT("profile");

  return (
    <Card title={t("account.title")}>
      <dl className="flex flex-col gap-2 text-sm">
        <Row label={t("account.userCode")}>
          {profile ? (
            profile.userId.slice(0, 8).toUpperCase()
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
        <Row label={t("account.role")}>
          {profile ? (
            roleKey ? (
              t(
                roleKey === "admin"
                  ? "account.roleAdmin"
                  : "account.roleInstructor",
              )
            ) : (
              profile.roleName
            )
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
    <div className="flex items-center justify-between gap-3 border-b border-[var(--color-border)] py-2 last:border-0">
      <dt className="text-[var(--color-text-muted)]">{label}</dt>
      <dd className="font-mono text-[var(--color-text)]">{children}</dd>
    </div>
  );
}

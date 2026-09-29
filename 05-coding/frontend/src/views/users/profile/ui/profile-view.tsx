// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Layout per 09-layoutBase/Trang cá nhân.dc.html: identity block (:101-110), personal-info form
// (:112-129, fields :214-221, revert/save :122-127, :229-230), security list (:131-144, :232-235),
// account summary (:148-158, :237-243 minus user-id/plan rows, see below), practice summary
// (:160-172, :244-248), settings shortcut link (:174).
//
// USR0502_profile, layout per 02-bd/screens/users/USR0502_profile.md Sheet 4.4 (2-column grid,
// identity/form/security on the left, account/practice/settings-link on the right). A GetMyProfile
// failure replaces areas A-D with one error state (Sheet 6 Khu vực B NO 10) — area E/F keep
// rendering, since they read from entities/progress independently.
//
// Two deliberate cuts from the mockup's `account` array (:237-243), already tracked as open BD
// questions, not this file's citation bug: IdentityCard drops the "Đổi ảnh" button (BD Sheet 6
// Khu vực A NO 4, Q1 avatar storage source unresolved) and AccountCard drops "Mã người dùng" plus
// the `Pro` plan row (BD Sheet 6 Khu vực D NO 2, Q5 UUID-vs-display-code; RD USR0502 O-5 — AlgoPrep
// has no account-tier model).
"use client";

import Link from "next/link";
import { useT } from "@/shared/i18n";
import { Button, ErrorState } from "@/shared/ui";
import { useMyProfile } from "@/entities/user";
import { PersonalInfoCard } from "@/features/edit-my-profile";
import { AccountCard } from "./account-card";
import { IdentityCard } from "./identity-card";
import { PracticeCard } from "./practice-card";
import { SecurityCard } from "./security-card";

export function ProfileView() {
  const t = useT("profile");
  const query = useMyProfile();

  return (
    <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-[minmax(0,1fr)_340px]">
      <div className="flex min-w-0 flex-col gap-3.5">
        {query.isError ? (
          <ErrorState>
            <div className="flex flex-col items-center gap-2">
              <span>{t("loadError")}</span>
              <Button size="sm" variant="ghost" onClick={() => query.refetch()}>
                {t("retry")}
              </Button>
            </div>
          </ErrorState>
        ) : (
          <>
            <IdentityCard profile={query.data} />
            {query.data ? (
              <PersonalInfoCard profile={query.data} onProfileSaved={() => query.refetch()} />
            ) : null}
            {query.data ? <SecurityCard profile={query.data} /> : null}
          </>
        )}
      </div>

      <div className="flex flex-col gap-3.5">
        <AccountCard profile={query.data} />
        <PracticeCard />
        <Link
          href="/settings"
          className="flex h-9 items-center justify-center rounded-lg border border-[var(--color-border)] text-sm font-semibold hover:bg-[var(--color-surface-hover)]"
        >
          {t("settingsShortcut")}
        </Link>
      </div>
    </div>
  );
}

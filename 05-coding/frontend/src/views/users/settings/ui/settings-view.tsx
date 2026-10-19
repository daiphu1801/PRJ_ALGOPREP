// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// USR0503_settings, layout per 09-layoutBase/Cài đặt.dc.html: 2-column grid minmax(0,1fr) 320px
// (:95). Left column — AppearanceCard (Giao diện group, :215-225), PreferencesForm (Workspace
// :226-238, Phỏng vấn giả lập :239-251, Thông báo :252-260, plus the Trạng thái/Lưu card :136-141,
// moved next to the other server-saved groups per 02-bd/screens/users/USR0503_settings.md Sheet 4.5),
// DangerZoneCard (Vùng nguy hiểm, :124-133). Right column — DataExportCard (Dữ liệu của bạn,
// :143-153), "Về trang cá nhân" link (:155).
// The "leave with unsaved changes" dialog (Khu vực J) lives in PreferencesForm via
// `useUnsavedChangesGuard`. Simplification tracked in the ledger row: the email-confirm phrase for account deletion uses the loaded profile's email directly rather than a
// fully independent GetMyProfile-equivalent call.
"use client";

import Link from "next/link";
import { useT } from "@/shared/i18n";
import { Button, ErrorState, Skeleton } from "@/shared/ui";
import { useMyProfile } from "@/entities/user";
import { AppearanceCard } from "./appearance-card";
import { DangerZoneCard } from "./danger-zone-card";
import { DataExportCard } from "./data-export-card";
import { PreferencesForm } from "./preferences-form";

export function SettingsView({
  area = "student",
}: {
  area?: "student" | "instructor";
}) {
  const t = useT("settings");
  const profile = useMyProfile();

  return (
    <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-[minmax(0,1fr)_320px]">
      <h1 className="sr-only">{t("pageTitle")}</h1>
      <div className="flex min-w-0 flex-col gap-3.5">
        <AppearanceCard />
        <PreferencesForm />
        {profile.isError ? (
          // D33: without the profile there is no email to confirm against, so say so instead of a
          // skeleton that never resolves.
          <ErrorState>
            <div className="flex flex-col items-center gap-2">
              <span>{t("loadError")}</span>
              <Button size="sm" variant="ghost" onClick={() => profile.refetch()}>
                {t("retry")}
              </Button>
            </div>
          </ErrorState>
        ) : profile.data ? (
          <DangerZoneCard email={profile.data.email} />
        ) : (
          <Skeleton className="h-24" />
        )}
      </div>

      <div className="flex flex-col gap-3.5">
        <DataExportCard />
        <Link
          href={area === "instructor" ? "/instructor/profile" : "/profile"}
          className="flex h-9 items-center justify-center rounded-lg border border-[var(--color-border)] text-sm font-semibold hover:bg-[var(--color-surface-hover)]"
        >
          {t("backToProfile")}
        </Link>
      </div>
    </div>
  );
}

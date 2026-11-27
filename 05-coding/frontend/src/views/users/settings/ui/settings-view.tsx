"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useT } from "@/shared/i18n";
import { Button, ErrorState, Skeleton } from "@/shared/ui";
import { useMyProfile } from "@/entities/user";
import { AppearanceCard } from "./appearance-card";
import { DangerZoneCard } from "./danger-zone-card";
import { DataExportCard } from "./data-export-card";
import { PreferencesForm } from "./preferences-form";
import { SecurityCard } from "./security-card";

export function SettingsView({
  area = "student",
}: {
  area?: "student" | "instructor";
}) {
  const t = useT("settings");
  const profile = useMyProfile(area);

  return (
    <div
      data-settings-view
      className="@container flex w-full min-w-0 flex-col gap-4"
    >
      <header className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-bold tracking-tight">{t("pageTitle")}</h1>
        <Button
          asChild
          variant="ghost"
          className="h-11 gap-2 border border-[var(--color-border)]"
        >
          <Link
            href={area === "instructor" ? "/instructor/profile" : "/profile"}
          >
            <ArrowLeft size={16} aria-hidden="true" />
            {t("backToProfile")}
          </Link>
        </Button>
      </header>
      <div className="grid min-w-0 grid-cols-1 items-start gap-4 @min-[880px]:grid-cols-[minmax(0,1fr)_clamp(18rem,29cqw,24rem)]">
        <PreferencesForm />
        <div className="flex min-w-0 flex-col gap-4">
          <AppearanceCard />
          {profile.data ? <SecurityCard profile={profile.data} /> : null}
          <DataExportCard />
          {profile.isError ? (
            <ErrorState>
              <div className="flex flex-col items-center gap-2">
                <span>{t("loadError")}</span>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => profile.refetch()}
                >
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
      </div>
    </div>
  );
}

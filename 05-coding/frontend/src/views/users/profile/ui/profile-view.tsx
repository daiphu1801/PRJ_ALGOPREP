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

/**
 * Staff areas share the screen, with the learner practice card shown only to students.
 * The settings shortcut stays in the current area for students and instructors.
 */
export function ProfileView({
  area = "student",
}: {
  area?: "student" | "instructor" | "admin";
}) {
  const isStudent = area === "student";
  const t = useT("profile");
  const query = useMyProfile(area);
  const security = query.data ? <SecurityCard profile={query.data} /> : null;

  return (
    <div className="@container flex w-full min-w-0 flex-col gap-4">
      <h1
        className={isStudent ? "sr-only" : "text-2xl font-bold tracking-tight"}
      >
        {t("pageTitle")}
      </h1>
      {!isStudent && !query.isError ? (
        <IdentityCard profile={query.data} />
      ) : null}
      <div className="grid min-w-0 grid-cols-1 items-start gap-4 @min-[880px]:grid-cols-[minmax(0,1fr)_clamp(16rem,26cqw,22rem)]">
        <div className="flex min-w-0 flex-col gap-4">
          {query.isError ? (
            <ErrorState>
              <div className="flex flex-col items-center gap-2">
                <span>{t("loadError")}</span>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => query.refetch()}
                >
                  {t("retry")}
                </Button>
              </div>
            </ErrorState>
          ) : (
            <>
              {isStudent ? <IdentityCard profile={query.data} /> : null}
              {query.data ? (
                <PersonalInfoCard
                  profile={query.data}
                  showTargetPosition={isStudent}
                  onProfileSaved={() => query.refetch()}
                />
              ) : null}
              {isStudent ? security : null}
            </>
          )}
        </div>

        <div className="flex min-w-0 flex-col gap-4">
          {!query.isError ? (
            <>
              <AccountCard profile={query.data} />
              {!isStudent ? security : null}
            </>
          ) : null}
          {isStudent ? <PracticeCard /> : null}
          {area !== "admin" ? (
            <Link
              href={
                area === "instructor" ? "/instructor/settings" : "/settings"
              }
              className="flex h-9 items-center justify-center rounded-lg border border-[var(--color-border)] text-sm font-semibold hover:bg-[var(--color-surface-hover)]"
            >
              {t("settingsShortcut")}
            </Link>
          ) : null}
        </div>
      </div>
    </div>
  );
}

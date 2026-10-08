// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực A. No "Đổi ảnh" button — BD Sheet 6 Khu vực A NO 4 says to skip it entirely until an
// avatar storage source is confirmed (Câu hỏi mở Q1), rather than ship a button that does nothing.
import { Skeleton } from "@/shared/ui";
import type { UserProfile } from "@/entities/user";

// Same "last two words, uppercase" rule already used for teacher/admin initials
// [SoT: 02-bd/screens/teacher/INS0201_class_management.md:359; 02-bd/screens/admin/ADM0201_user_management.md:343].
function initialsOf(displayName: string): string {
  const parts = displayName.trim().split(/\s+/);
  const last2 = parts.slice(-2);
  return last2.map((p) => p[0]?.toUpperCase() ?? "").join("") || "?";
}

export function IdentityCard({
  profile,
}: {
  profile: UserProfile | undefined;
}) {
  if (!profile) {
    return (
      <section className="glass-card flex items-center gap-4 border border-[var(--color-border)] px-6 py-5">
        <Skeleton className="h-[68px] w-[68px] rounded-full" />
        <div className="flex flex-col gap-2">
          <Skeleton className="h-6 w-40" />
          <Skeleton className="h-4 w-56" />
        </div>
      </section>
    );
  }

  return (
    <section className="glass-card flex flex-wrap items-center gap-4 border border-[var(--color-border)] px-6 py-5">
      <div className="flex h-[68px] w-[68px] shrink-0 items-center justify-center rounded-full border border-[var(--color-border)] bg-[var(--color-surface)] text-xl font-semibold text-[var(--color-text-muted)]">
        {initialsOf(profile.displayName)}
      </div>
      <div className="min-w-0">
        <p className="text-xl font-bold tracking-tight">
          {profile.displayName}
        </p>
        <p className="mt-0.5 font-mono text-sm text-[var(--color-text-subtle)]">
          {profile.email}
        </p>
      </div>
    </section>
  );
}

// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực A: the avatar is the real picture with its change/remove controls (BD USR0502 V1.3,
// DEC-2026-1117-user-avatar-upload); the initials circle is only the fallback inside it.
import { Skeleton } from "@/shared/ui";
import type { UserProfile } from "@/entities/user";
import { AvatarEditor } from "@/features/edit-my-avatar";

export function IdentityCard({
  profile,
}: {
  profile: UserProfile | undefined;
}) {
  if (!profile) {
    return (
      <section className="glass-card flex items-center gap-4 border border-[var(--color-border)] px-4 py-5 sm:px-6">
        <Skeleton className="size-[68px] shrink-0 rounded-full" />
        <div className="flex min-w-0 flex-col gap-2">
          <Skeleton className="h-6 w-40 max-w-full" />
          <Skeleton className="h-4 w-56 max-w-full" />
        </div>
      </section>
    );
  }

  return (
    <section className="glass-card border border-[var(--color-border)] px-4 py-5 sm:px-6">
      <AvatarEditor
        userId={profile.userId}
        name={profile.displayName}
        email={profile.email}
        avatarVersion={profile.avatarVersion}
      />
    </section>
  );
}

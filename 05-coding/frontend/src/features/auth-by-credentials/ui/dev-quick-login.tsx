// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { isMockMode } from "@/shared/api";
import type { Role } from "@/entities/user";
import type { useAuthFlow } from "../model/use-auth-flow";

/**
 * The identifier the mock maps to each role (`entities/auth/api/__mock__/fake-auth.ts`: `admin` and
 * `instructor` are reserved, everything else logs in as a student).
 */
const QUICK_LOGIN_IDENTIFIER: Record<Role, string> = {
  STUDENT: "student",
  INSTRUCTOR: "instructor",
  ADMIN: "admin",
};

const LABEL_KEY: Record<Role, string> = {
  STUDENT: "quickLoginStudent",
  INSTRUCTOR: "quickLoginInstructor",
  ADMIN: "quickLoginAdmin",
};

type DevQuickLoginProps = {
  flow: ReturnType<typeof useAuthFlow>;
  /** Which accounts this screen offers. The instructor and admin screens only accept their own. */
  roles: Role[];
};

/**
 * One-click login while the data layer is mocked, so reviewing a screen does not start with typing
 * credentials into a form that accepts anything anyway.
 *
 * Gated on `isMockMode()` rather than on `NODE_ENV`: the panel should exist for exactly as long as
 * `login()` resolves to `fakeLogin` (`entities/auth/api/mutations.ts`), and disappear by itself the
 * day `NEXT_PUBLIC_MOCK_DATA` is turned off — no second switch to remember at graduation, and no
 * way for it to survive into a build that talks to the real identity service.
 */
export function DevQuickLogin({ flow, roles }: DevQuickLoginProps) {
  const t = useT("auth");

  if (!isMockMode()) return null;

  return (
    <div className="mt-6 rounded-lg border border-dashed border-[var(--color-border)] p-3">
      <p className="text-[11px] font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">
        {t("quickLoginTitle")}
      </p>
      <div className="mt-2 flex flex-wrap gap-2">
        {roles.map((role) => (
          <button
            key={role}
            type="button"
            disabled={flow.isSubmitting}
            onClick={() => void flow.quickLogin(QUICK_LOGIN_IDENTIFIER[role])}
            className="rounded-md border border-[var(--color-border)] px-2.5 py-1 text-xs font-medium text-[var(--color-text)] hover:bg-[var(--color-surface-hover)] disabled:opacity-50"
          >
            {t(LABEL_KEY[role])}
          </button>
        ))}
      </div>
      <p className="mt-2 text-[11px] leading-relaxed text-[var(--color-text-muted)]">
        {t("quickLoginHint")}
      </p>
    </div>
  );
}

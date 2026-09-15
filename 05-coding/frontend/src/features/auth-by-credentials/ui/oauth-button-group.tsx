// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { Button } from "@/shared/ui";

type OAuthButtonGroupProps = {
  onSelect: (provider: "google" | "github") => void;
  disabled?: boolean;
};

/**
 * Both buttons call the same success path in the static prototype
 * (02-bd/screens/shared/auth.md BD Q2) — a real per-provider OAuth redirect is DD's job. Only
 * shown in `signup`/`login`, per the component inventory.
 */
export function OAuthButtonGroup({ onSelect, disabled }: OAuthButtonGroupProps) {
  const t = useT("auth");

  return (
    <div>
      <div className="my-3 flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
        <span className="h-px flex-1 bg-[var(--color-border)]" />
        {t("oauthDivider")}
        <span className="h-px flex-1 bg-[var(--color-border)]" />
      </div>
      <div className="grid grid-cols-2 gap-2">
        <Button type="button" variant="ghost" disabled={disabled} onClick={() => onSelect("google")}>
          Google
        </Button>
        <Button type="button" variant="ghost" disabled={disabled} onClick={() => onSelect("github")}>
          GitHub
        </Button>
      </div>
    </div>
  );
}

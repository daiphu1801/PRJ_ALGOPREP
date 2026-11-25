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
 * (02-bd/screens/shared/SHR0101_auth.md, Câu hỏi mở Q2) — a real per-provider OAuth redirect is DD's job. Only
 * shown in `signup`/`login`, per the component inventory.
 */
export function OAuthButtonGroup({
  onSelect,
  disabled,
}: OAuthButtonGroupProps) {
  const t = useT("auth");

  return (
    <div>
      <div className="my-3 flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
        <span className="h-px flex-1 bg-[var(--color-border)]" />
        {t("oauthDivider")}
        <span className="h-px flex-1 bg-[var(--color-border)]" />
      </div>
      <div className="grid grid-cols-2 gap-2">
        <Button
          type="button"
          variant="ghost"
          className="gap-2"
          disabled={disabled}
          onClick={() => onSelect("google")}
        >
          <svg
            aria-hidden="true"
            viewBox="0 0 48 48"
            className="size-4 shrink-0"
          >
            <path
              fill="#4285F4"
              d="M43.61 24.46c0-1.36-.12-2.67-.35-3.92H24v7.43h11.05a9.45 9.45 0 0 1-4.1 6.19v5.15h6.63c3.88-3.57 6.03-8.83 6.03-14.85Z"
            />
            <path
              fill="#34A853"
              d="M24 44c5.51 0 10.13-1.83 13.5-4.96l-6.63-5.15c-1.83 1.23-4.17 1.97-6.87 1.97-5.33 0-9.85-3.6-11.47-8.45H5.7v5.31A20 20 0 0 0 24 44Z"
            />
            <path
              fill="#FBBC05"
              d="M12.53 27.41a12.02 12.02 0 0 1 0-6.82v-5.31H5.7a20 20 0 0 0 0 17.44l6.83-5.31Z"
            />
            <path
              fill="#EA4335"
              d="M24 12.14c3 0 5.69 1.03 7.81 3.05l5.86-5.86C34.12 6.03 29.51 4 24 4A20 20 0 0 0 5.7 15.28l6.83 5.31c1.62-4.85 6.14-8.45 11.47-8.45Z"
            />
          </svg>
          Google
        </Button>
        <Button
          type="button"
          variant="ghost"
          className="gap-2"
          disabled={disabled}
          onClick={() => onSelect("github")}
        >
          <svg
            aria-hidden="true"
            viewBox="0 0 24 24"
            fill="currentColor"
            className="size-4 shrink-0"
          >
            <path d="M12 1a11 11 0 0 0-3.48 21.44c.55.1.75-.24.75-.53v-2.05c-3.06.67-3.71-1.3-3.71-1.3-.5-1.27-1.22-1.6-1.22-1.6-1-.69.08-.67.08-.67 1.11.08 1.69 1.14 1.69 1.14.98 1.68 2.58 1.19 3.21.91.1-.71.39-1.2.7-1.48-2.44-.28-5.01-1.22-5.01-5.44 0-1.2.43-2.18 1.13-2.95-.11-.28-.49-1.4.11-2.91 0 0 .92-.29 3.03 1.13a10.5 10.5 0 0 1 5.52 0c2.1-1.42 3.02-1.13 3.02-1.13.6 1.51.22 2.63.11 2.91a4.23 4.23 0 0 1 1.13 2.95c0 4.23-2.57 5.16-5.02 5.43.4.35.75 1.02.75 2.05v3.01c0 .3.2.64.76.53A11 11 0 0 0 12 1Z" />
          </svg>
          GitHub
        </Button>
      </div>
    </div>
  );
}

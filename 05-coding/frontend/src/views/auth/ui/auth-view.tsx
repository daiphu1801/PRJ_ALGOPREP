"use client";

import { useState } from "react";
import { useT } from "@/shared/i18n";
import { Button } from "@/shared/ui";

/**
 * Chế độ của màn `auth`. Prototype và RD chốt: một màn duy nhất, chuyển chế độ tại chỗ, không
 * rời URL (01-rd/screens/shared/auth.md mục 3 điểm 3). Ba chế độ `forgot_*` (F1-17) đã có trong
 * RD nhưng chưa dựng UI — khai trong union để không ai tưởng màn chỉ có hai chế độ.
 */
export type AuthMode = "login" | "signup" | "forgot_email" | "forgot_otp" | "forgot_reset";

type AuthViewProps = {
  /** Cho phép deep-link: /login và /register vào đúng chế độ, dù UI vẫn là một màn. */
  initialMode?: AuthMode;
};

export function AuthView({ initialMode = "signup" }: AuthViewProps) {
  // Mặc định `signup` theo 01-rd/screens/shared/auth.md mục 3 điểm 1 (prototype vào lần đầu là
  // signup), route /login truyền initialMode="login" để ghi đè.
  const [mode, setMode] = useState<AuthMode>(initialMode);
  const t = useT("auth");

  const isLogin = mode === "login";

  return (
    <section className="w-full max-w-sm p-6">
      <h1 className="text-lg font-semibold">{isLogin ? t("loginTitle") : t("signupTitle")}</h1>
      <p className="mt-2 text-sm text-[var(--color-text-muted)]">
        {/* Form, OAuth, lỗi inline theo field (auth.md Q1) dựng khi có 02-bd/screens/shared/auth.md */}
      </p>
      <Button
        variant="ghost"
        className="mt-4"
        onClick={() => setMode(isLogin ? "signup" : "login")}
      >
        {isLogin ? t("switchToSignup") : t("switchToLogin")}
      </Button>
    </section>
  );
}

"use client";

import { useT } from "@/shared/i18n";
import { Toaster } from "@/shared/ui";

export function AppToaster() {
  const t = useT("common");

  return (
    <Toaster
      labels={{
        region: t("toast.region"),
        dismiss: t("toast.dismiss"),
        tone: {
          success: t("toast.tone.success"),
          error: t("toast.tone.error"),
          warning: t("toast.tone.warning"),
          info: t("toast.tone.info"),
        },
      }}
    />
  );
}

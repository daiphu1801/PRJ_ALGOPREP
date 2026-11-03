"use client";

import { useTranslations } from "next-intl";
import { ApiError } from "@/shared/api/api-error";

/**
 * Turns an API failure into text for the user. Looks the backend `errorCode` up under the
 * `apiErrors` namespace and falls back to UNKNOWN_ERROR for codes with no entry, so the screen
 * never shows a raw code, a server `detail`, or an empty string.
 */
export function useApiErrorMessage(): (error: unknown) => string {
  const t = useTranslations("apiErrors");
  return (error) => {
    const code = error instanceof ApiError ? error.code : "UNKNOWN_ERROR";
    return t.has(code) ? t(code) : t("UNKNOWN_ERROR");
  };
}

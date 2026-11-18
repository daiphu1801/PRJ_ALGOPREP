"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { cancelDeactivation, oauthCallback } from "@/entities/auth";
import { HOME_PATH_BY_ROLE } from "@/entities/user";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, Card, Skeleton } from "@/shared/ui";

type Provider = "google" | "github";

type View =
  | { kind: "working" }
  | { kind: "error"; message: string }
  | { kind: "deactivated"; ticket: string }
  | { kind: "restored" };

/**
 * Landing route of the provider's redirect (DD F1 section 0.1): reads `code` and `state`, removes
 * them from the address bar, calls #6 once and then behaves like a finished login. A restore ticket
 * (IDT-104) never leaves memory and never goes into the URL.
 */
export function OAuthCallbackView({ provider }: { provider: string }) {
  const t = useT("authCallback");
  const apiMessage = useApiErrorMessage();
  const router = useRouter();
  const params = useSearchParams();
  const started = useRef(false);
  const supported = provider === "google" || provider === "github";
  const [view, setView] = useState<View>(
    supported
      ? { kind: "working" }
      : { kind: "error", message: t("unknownProvider") },
  );
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    // Strict Mode runs effects twice in development; the code is single-use, so call only once.
    if (started.current) return;
    started.current = true;
    const code = params.get("code") ?? "";
    const state = params.get("state") ?? "";
    window.history.replaceState(null, "", window.location.pathname);
    if (!supported) {
      toast.error(t("unknownProvider"));
      return;
    }
    const fail = (message: string) => {
      toast.error(message);
      setView({ kind: "error", message });
    };
    void oauthCallback(provider as Provider, code, state)
      .then((outcome) => {
        if (outcome.ok && outcome.deactivated) {
          if (outcome.restoreTicket) {
            setView({ kind: "deactivated", ticket: outcome.restoreTicket });
          } else {
            fail(t("failed"));
          }
        } else if (outcome.ok) {
          router.replace(HOME_PATH_BY_ROLE[outcome.role]);
        } else {
          fail(t("failed"));
        }
      })
      .catch((error: unknown) => fail(apiMessage(error)));
  }, [params, provider, supported, router, t, apiMessage]);

  async function restore(ticket: string) {
    setBusy(true);
    try {
      await cancelDeactivation(ticket);
      setView({ kind: "restored" });
    } catch (error) {
      // The ticket is single-use: after a failure the person signs in again for a new one.
      const message = apiMessage(error);
      toast.error(message);
      setView({ kind: "error", message });
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center p-6">
      <Card title={t("title")} className="w-full max-w-md">
        {view.kind === "working" && (
          <div role="status" aria-live="polite" className="flex flex-col gap-3">
            <p className="text-sm">{t("working")}</p>
            <Skeleton className="h-4 w-full" />
          </div>
        )}
        {view.kind === "error" && (
          <div className="flex flex-col gap-4">
            <p className="text-sm">{view.message}</p>
            <Link href="/login" className="text-sm font-semibold underline">
              {t("backToLogin")}
            </Link>
          </div>
        )}
        {view.kind === "deactivated" && (
          <div className="flex flex-col gap-4">
            <p className="text-sm">{t("deactivatedBody")}</p>
            <Button disabled={busy} onClick={() => void restore(view.ticket)}>
              {t("restore")}
            </Button>
          </div>
        )}
        {view.kind === "restored" && (
          <div className="flex flex-col gap-4">
            <p className="text-sm">{t("restored")}</p>
            <Link href="/login" className="text-sm font-semibold underline">
              {t("backToLogin")}
            </Link>
          </div>
        )}
      </Card>
    </main>
  );
}

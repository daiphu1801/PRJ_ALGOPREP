// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useCallback, useState } from "react";
import { useRouter } from "next/navigation";
import { deleteMyAccount } from "@/entities/user";

/** Outcome of `confirm`: `idle` when the phrase did not match (nothing was sent), else done or the error key. */
export type DeactivateResult =
  | { status: "idle" }
  | { status: "done" }
  | { status: "error"; errorKey: string };

export function useDeactivateAccount(email: string) {
  const router = useRouter();
  const [confirmPhrase, setConfirmPhrase] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // D38: trim + case-insensitive on both sides (owner decision).
  const normalize = (v: string) => v.trim().toLowerCase();
  const canConfirm = normalize(confirmPhrase) === normalize(email);

  const confirm = useCallback(async (): Promise<DeactivateResult> => {
    if (!canConfirm) return { status: "idle" };
    setIsSubmitting(true);
    const outcome = await deleteMyAccount(confirmPhrase.trim());
    if (!outcome.ok) {
      setIsSubmitting(false);
      return { status: "error", errorKey: outcome.message };
    }
    // D34: stay "submitting" on success so the confirm button cannot fire a second request while
    // the router leaves the page.
    // Deactivation revokes every session — send the learner to the sign-in screen, matching BR-08.
    router.push("/login");
    return { status: "done" };
  }, [canConfirm, confirmPhrase, router]);

  return {
    confirmPhrase,
    setConfirmPhrase,
    canConfirm,
    isSubmitting,
    confirm,
  };
}

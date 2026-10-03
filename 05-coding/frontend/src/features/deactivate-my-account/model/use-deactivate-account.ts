// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useCallback, useState } from "react";
import { useRouter } from "next/navigation";
import { deleteMyAccount } from "@/entities/user";

/** Outcome of `confirm`: `idle` when the phrase did not match (nothing was sent), else done or the error key. */
export type DeactivateResult = { status: "idle" } | { status: "done" } | { status: "error"; errorKey: string };

export function useDeactivateAccount(email: string) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [confirmPhrase, setConfirmPhrase] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const openDialog = useCallback(() => {
    setConfirmPhrase("");
    setOpen(true);
  }, []);
  const closeDialog = useCallback(() => setOpen(false), []);

  const canConfirm = confirmPhrase === email;

  const confirm = useCallback(async (): Promise<DeactivateResult> => {
    if (!canConfirm) return { status: "idle" };
    setIsSubmitting(true);
    try {
      const outcome = await deleteMyAccount(confirmPhrase);
      if (!outcome.ok) {
        return { status: "error", errorKey: outcome.message };
      }
      setOpen(false);
      // Deactivation revokes every session — send the learner to the sign-in screen, matching BR-08.
      router.push("/login");
      return { status: "done" };
    } finally {
      setIsSubmitting(false);
    }
  }, [canConfirm, confirmPhrase, router]);

  return { open, openDialog, closeDialog, confirmPhrase, setConfirmPhrase, canConfirm, isSubmitting, confirm };
}

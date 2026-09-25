// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useCallback, useState } from "react";
import { useRouter } from "next/navigation";
import { deleteMyAccount } from "@/entities/user";

export function useDeactivateAccount(email: string) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [confirmPhrase, setConfirmPhrase] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const openDialog = useCallback(() => {
    setConfirmPhrase("");
    setError(null);
    setOpen(true);
  }, []);
  const closeDialog = useCallback(() => setOpen(false), []);

  const canConfirm = confirmPhrase === email;

  const confirm = useCallback(async () => {
    if (!canConfirm) return;
    setIsSubmitting(true);
    try {
      const outcome = await deleteMyAccount(confirmPhrase);
      if (!outcome.ok) {
        setError(outcome.message);
        return;
      }
      setOpen(false);
      // Deactivation revokes every session — send the learner to the sign-in screen, matching BR-08.
      router.push("/login");
    } finally {
      setIsSubmitting(false);
    }
  }, [canConfirm, confirmPhrase, router]);

  return { open, openDialog, closeDialog, confirmPhrase, setConfirmPhrase, canConfirm, error, isSubmitting, confirm };
}

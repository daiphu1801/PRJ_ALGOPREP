// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Confirmation step before an action that is not easily undone. Required by
// 02-bd/screens/admin/admin_user_management.md section 3 (`bulk-action-confirming`) for bulk lock /
// bulk role change — "hành động phá huỷ khả năng đăng nhập".
//
// The `destructive` variant only changes the confirm button's colour. Whether a given action counts
// as destructive is the screen's call, not this component's — it takes no domain type.
"use client";

import type { ReactNode } from "react";
import { Button } from "../primitives/button";
import { Modal } from "../overlay/modal";

type ConfirmDialogProps = {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  children: ReactNode;
  confirmLabel: string;
  cancelLabel: string;
  destructive?: boolean;
  /** Confirm request in flight — blocks a double submit. */
  pending?: boolean;
  /** Disables only the confirm button (e.g. until a typed value matches); Cancel stays enabled. */
  confirmDisabled?: boolean;
};

export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title,
  children,
  confirmLabel,
  cancelLabel,
  destructive = false,
  pending = false,
  confirmDisabled = false,
}: ConfirmDialogProps) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      footer={
        <>
          <Button
            variant="ghost"
            size="sm"
            onClick={onClose}
            disabled={pending}
          >
            {cancelLabel}
          </Button>
          <Button
            size="sm"
            onClick={onConfirm}
            disabled={pending || confirmDisabled}
            aria-busy={pending || undefined}
            className={
              destructive
                ? "bg-[var(--color-danger)] text-[var(--color-background)]"
                : undefined
            }
          >
            {confirmLabel}
          </Button>
        </>
      }
    >
      {children}
    </Modal>
  );
}

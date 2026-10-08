// Three-way prompt shown by `useUnsavedChangesGuard`: save and leave, leave without saving, or stay.
// Labels come from the screen so the dialog carries no domain wording.
"use client";

import type { ReactNode } from "react";
import { Button } from "../primitives/button";
import { Modal } from "../overlay/modal";

type UnsavedChangesDialogProps = {
  open: boolean;
  title: string;
  children: ReactNode;
  saveAndLeaveLabel: string;
  leaveLabel: string;
  stayLabel: string;
  onSaveAndLeave: () => void;
  onLeave: () => void;
  onStay: () => void;
  /** A save is in flight — blocks a double submit. */
  pending?: boolean;
};

export function UnsavedChangesDialog({
  open,
  title,
  children,
  saveAndLeaveLabel,
  leaveLabel,
  stayLabel,
  onSaveAndLeave,
  onLeave,
  onStay,
  pending = false,
}: UnsavedChangesDialogProps) {
  return (
    <Modal
      open={open}
      onClose={onStay}
      title={title}
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onStay} disabled={pending}>
            {stayLabel}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={onLeave}
            disabled={pending}
            className="text-[var(--color-danger)]"
          >
            {leaveLabel}
          </Button>
          <Button
            size="sm"
            onClick={onSaveAndLeave}
            disabled={pending}
            aria-busy={pending || undefined}
          >
            {saveAndLeaveLabel}
          </Button>
        </>
      }
    >
      {children}
    </Modal>
  );
}

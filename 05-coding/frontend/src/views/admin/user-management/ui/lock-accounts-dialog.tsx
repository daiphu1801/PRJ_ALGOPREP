// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Bulk lock confirmation, rebuilt from ConfirmDialog into a Modal because the owner instruction
// 2026-10-05 added two inputs: a free-text reason and a toggle that mails the affected users.
//
// F1-13 only says "khoá tài khoản" — nothing about telling the user. The reason field, the email and
// the rule that a reason is mandatory whenever the mail goes out are a NEW requirement and open a
// new `Fx-nn` code in 01-rd/req/identity.md. Built prototype-first on purpose: the project order is
// RD → BD → Prototype → DD, and this is the one screen area with no static mockup to compare against
// (09-layoutBase/Admin - Người dùng.dc.html never drew a dialog — that absence is why BD section 3
// flags the state as [SoT: Suy luận]). Documents follow once the owner signs the copy off.
//
// Layout is Modal + TextArea + Toggle, all shared — same family as the "Thêm tài khoản" popup.
"use client";

import { useState } from "react";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, Modal, TextArea, Toggle } from "@/shared/ui";
import type { AdminUser } from "../model/types";

/** Rows named in full before the dialog collapses the rest into a count. */
const NAME_PREVIEW = 8;

type Props = {
  open: boolean;
  onClose: () => void;
  /** Rows the lock would actually change; the caller has already dropped the already-locked ones. */
  targets: readonly AdminUser[];
  /** How many of the selected rows are already locked and will be skipped. */
  skippedCount: number;
  /** The acting admin is among the targets — allowed, but says so out loud. */
  includesSelf: boolean;
  /** `notify` comes from the toggle; the reason is stored on the audit log either way. */
  onConfirm: (reason: string, notify: boolean) => void;
};

export function LockAccountsDialog({
  open,
  onClose,
  targets,
  skippedCount,
  includesSelf,
  onConfirm,
}: Props) {
  const t = useT("adminUserManagement");
  const [reason, setReason] = useState("");
  const [notify, setNotify] = useState(true);
  const [touched, setTouched] = useState(false);

  // Mailing a "your account is locked" without saying why is the exact message that turns a routine
  // security action into a support ticket, so the reason is not optional once the mail is on.
  const reasonError =
    !notify || reason.trim() ? undefined : t("lockDialog.errorReasonRequired");
  const shown = targets.slice(0, NAME_PREVIEW);
  const rest = targets.length - shown.length;

  function close() {
    setReason("");
    setNotify(true);
    setTouched(false);
    onClose();
  }

  function submit() {
    setTouched(true);
    if (reasonError) {
      toast.warning(reasonError);
      return;
    }
    onConfirm(reason.trim(), notify);
    close();
  }

  return (
    <Modal
      open={open}
      onClose={close}
      title={t("lockDialog.title", { count: targets.length })}
      footer={
        <>
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            onClick={close}
          >
            {t("cancel")}
          </Button>
          <Button
            size="sm"
            onClick={submit}
            className="bg-[var(--color-admin-negative)] text-[var(--color-admin-negative-text)]"
          >
            {t("lockDialog.submit")}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <p className="text-[13px]">
          {t("lockDialog.body", { count: targets.length })}
        </p>

        {/* Names, not just a count: the row list is sorted by last activity, so "9 accounts" alone
            gives no way to tell whether the right nine are about to be locked. */}
        <ul className="max-h-40 overflow-y-auto rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-[12.5px]">
          {shown.map((user) => (
            <li
              key={user.email}
              className="flex items-baseline justify-between gap-3 py-0.5"
            >
              <span className="truncate font-semibold">{user.name}</span>
              <span className="shrink-0 font-mono text-[11px] text-[var(--color-text-subtle)]">
                {user.email}
              </span>
            </li>
          ))}
          {rest > 0 ? (
            <li className="py-0.5 text-[var(--color-text-muted)]">
              {t("lockDialog.moreCount", { count: rest })}
            </li>
          ) : null}
        </ul>

        {skippedCount > 0 ? (
          <p className="text-[12.5px] text-[var(--color-admin-warn)]">
            {t("lockDialog.skipped", { count: skippedCount })}
          </p>
        ) : null}

        {includesSelf ? (
          <p className="text-[12.5px] font-semibold text-[var(--color-admin-warn)]">
            {t("lockDialog.selfWarning")}
          </p>
        ) : null}

        <TextArea
          label={t("lockDialog.reasonLabel")}
          rows={3}
          value={reason}
          placeholder={t("lockDialog.reasonPlaceholder")}
          onChange={(event) => setReason(event.target.value)}
          invalid={touched && Boolean(reasonError)}
        />

        <Toggle
          checked={notify}
          onCheckedChange={setNotify}
          label={t("lockDialog.notifyLabel", { count: targets.length })}
          showLabel
        />
        <p className="text-[12.5px] text-[var(--color-text-muted)]">
          {t("lockDialog.notifyHint")}
        </p>
        <p className="text-[12.5px] text-[var(--color-text-subtle)]">
          {t("lockDialog.reasonHint")}
        </p>
      </div>
    </Modal>
  );
}

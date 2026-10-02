// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Thêm tài khoản" (ADM0201 Q1, DEC-2026-1001-admin-configurable-settings): ADMIN creates an
// INSTRUCTOR or STUDENT account by hand; the temporary password goes out by email. No endpoint yet,
// so the screen adds the row locally and says the email was "sent".
"use client";

import { useState } from "react";
import { useT } from "@/shared/i18n";
import { Button, Modal, NoticeTile, SelectField, TextField } from "@/shared/ui";
import type { AdminUser, AdminUserRole } from "../model/types";

type CreatableRole = Exclude<AdminUserRole, "admin">;

type Props = {
  open: boolean;
  onClose: () => void;
  existingEmails: ReadonlySet<string>;
  onCreate: (user: AdminUser) => void;
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function AddAccountDialog({ open, onClose, existingEmails, onCreate }: Props) {
  const t = useT("adminUserManagement");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<CreatableRole>("student");
  const [touched, setTouched] = useState(false);
  const [created, setCreated] = useState<string | null>(null);

  const normalized = email.trim().toLowerCase();
  const nameError = name.trim() ? undefined : t("addDialog.errorName");
  const emailError = !EMAIL_PATTERN.test(normalized)
    ? t("addDialog.errorEmail")
    : existingEmails.has(normalized)
      ? t("addDialog.errorDuplicate")
      : undefined;

  function close() {
    setName("");
    setEmail("");
    setRole("student");
    setTouched(false);
    setCreated(null);
    onClose();
  }

  function submit() {
    setTouched(true);
    if (nameError || emailError) return;
    onCreate({
      name: name.trim(),
      email: normalized,
      role,
      solvedCount: 0,
      submissionCount: 0,
      lastActiveLabel: "-",
      status: "pending",
    });
    setCreated(normalized);
  }

  return (
    <Modal
      open={open}
      onClose={close}
      title={t("addDialog.title")}
      footer={
        <>
          <Button variant="ghost" size="sm" className="border border-[var(--color-border)]" onClick={close}>
            {created ? t("addDialog.done") : t("cancel")}
          </Button>
          {created ? null : (
            <Button variant="cta" size="sm" onClick={submit}>
              {t("addDialog.submit")}
            </Button>
          )}
        </>
      }
    >
      {created ? (
        <NoticeTile tone="info" title={t("addDialog.createdTitle")}>
          {t("addDialog.createdBody", { email: created })}
        </NoticeTile>
      ) : (
        <div className="flex flex-col gap-3">
          <TextField
            label={t("addDialog.nameLabel")}
            value={name}
            onChange={(event) => setName(event.target.value)}
            error={touched ? nameError : undefined}
          />
          <TextField
            label={t("addDialog.emailLabel")}
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            error={touched ? emailError : undefined}
          />
          <SelectField
            label={t("addDialog.roleLabel")}
            value={role}
            onChange={(event) => setRole(event.target.value as CreatableRole)}
            options={[
              { value: "student", label: t("role.student") },
              { value: "instructor", label: t("role.instructor") },
            ]}
          />
          <p className="text-[12.5px] text-[var(--color-text-muted)]">{t("addDialog.note")}</p>
        </div>
      )}
    </Modal>
  );
}

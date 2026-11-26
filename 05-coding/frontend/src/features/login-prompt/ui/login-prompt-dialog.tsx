"use client";

import Link from "next/link";
import { useT } from "@/shared/i18n";
import { Button, Modal } from "@/shared/ui";

type LoginPromptDialogProps = {
  open: boolean;
  /** Đường dẫn nội bộ để quay lại sau khi xác thực; `auth` tự lọc bằng isSafeRedirect. */
  returnTo: string;
  onClose: () => void;
};

/**
 * Hộp thoại "Cần đăng nhập" cho khách ở khu Người học: dùng chung cho thanh nav, nút Giải bài và
 * Lưu bài (03-dd/screens/users/USR0104_problem_overview.md mục 4A). Không lưu hành động chờ: sau khi
 * đăng nhập người dùng quay lại đúng trang và bấm lại.
 */
export function LoginPromptDialog({
  open,
  returnTo,
  onClose,
}: LoginPromptDialogProps) {
  const t = useT("loginPrompt");
  const redirect = encodeURIComponent(returnTo);
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={t("title")}
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            {t("later")}
          </Button>
          <Button
            asChild
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
          >
            <Link href={`/register?redirect=${redirect}`}>{t("register")}</Link>
          </Button>
          <Button asChild variant="cta" size="sm">
            <Link href={`/login?redirect=${redirect}`}>{t("login")}</Link>
          </Button>
        </>
      }
    >
      <p className="text-[14px] text-[var(--color-text-muted)]">{t("body")}</p>
    </Modal>
  );
}

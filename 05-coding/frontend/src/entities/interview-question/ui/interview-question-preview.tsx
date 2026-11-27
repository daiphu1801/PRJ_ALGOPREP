"use client";

import { useT } from "@/shared/i18n";
import { Button, MarkdownPreview, Modal } from "@/shared/ui";

export function InterviewQuestionPreview({
  title,
  content,
  followUps,
  onClose,
}: {
  title: string;
  content: string | null;
  followUps: readonly string[];
  onClose: () => void;
}) {
  const t = useT("interviewQuestionAuthoring");
  const prompts = followUps.filter((item) => item.trim());

  return (
    <Modal
      open
      title={t("previewAsLearner")}
      onClose={onClose}
      className="max-h-[calc(calc(100*var(--dvh1))-2rem)] overflow-y-auto"
      footer={
        <Button variant="ghost" size="sm" onClick={onClose}>
          {t("previewClose")}
        </Button>
      }
    >
      <div className="space-y-4">
        {title.trim() ? (
          <MarkdownPreview size="lg" className="font-semibold wrap-anywhere">
            {title}
          </MarkdownPreview>
        ) : (
          <p className="text-sm text-[var(--color-text-muted)]">
            {t("previewEmpty")}
          </p>
        )}
        {content?.trim() && <MarkdownPreview>{content}</MarkdownPreview>}
        {prompts.length > 0 && (
          <div className="border-t border-[var(--color-border)] pt-3">
            <h3 className="mb-2 text-sm font-semibold">{t("group2Title")}</h3>
            <ol className="list-decimal space-y-2 pl-5 text-sm leading-relaxed wrap-anywhere">
              {prompts.map((prompt, index) => (
                <li key={index}>{prompt}</li>
              ))}
            </ol>
          </div>
        )}
      </div>
    </Modal>
  );
}

import { NoticeTile } from "@/shared/ui";
import type { JudgeQueue } from "../api/progress";

export type QueueNoticeKey = "waiting" | "waitingWithEstimate" | "aboutToStart";

/**
 * F4-14: approximate queue position while a submission is PENDING. Renders nothing when `queue` is
 * null. NoticeTile is a `role="status"` region (polite, no focus change); the text only changes
 * when the numbers do, so the 5 s re-read does not re-announce.
 */
export function QueueNotice({
  queue,
  t,
  className,
}: {
  queue: JudgeQueue | null | undefined;
  /** Screen-specific i18n lookup: `submissionResult.queue.*` or `problemDetail.console.queue.*`. */
  t: (
    key: QueueNoticeKey,
    values: { ahead: number; seconds: number },
  ) => string;
  className?: string;
}) {
  if (!queue) return null;
  const seconds = queue.estimatedWaitSeconds;
  const key: QueueNoticeKey =
    queue.ahead <= 0
      ? "aboutToStart"
      : seconds == null
        ? "waiting"
        : "waitingWithEstimate";
  return (
    <NoticeTile tone="info" title={t(key, { ahead: queue.ahead, seconds: seconds ?? 0 })} className={className} />
  );
}

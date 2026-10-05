// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md.
//
// USR0401 `interview_bank_list`. Two states in one screen, not two routes — `browse` (default) and
// `drill` (flashcard) [SoT: 02-bd/screens/users/USR0401_interview_bank_list.md Sheet 1-4].
// Layout reference: 09-layoutBase/Câu hỏi phỏng vấn.dc.html.
//
// Reuses `entities/interview-question` end to end (type, mock, recall/bookmark state, list row,
// recall picker) instead of duplicating it — the entity was built for the admin-facing
// `interview-question-management` screen and is generic enough per
// `06-plan/PROTOTYPE_DEBT.md` mục 9.
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Search } from "lucide-react";
import {
  topicLabel,
  levelLabel,
  levelTone,
  useInterviewLevels,
  useInterviewTopics,
  fetchInterviewQuestionPage,
  useRecallAndBookmarkState,
  InterviewQuestionListRow,
  RecallLevelPicker,
  type InterviewQuestion,
  type QuestionLevel,
  type QuestionTopic,
  type RecallLevel,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Badge, Button, Card, EmptyState, FilterMenu, StatCard, TextField } from "@/shared/ui";

// Mặc định 10 thẻ, khoảng 5-20 [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:235].
const DRILL_SIZE = 10;

type StatusFilter = "all" | "new" | "reviewing" | "known";
type TopicFilter = QuestionTopic | "all";
type LevelFilter = QuestionLevel | "all";

function matchesStatus(recall: RecallLevel | null, filter: StatusFilter): boolean {
  if (filter === "all") return true;
  if (filter === "new") return recall === null;
  if (filter === "known") return recall === "known";
  return recall === "vague" || recall === "forgotten";
}

// EVT-11 [SoT: 02-bd/screens/users/USR0401_interview_bank_list.md Sheet 8 NO 11]: ưu tiên câu cần
// ôn lại, rồi câu chưa từng tự chấm, rồi câu đã thuộc.
const DRILL_PRIORITY: Record<"due" | "new" | "known", number> = { due: 0, new: 1, known: 2 };
function drillPriority(recall: RecallLevel | null): number {
  if (recall === null) return DRILL_PRIORITY.new;
  if (recall === "known") return DRILL_PRIORITY.known;
  return DRILL_PRIORITY.due;
}

export function InterviewBankListView() {
  const t = useT("interviewBankList");
  const [page] = useState(fetchInterviewQuestionPage);
  const topicList = useInterviewTopics();
  const levelList = useInterviewLevels();
  const { recall, bookmarks, rate, toggleBookmark } = useRecallAndBookmarkState(page.questions);

  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [topic, setTopic] = useState<TopicFilter>("all");
  const [level, setLevel] = useState<LevelFilter>("all");
  const [selectedCode, setSelectedCode] = useState<string | null>(page.questions[0]?.code ?? null);

  const [view, setView] = useState<"browse" | "drill">("browse");
  const [drillQueue, setDrillQueue] = useState<InterviewQuestion[]>([]);
  const [drillIndex, setDrillIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return page.questions.filter((question) => {
      if (topic !== "all" && question.topic !== topic) return false;
      if (level !== "all" && question.level !== level) return false;
      if (!matchesStatus(recall[question.code] ?? null, status)) return false;
      if (needle && !question.question.toLowerCase().includes(needle)) return false;
      return true;
    });
  }, [page.questions, recall, query, status, topic, level]);

  const selected = filtered.find((q) => q.code === selectedCode) ?? filtered[0] ?? null;

  const known = page.questions.filter((q) => recall[q.code] === "known").length;
  const reviewing = page.questions.filter((q) => {
    const r = recall[q.code];
    return r === "vague" || r === "forgotten";
  }).length;
  // [SoT: Suy luận] — mock has no `rated_at` timestamps, so "drilled this week" approximates as
  // "has ever been rated" rather than counting last-7-days events (Câu hỏi mở Q2 of the BD flags
  // the same under-count risk even with a real recall_ratings table).
  const drilledThisWeek = page.questions.filter((q) => recall[q.code] !== null).length;

  function startDrill() {
    if (filtered.length === 0) return;
    const queue = [...filtered]
      .sort((a, b) => drillPriority(recall[a.code] ?? null) - drillPriority(recall[b.code] ?? null))
      .slice(0, DRILL_SIZE);
    setDrillQueue(queue);
    setDrillIndex(0);
    setRevealed(false);
    setView("drill");
  }

  function exitDrill() {
    setView("browse");
  }

  function rateDrillCard(level: RecallLevel) {
    const current = drillQueue[drillIndex];
    if (!current) return;
    rate(current.code, level);
    if (drillIndex + 1 >= drillQueue.length) {
      setView("browse");
      toast.success(t("toast.drillDone", { count: drillQueue.length }));
      return;
    }
    setDrillIndex((i) => i + 1);
    setRevealed(false);
  }

  function rateSelected(code: string, level: RecallLevel) {
    rate(code, level);
    toast.success(t("toast.rated"));
  }

  function toggleBookmarkWithToast(code: string) {
    const wasBookmarked = bookmarks[code] ?? false;
    toggleBookmark(code);
    toast.success(wasBookmarked ? t("toast.unbookmarked") : t("toast.bookmarked"));
  }

  // The list filters live while typing (no toast per keystroke); Enter is the explicit search.
  function announceSearch() {
    toast.info(filtered.length > 0 ? t("toast.searchResult", { count: filtered.length }) : t("toast.searchEmpty"));
  }

  const recallLabels: Record<RecallLevel, string> = {
    known: t("recall.known"),
    vague: t("recall.vague"),
    forgotten: t("recall.forgotten"),
  };
  const recallBadgeLabel = (level: RecallLevel | null) => (level ? recallLabels[level] : t("recall.unrated"));

  if (view === "drill") {
    const card = drillQueue[drillIndex];
    if (!card) return null;
    return (
      <div className="mx-auto max-w-[780px] p-6">
        <div className="mb-3 flex items-center justify-between gap-3">
          <span className="font-mono text-[11.5px] font-semibold text-[var(--color-text-subtle)]">
            {t("drill.counter", { current: drillIndex + 1, total: drillQueue.length })}
          </span>
          <Button variant="ghost" size="sm" className="border border-[var(--color-border)]" onClick={exitDrill}>
            {t("drill.exit")}
          </Button>
        </div>
        <div className="mb-4 h-1 overflow-hidden rounded-full bg-[var(--color-track)]">
          <div
            className="h-1 rounded-full bg-[var(--color-primary)] transition-[width]"
            style={{ width: `${Math.round(((drillIndex + 1) / drillQueue.length) * 100)}%` }}
          />
        </div>
        <Card>
          <div className="mb-4 flex items-center gap-2">
            <span className="text-[10.5px] font-semibold tracking-[0.06em] text-[var(--color-text-subtle)] uppercase">
              {topicLabel(topicList, card.topic)}
            </span>
            <span className="text-[11.5px] font-semibold text-[var(--color-text-muted)]">
              {levelLabel(levelList, card.level)}
            </span>
          </div>
          <h2 className="mb-6 min-h-[80px] text-center text-lg font-semibold text-pretty">
            {card.question}
          </h2>
          {revealed ? (
            <ul className="mb-6 flex flex-col gap-2 border-t border-[var(--color-border)] pt-4">
              {card.suggestedApproach.map((point, index) => (
                <li key={point} className="flex gap-2.5 text-[13.5px] text-[var(--color-text-muted)]">
                  <span className="font-mono text-[11.5px] font-semibold text-[var(--color-primary)]">
                    {String(index + 1).padStart(2, "0")}
                  </span>
                  <span>{point}</span>
                </li>
              ))}
            </ul>
          ) : null}
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-4">
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              onClick={() => setRevealed((r) => !r)}
            >
              {revealed ? t("drill.hide") : t("drill.reveal")}
            </Button>
            <RecallLevelPicker
              value={null}
              onRate={rateDrillCard}
              labels={recallLabels}
              groupLabel={t("quickView.recallLabel")}
            />
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-4 flex flex-wrap items-center gap-4">
        <div className="grid flex-1 grid-cols-2 gap-3 sm:grid-cols-4">
          <StatCard label={t("stats.total")} value={page.totalQuestions} />
          <StatCard label={t("stats.known")} value={`${known} / ${page.questions.length}`} />
          <StatCard label={t("stats.reviewing")} value={`${reviewing} / ${page.questions.length}`} />
          <StatCard label={t("stats.drilledThisWeek")} value={drilledThisWeek} />
        </div>
        <div className="flex gap-2">
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            disabled={filtered.length === 0}
            onClick={startDrill}
          >
            {t("startDrill", { count: Math.min(DRILL_SIZE, filtered.length) })}
          </Button>
          {/* Chưa có route độc lập cho mock_interview (USR0302) — mount hiện tại chỉ có
              /submissions/[submissionId]/interview, đòi hỏi một submissionId mà màn này không có
              [SoT: Suy luận — không tìm thấy route nào khác dưới src/app/(student)]. Ghi nợ ở
              Summary thay vì dẫn tới một URL bịa. */}
          <Button variant="primary" size="sm" disabled title={t("startMockInterviewUnavailable")}>
            {t("startMockInterview")}
          </Button>
        </div>
      </div>

      <div className="grid gap-3.5 lg:grid-cols-2">
        <Card className="!p-0 overflow-hidden">
          <div className="flex flex-col gap-2.5 border-b border-[var(--color-border)] px-4 py-3">
            <div className="flex flex-wrap items-center gap-2.5">
              <TextField
                label={t("searchLabel")}
                hideLabel
                leadingIcon={<Search className="h-3.5 w-3.5" />}
                placeholder={t("searchPlaceholder")}
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") announceSearch();
                }}
                wrapperClassName="min-w-[180px] flex-1"
              />
              <FilterMenu
                label={t("statusFilterLabel")}
                value={status}
                onValueChange={setStatus}
                options={[
                  { value: "all" as const, label: t("statusTabs.all") },
                  { value: "new" as const, label: t("statusTabs.new") },
                  { value: "reviewing" as const, label: t("statusTabs.reviewing") },
                  { value: "known" as const, label: t("statusTabs.known") },
                ]}
              />
            </div>
            <div className="flex flex-wrap gap-1.5">
              <FilterMenu
                label={t("topicFilterLabel")}
                value={topic}
                onValueChange={setTopic}
                options={[
                  { value: "all" as const, label: t("filterAll") },
                  ...topicList.map((item) => ({ value: item.key, label: item.label })),
                ]}
              />
            </div>
            <div className="flex flex-wrap gap-1.5">
              <FilterMenu
                label={t("levelFilterLabel")}
                value={level}
                onValueChange={setLevel}
                options={[
                  { value: "all" as const, label: t("filterAll") },
                  ...levelList.map((item) => ({ value: item.key, label: item.label })),
                ]}
              />
            </div>
          </div>
          <div className="max-h-[620px] overflow-y-auto">
            {filtered.length === 0 ? (
              <EmptyState>{t("emptyFiltered")}</EmptyState>
            ) : (
              filtered.map((question) => (
                <InterviewQuestionListRow
                  key={question.code}
                  question={question}
                  topicLabel={topicLabel(topicList, question.topic)}
                  levelLabel={levelLabel(levelList, question.level)}
                  levelTone={levelTone(levelList, question.level)}
                  recallLabel={recallBadgeLabel(recall[question.code] ?? null)}
                  recall={recall[question.code] ?? null}
                  selected={question.code === selected?.code}
                  bookmarked={bookmarks[question.code] ?? false}
                  bookmarkLabel={t("bookmarkLabel")}
                  onSelect={() => setSelectedCode(question.code)}
                  onToggleBookmark={() => toggleBookmarkWithToast(question.code)}
                />
              ))
            )}
          </div>
        </Card>

        <Card className="sticky top-4 self-start">
          {selected ? (
            <>
              <div className="mb-3 flex items-center gap-2">
                <Badge variant="neutral">{topicLabel(topicList, selected.topic)}</Badge>
                <span className="text-[11.5px] font-semibold text-[var(--color-text-muted)]">
                  {levelLabel(levelList, selected.level)}
                </span>
              </div>
              <h2 className="mb-4 text-lg font-semibold text-pretty">{selected.question}</h2>

              <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                {t("quickView.outlineLabel")}
              </p>
              <ul className="mb-4 flex flex-col gap-2">
                {selected.suggestedApproach.map((point, index) => (
                  <li key={point} className="flex gap-2.5 text-[13px] text-[var(--color-text-muted)]">
                    <span className="font-mono text-[11px] font-semibold text-[var(--color-primary)]">
                      {String(index + 1).padStart(2, "0")}
                    </span>
                    <span>{point}</span>
                  </li>
                ))}
              </ul>

              {selected.coreKeywords.length > 0 ? (
                <div className="mb-4 flex flex-wrap gap-1.5">
                  {selected.coreKeywords.map((keyword) => (
                    <Badge key={keyword} variant="neutral">
                      {keyword}
                    </Badge>
                  ))}
                </div>
              ) : null}

              <div className="border-t border-[var(--color-border)] pt-3">
                <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                  {t("quickView.recallLabel")}
                </p>
                <RecallLevelPicker
                  value={recall[selected.code] ?? null}
                  onRate={(level) => rateSelected(selected.code, level)}
                  labels={recallLabels}
                  groupLabel={t("quickView.recallLabel")}
                  className="mb-3"
                />
                <Link
                  href={`/interview-bank/${selected.code}`}
                  className="text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline"
                >
                  {t("quickView.viewDetail")}
                </Link>
              </div>
            </>
          ) : (
            <EmptyState>{t("quickView.selectPrompt")}</EmptyState>
          )}
        </Card>
      </div>
    </div>
  );
}

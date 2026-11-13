// USR0401 `interview_bank_list`. Two states in one screen, not two routes: `browse` (default) and
// `drill` (flashcard) [SoT: 02-bd/screens/users/USR0401_interview_bank_list.md Sheet 1-4].
// Layout reference: 09-layoutBase/Câu hỏi phỏng vấn.dc.html.
//
// Data: F6 #2 to #5, #9, #10 through `entities/interview-question` (03-dd/screens/users/USR0401
// section 2). The server filters and pages; this screen sends the filters, loads pages on demand and
// reads the per-user recall and bookmark from the rows. A rating or bookmark patches the cached rows
// (never refetches), so a row the filter would now drop stays on screen until a filter changes.
"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { Layers, Mic } from "lucide-react";
import {
  isQuestionGone,
  nextReviewDays,
  topicLabel,
  levelLabel,
  levelTone,
  useDropUnavailableQuestion,
  useInterviewLevels,
  useInterviewTopics,
  useLearnerQuestionDetail,
  useLearnerQuestionList,
  useRateRecall,
  useSetBookmark,
  useStartDrill,
  InterviewQuestionListRow,
  DRILL_SIZE,
  RecallLevelPicker,
  type DrillCard,
  type LearnerQuestionFilters,
  type LearnerQuestionItem,
  type RecallLevel,
  type RecallStatusFilter,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  FilterBar,
  FilterMenu,
  ProgressBar,
  QueryError,
  SeeAllLink,
  Skeleton,
  Toggle,
} from "@/shared/ui";

// Search box cap [SoT: 02-bd/screens/users/USR0401_interview_bank_list.md Sheet 9 NO 3].
const SEARCH_MAX_LENGTH = 100;

// The search text reaches the server only after the learner stops typing [SoT: DD USR0401 BR-SC-01].
const SEARCH_DEBOUNCE_MS = 400;

const SKELETON_ROWS = 9;

export function InterviewBankListView() {
  const t = useT("interviewBankList");
  const topicList = useInterviewTopics();
  const levelList = useInterviewLevels();
  const dropQuestion = useDropUnavailableQuestion();
  const rateMutation = useRateRecall();
  const bookmarkMutation = useSetBookmark();
  const drillMutation = useStartDrill();

  const [query, setQuery] = useState("");
  const [sentQuery, setSentQuery] = useState("");
  const [status, setStatus] = useState<RecallStatusFilter>("all");
  const [topic, setTopic] = useState("all");
  const [level, setLevel] = useState("all");
  // Independent axis next to the four status tabs, not a fifth tab [SoT: USR0401 BD Q8].
  const [bookmarkedOnly, setBookmarkedOnly] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const [view, setView] = useState<"browse" | "drill">("browse");
  const [drillQueue, setDrillQueue] = useState<DrillCard[]>([]);
  const [drillIndex, setDrillIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(
      () => setSentQuery(query),
      SEARCH_DEBOUNCE_MS,
    );
    return () => window.clearTimeout(timer);
  }, [query]);

  const filters = useMemo<LearnerQuestionFilters>(
    () => ({
      q: sentQuery.trim().slice(0, SEARCH_MAX_LENGTH),
      status,
      topic,
      level,
      bookmarkedOnly,
    }),
    [sentQuery, status, topic, level, bookmarkedOnly],
  );

  const list = useLearnerQuestionList(filters);
  // Toasts raised from effects read the latest `t` without re-running when a new translator appears.
  const translate = useRef(t);
  useEffect(() => {
    translate.current = t;
  });
  const notify = (key: string) => toast.error(translate.current(key));

  // The filters of the last answer that arrived: a failed reload puts the screen back on them. Both
  // adjustments happen while rendering (guarded, so they settle in one extra pass), the toast in an effect.
  const filterKey = JSON.stringify(filters);
  const [lastGood, setLastGood] = useState<{
    key: string;
    filters: LearnerQuestionFilters;
  } | null>(null);
  const [restoredCount, setRestoredCount] = useState(0);
  if (list.isSuccess && !list.isPlaceholderData && lastGood?.key !== filterKey)
    setLastGood({ key: filterKey, filters });
  if (list.isError && lastGood && lastGood.key !== filterKey) {
    setQuery(lastGood.filters.q);
    setSentQuery(lastGood.filters.q);
    setStatus(lastGood.filters.status);
    setTopic(lastGood.filters.topic);
    setLevel(lastGood.filters.level);
    setBookmarkedOnly(lastGood.filters.bookmarkedOnly);
    setRestoredCount((count) => count + 1);
  }
  useEffect(() => {
    if (restoredCount > 0) notify("toast.loadFailed");
  }, [restoredCount]);

  // Pages are merged by id so a row that moved between two page loads is not shown twice.
  const items = useMemo<LearnerQuestionItem[]>(() => {
    const seen = new Set<string>();
    const merged: LearnerQuestionItem[] = [];
    for (const page of list.data?.pages ?? [])
      for (const item of page.items)
        if (!seen.has(item.id)) {
          seen.add(item.id);
          merged.push(item);
        }
    return merged;
  }, [list.data]);
  const firstPage = list.data?.pages[0];

  const selected = items.find((q) => q.id === selectedId) ?? items[0] ?? null;
  const detail = useLearnerQuestionDetail(selected?.id ?? null);
  const selectedRecall = detail.data?.recall ?? null;

  // The question was retired while the learner looked at it (INB-010 on the detail read).
  const goneId = detail.data === null ? (selected?.id ?? null) : null;
  useEffect(() => {
    if (!goneId) return;
    dropQuestion(goneId);
    notify("toast.unavailable");
  }, [goneId, dropQuestion]);

  function failureToast(error: unknown, fallback: string) {
    toast.error(isQuestionGone(error) ? t("toast.unavailable") : fallback);
  }

  function startDrill() {
    drillMutation.mutate(filters, {
      onSuccess: (cards) => {
        if (cards.length === 0) {
          toast.info(t("toast.drillEmpty"));
          return;
        }
        setDrillQueue(cards);
        setDrillIndex(0);
        setRevealed(false);
        setView("drill");
      },
      onError: () => toast.error(t("toast.drillFailed")),
    });
  }

  function rateDrillCard(rating: RecallLevel) {
    const current = drillQueue[drillIndex];
    if (!current) return;
    // Not awaited: the next card shows at once; a failure only warns (BD:541).
    rateMutation.mutate(
      { id: current.id, level: rating },
      { onError: () => toast.warning(t("toast.drillRateFailed")) },
    );
    if (drillIndex + 1 >= drillQueue.length) {
      setView("browse");
      toast.success(t("toast.drillDone", { count: drillQueue.length }));
      return;
    }
    setDrillIndex((i) => i + 1);
    setRevealed(false);
  }

  function rateSelected(id: string, rating: RecallLevel) {
    rateMutation.mutate(
      { id, level: rating },
      {
        onSuccess: () => toast.success(t("toast.rated")),
        onError: (error) => failureToast(error, t("toast.rateFailed")),
      },
    );
  }

  function toggleBookmark(item: LearnerQuestionItem) {
    const target = !item.bookmarked;
    bookmarkMutation.mutate(
      { id: item.id, bookmarked: target },
      {
        onSuccess: () =>
          toast.success(
            target ? t("toast.bookmarked") : t("toast.unbookmarked"),
          ),
        onError: (error) => failureToast(error, t("toast.bookmarkFailed")),
      },
    );
  }

  // The list filters while typing (after the debounce, no toast per keystroke); Enter is the explicit search.
  function announceSearch() {
    setSentQuery(query);
    const count = firstPage?.shown ?? 0;
    toast.info(
      count > 0 ? t("toast.searchResult", { count }) : t("toast.searchEmpty"),
    );
  }

  const recallLabels: Record<RecallLevel, string> = {
    known: t("recall.known"),
    vague: t("recall.vague"),
    forgotten: t("recall.forgotten"),
  };
  const recallBadgeLabel = (rating: RecallLevel | null) =>
    rating ? recallLabels[rating] : t("recall.unrated");

  if (view === "drill") {
    const card = drillQueue[drillIndex];
    if (!card) return null;
    return (
      <div className="mx-auto max-w-[780px] p-6">
        <div className="mb-3 flex items-center justify-between gap-3">
          <span className="font-mono text-[11.5px] font-semibold text-[var(--color-text-subtle)]">
            {t("drill.counter", {
              current: drillIndex + 1,
              total: drillQueue.length,
            })}
          </span>
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            onClick={() => setView("browse")}
          >
            {t("drill.exit")}
          </Button>
        </div>
        <ProgressBar
          value={drillIndex + 1}
          max={drillQueue.length}
          label={t("drill.progressLabel")}
          fill="var(--color-primary)"
          height={4}
          className="mb-4"
        />
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
            {card.title}
          </h2>
          {revealed && card.suggestedApproach.length > 0 ? (
            <ul className="mb-6 flex flex-col gap-2 border-t border-[var(--color-border)] pt-4">
              {card.suggestedApproach.map((point, index) => (
                <li
                  key={`${index}-${point}`}
                  className="flex gap-2.5 text-[13.5px] text-[var(--color-text-muted)]"
                >
                  <span className="font-mono text-[11.5px] font-semibold text-[var(--color-primary)]">
                    {String(index + 1).padStart(2, "0")}
                  </span>
                  <span>{point}</span>
                </li>
              ))}
            </ul>
          ) : null}
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-4">
            {card.suggestedApproach.length > 0 ? (
              <Button
                variant="ghost"
                size="sm"
                className="border border-[var(--color-border)]"
                aria-expanded={revealed}
                onClick={() => setRevealed((r) => !r)}
              >
                {revealed ? t("drill.hide") : t("drill.reveal")}
              </Button>
            ) : (
              <span />
            )}
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

  const shown = firstPage?.shown ?? 0;
  const total = firstPage?.total ?? 0;

  return (
    <div className="p-6">
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-lg font-bold tracking-tight">{t("title")}</h1>
          <p className="text-[12.5px] text-[var(--color-text-muted)]">
            {t("subtitle")}
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="ghost"
            size="md"
            className="border border-[var(--color-border)]"
            disabled={shown === 0 || list.isFetching || drillMutation.isPending}
            onClick={startDrill}
          >
            <Layers className="mr-2 h-4 w-4" aria-hidden="true" />
            {t("startDrill", { count: Math.min(DRILL_SIZE, shown) })}
          </Button>
          <Button asChild variant="primary" size="md">
            <Link href="/mock-interview">
              <Mic className="mr-2 h-4 w-4" aria-hidden="true" />
              {t("startMockInterview")}
            </Link>
          </Button>
        </div>
      </div>

      <FilterBar
        search={{
          label: t("searchLabel"),
          placeholder: t("searchPlaceholder"),
          value: query,
          onChange: setQuery,
          onSubmit: announceSearch,
          maxLength: SEARCH_MAX_LENGTH,
        }}
      >
        <FilterMenu
          label={t("statusFilterLabel")}
          value={status}
          onValueChange={setStatus}
          options={[
            { value: "all" as const, label: t("statusTabs.all") },
            { value: "new" as const, label: t("statusTabs.new") },
            {
              value: "reviewing" as const,
              label: t("statusTabs.reviewing"),
            },
            { value: "known" as const, label: t("statusTabs.known") },
          ]}
        />
        <FilterMenu
          label={t("topicFilterLabel")}
          value={topic}
          onValueChange={setTopic}
          options={[
            { value: "all", label: t("filterAll") },
            ...topicList.map((item) => ({
              value: item.key,
              label: item.label,
            })),
          ]}
        />
        <FilterMenu
          label={t("levelFilterLabel")}
          value={level}
          onValueChange={setLevel}
          options={[
            { value: "all", label: t("filterAll") },
            ...levelList.map((item) => ({
              value: item.key,
              label: item.label,
            })),
          ]}
        />
        <Toggle
          showLabel
          checked={bookmarkedOnly}
          onCheckedChange={setBookmarkedOnly}
          label={t("bookmarkedOnly")}
        />
      </FilterBar>

      <div className="grid items-start gap-4 lg:[grid-template-columns:minmax(0,5fr)_minmax(0,6fr)]">
        <Card className="!p-0 overflow-hidden">
          <p
            role="status"
            className="border-b border-[var(--color-border)] px-4 py-2.5 text-[12px] font-semibold text-[var(--color-text-muted)]"
          >
            {firstPage ? t("shownCount", { shown, total }) : " "}
          </p>
          <div
            className="max-h-[68dvh] overflow-y-auto"
            aria-busy={list.isFetching || undefined}
          >
            {list.isPending ? (
              <div className="flex flex-col gap-2 p-4">
                {Array.from({ length: SKELETON_ROWS }, (_, index) => (
                  <Skeleton key={index} className="h-12 w-full" />
                ))}
              </div>
            ) : list.isError && !firstPage ? (
              <QueryError query={list} />
            ) : items.length === 0 ? (
              <EmptyState>
                {total === 0 ? t("emptyBank") : t("emptyFiltered")}
              </EmptyState>
            ) : (
              <>
                {items.map((question) => (
                  <InterviewQuestionListRow
                    key={question.id}
                    question={question}
                    topicLabel={topicLabel(topicList, question.topic)}
                    levelLabel={levelLabel(levelList, question.level)}
                    levelTone={levelTone(levelList, question.level)}
                    recallLabel={recallBadgeLabel(question.recall)}
                    recall={question.recall}
                    selected={question.id === selected?.id}
                    bookmarked={question.bookmarked}
                    bookmarkLabel={t("bookmarkLabel")}
                    onSelect={() => setSelectedId(question.id)}
                    onToggleBookmark={() => toggleBookmark(question)}
                  />
                ))}
                {list.isFetchNextPageError ? (
                  <p className="px-4 py-3 text-center text-[12.5px] text-[var(--color-text-muted)]">
                    {t("loadMoreFailed")}
                  </p>
                ) : null}
                {list.hasNextPage ? (
                  <div className="p-3 text-center">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="border border-[var(--color-border)]"
                      disabled={list.isFetchingNextPage}
                      onClick={() => void list.fetchNextPage()}
                    >
                      {list.isFetchNextPageError
                        ? t("loadMoreRetry")
                        : t("loadMore")}
                    </Button>
                  </div>
                ) : null}
              </>
            )}
          </div>
        </Card>

        <Card className="sticky top-4 self-start">
          {selected ? (
            <>
              <div className="mb-3 flex items-center gap-2">
                <Badge variant="neutral">
                  {topicLabel(topicList, selected.topic)}
                </Badge>
                <span className="text-[11.5px] font-semibold text-[var(--color-text-muted)]">
                  {levelLabel(levelList, selected.level)}
                </span>
              </div>
              <h2 className="mb-5 text-xl font-bold tracking-tight text-pretty">
                {selected.title}
              </h2>

              {detail.isError ? (
                <div className="mb-4 flex flex-col items-start gap-2">
                  <p className="text-[13px] text-[var(--color-text-muted)]">
                    {t("quickView.loadFailed")}
                  </p>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="border border-[var(--color-border)]"
                    onClick={() => void detail.refetch()}
                  >
                    {t("quickView.retry")}
                  </Button>
                </div>
              ) : !detail.data ? (
                <div className="mb-4 flex flex-col gap-2">
                  <Skeleton className="h-8 w-full" />
                  <Skeleton className="h-8 w-full" />
                </div>
              ) : (
                <>
                  {detail.data.suggestedApproach.length > 0 ? (
                    <>
                      <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                        {t("quickView.outlineLabel")}
                      </p>
                      <ul className="mb-4 flex flex-col gap-2">
                        {detail.data.suggestedApproach.map((point, index) => (
                          <li
                            key={`${index}-${point}`}
                            className="flex gap-2.5 rounded-lg bg-[var(--color-surface-hover)] px-3 py-2 text-[13px] text-[var(--color-text-muted)]"
                          >
                            <span className="font-mono text-[11px] font-semibold text-[var(--color-primary)]">
                              {String(index + 1).padStart(2, "0")}
                            </span>
                            <span>{point}</span>
                          </li>
                        ))}
                      </ul>
                    </>
                  ) : null}

                  {detail.data.coreKeywords.length > 0 ? (
                    <>
                      <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                        {t("quickView.keywordsLabel")}
                      </p>
                      <div className="mb-4 flex flex-wrap gap-1.5">
                        {detail.data.coreKeywords.map((keyword) => (
                          <Badge key={keyword} variant="neutral">
                            {keyword}
                          </Badge>
                        ))}
                      </div>
                    </>
                  ) : null}
                </>
              )}

              <div className="border-t border-[var(--color-border)] pt-3">
                <p className="mb-2 text-[10.5px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                  {t("quickView.recallLabel")}
                </p>
                <RecallLevelPicker
                  value={selected.recall}
                  onRate={(rating) => rateSelected(selected.id, rating)}
                  labels={recallLabels}
                  groupLabel={t("quickView.recallLabel")}
                  disabled={rateMutation.isPending}
                  className="mb-2"
                />
                {selectedRecall ? (
                  <p className="mb-3 text-[12px] text-[var(--color-text-subtle)]">
                    {nextReviewDays(selectedRecall.nextReviewAt) >= 1
                      ? t("quickView.nextReview", {
                          n: nextReviewDays(selectedRecall.nextReviewAt),
                        })
                      : t("quickView.nextReviewDue")}
                  </p>
                ) : null}
                <SeeAllLink
                  href={`/interview-bank/${selected.id}`}
                  className="mt-0"
                >
                  {t("quickView.viewDetail")}
                </SeeAllLink>
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

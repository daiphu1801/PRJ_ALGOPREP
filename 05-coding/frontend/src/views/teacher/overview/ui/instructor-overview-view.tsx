// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Instructor overview (INS0101), rebuilt 2026-10-18 on the owner's request "đẹp và đầy đủ" from shared parts
// (shared first), compacted 2026-11-22 to follow the admin overview: one `@container` root so breakpoints follow
// the width left beside the sidebar (not the viewport); left column = "Học viên" (summary + half-donut + absent by
// class) over "Lớp của tôi" and "Bài giao cho lớp"; right column = "Lịch dạy trong tuần", taken out of the flow so
// a long week scrolls inside its card instead of stretching the row; bottom row = daily chart (mock only),
// "Bài nộp gần đây", "Hoạt động gần đây". The two KPI stat cards were folded into the "Học viên" card head (they
// repeated its numbers). The old blocks "Cần chấm tay", the card "Điểm TB lớp" and the chart "Tiến độ học viên"
// stay dropped (DEC-2026-1013-remove-instructor-manual-grading, DEC-2026-1014-instructor-overview-trim-and-nav-label).
// Not yet approved by the owner: the added blocks (daily chart, recent submissions, weekly schedule) and the
// 2026-11-22 layout.
"use client";

import { UserAvatar } from "@/entities/user";
import { useMemo, useSyncExternalStore, type ReactNode } from "react";
import Link from "next/link";
import { useTopAssignedProblems } from "@/entities/class-assignment";
import {
  useClassStudents,
  useClassActivity,
  useInstructorClassSummary,
  useInstructorClassTotals,
} from "@/entities/class";
import {
  SUBMISSION_STATUS_VARIANT,
  useInstructorReadableSubmissions,
} from "@/entities/submission";
import {
  buildSubmissionRows,
  useInstructorSubmissionPage,
} from "@/features/submission-rows";
import { isMockMode } from "@/shared/api";
import { useT } from "@/shared/i18n";
import {
  AgendaList,
  Badge,
  Card,
  ColumnChart,
  HalfDonutGauge,
  RankedProgressList,
  QueryError,
  RecentList,
  Skeleton,
  StackedBarChart,
  type AgendaDay,
} from "@/shared/ui";

const LINK_CLASS =
  "text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline";
const CLASS_COLOR_VARS = [
  "--color-admin-teal",
  "--color-admin-cyan",
  "--color-admin-warn",
];
// Each list block shows at most this many rows so the whole page reads in one screen.
const ACTIVITY_LIMIT = 5;
const DAY_KEYS = [1, 2, 3, 4, 5, 6, 7] as const;
const DAY_MS = 86_400_000;
const FAILED_STATUSES = [
  "WRONG_ANSWER",
  "TIME_LIMIT_EXCEEDED",
  "RUNTIME_ERROR",
  "COMPILE_ERROR",
];

// ISO weekday of today (1 = Monday ... 7 = Sunday); 0 on the server so the first paint carries no highlight.
const noSubscription = () => () => {};
const useToday = () =>
  useSyncExternalStore(
    noSubscription,
    () => new Date().getDay() || 7,
    () => 0,
  );

const dayMonth = (time: number) =>
  new Date(time).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
  });

/** Skeleton while a block loads, the shared retry state when it fails, its body otherwise. */
function BlockBody({
  query,
  children,
}: {
  query: { isLoading: boolean; isError: boolean; refetch: () => unknown };
  children: ReactNode;
}) {
  if (query.isLoading) return <Skeleton className="h-24" />;
  if (query.isError) return <QueryError query={query} />;
  return children;
}

export function InstructorOverviewView() {
  const t = useT("instructorOverview");
  const tVerdict = useT("submissionResult");
  const tActivity = useT("instructorOverview.activity");
  const today = useToday();

  // F1 #56 carries completion and absent counts; when F4 is down it falls back to the plain list (#19).
  const classesQuery = useInstructorClassSummary();
  const classes = classesQuery.data ?? [];
  const totals = useInstructorClassTotals().data;
  const activityQuery = useClassActivity();
  const activity = activityQuery.data;
  const topQuery = useTopAssignedProblems();
  const topAssignments = topQuery.data ?? [];
  const students = useClassStudents().data;
  const mock = isMockMode();

  // Without the F4 numbers (the #19 fallback) nothing is drawn instead of a row of invented zeros
  // (ponytail rung 2, PROTOTYPE_DEBT lớp học).
  const hasMetrics = classes.some((c) => c.absentCount !== undefined);
  // #56 counts each person once; summing the cards would count a student of two classes twice.
  const totalStudents =
    totals?.studentCount ?? classes.reduce((sum, c) => sum + c.studentCount, 0);
  const absentStudents =
    totals?.absentStudentCount ??
    classes.reduce((sum, c) => sum + (c.absentCount ?? 0), 0);
  const activeStudents = totalStudents - absentStudents;
  const activePercent =
    totalStudents > 0 ? Math.round((activeStudents / totalStudents) * 100) : 0;

  // Mock: rows are built from the fixtures. Real: the first page of F4 #13 (newest first, leftClass included).
  const readableQuery = useInstructorReadableSubmissions(mock);
  const realPage = useInstructorSubmissionPage({
    result: "all",
    q: "",
    page: 1,
  });
  const rows = useMemo(
    () => buildSubmissionRows(students ?? [], readableQuery.data ?? []),
    [students, readableQuery.data],
  );
  const recentRows = (mock ? rows : (realPage.data?.items ?? [])).slice(0, 5);

  // Daily chart: the 7 days that END at the newest submission, so the mock data (fixed dates) still shows.
  // ponytail: the real chart ends today and is computed at query time from the submissions (cache 15 min).
  const dailyBars = useMemo(() => {
    if (rows.length === 0) return [];
    const last = new Date(rows[0]!.submittedAt);
    last.setHours(0, 0, 0, 0);
    return Array.from({ length: 7 }, (_, index) => {
      const start = last.getTime() - (6 - index) * DAY_MS;
      const day = rows.filter(
        (row) => row.submittedAt >= start && row.submittedAt < start + DAY_MS,
      );
      const accepted = day.filter((row) => row.status === "ACCEPTED").length;
      const failed = day.filter((row) =>
        FAILED_STATUSES.includes(row.status),
      ).length;
      return {
        label: dayMonth(start),
        title: `${dayMonth(start)}: ${day.length}`,
        values: { accepted, failed, other: day.length - accepted - failed },
      };
    });
  }, [rows]);

  const agendaDays: AgendaDay[] = DAY_KEYS.map((day) => ({
    key: String(day),
    label: t(`schedule.day.${day}`),
    highlighted: day === today,
    entries: classes
      .filter((klass) => klass.schedule?.days.includes(day))
      .sort((a, b) =>
        (a.schedule?.startTime ?? "").localeCompare(
          b.schedule?.startTime ?? "",
        ),
      )
      .map((klass) => ({
        id: `${klass.id}-${day}`,
        time: klass.schedule?.startTime ?? "",
        title: klass.name,
        meta: [
          klass.schedule?.startTime && klass.schedule?.endTime
            ? `${klass.schedule.startTime}–${klass.schedule.endTime}`
            : "",
          klass.schedule?.room,
        ]
          .filter(Boolean)
          .join(" · "),
        colorVar:
          CLASS_COLOR_VARS[
            Math.max(0, classes.indexOf(klass)) % CLASS_COLOR_VARS.length
          ],
      })),
  }));

  if (classesQuery.isError) return <QueryError query={classesQuery} />;

  // Only classes with someone absent are listed; a column of "0 vắng" rows is noise.
  const absentClasses = classes.filter((klass) => (klass.absentCount ?? 0) > 0);
  const summary = `${t("kpi.classesCount", { count: classes.length })} · ${t("kpi.classesMeta", { count: totalStudents })}`;

  return (
    <div className="@container flex min-w-0 flex-1 flex-col gap-3">
      <h1 className="sr-only">{t("title")}</h1>

      {/* One grid of six cards, each holding at most ~5 rows, so the page reads in a single screen: row 1 =
          students, class completion, assignments; row 2 = schedule, recent submissions, recent activity. */}
      <div className="grid grid-cols-1 gap-3 @min-[640px]:grid-cols-2 @min-[960px]:grid-cols-3">
        {hasMetrics ? (
          <Card
            className="h-full"
            title={t("students.title")}
            description={summary}
          >
            <div className="@container">
              <div
                className={`grid items-center gap-3 ${absentClasses.length > 0 ? "@min-[520px]:grid-cols-2" : ""}`}
              >
                <HalfDonutGauge
                  chartClassName="max-w-[180px]"
                  center={
                    <>
                      <span className="block text-[22px] leading-none font-bold">
                        {activePercent}%
                      </span>
                      <span className="text-[11px] text-[var(--color-text-muted)]">
                        {t("students.activeRate")}
                      </span>
                    </>
                  }
                  slices={[
                    {
                      label: t("students.active"),
                      value: activeStudents,
                      colorVar: "--color-admin-teal",
                    },
                    {
                      label: t("students.absent"),
                      value: absentStudents,
                      colorVar: "--color-admin-warn",
                    },
                  ]}
                />
                {absentClasses.length > 0 ? (
                  <div className="min-w-0">
                    <p className="mb-2 text-[12px] font-semibold text-[var(--color-text-muted)]">
                      {t("students.byClass")}
                    </p>
                    <RankedProgressList
                      numbered={false}
                      accentColorVar="--color-admin-warn"
                      items={absentClasses.map((klass) => ({
                        label: klass.name,
                        value: klass.absentCount ?? 0,
                        valueLabel: t("students.absentCount", {
                          count: klass.absentCount ?? 0,
                        }),
                      }))}
                    />
                  </div>
                ) : null}
              </div>
            </div>
          </Card>
        ) : (
          <Card
            className="h-full"
            title={t("kpi.classes")}
            description={summary}
          >
            <p className="text-[26px] leading-none font-bold">
              {classes.length}
            </p>
          </Card>
        )}

        <Card
          className="flex h-full flex-col"
          title={t("classes.title")}
          description={t("classes.subtitle")}
          action={
            <Link href="/instructor/classes" className={LINK_CLASS}>
              {t("classes.linkManage")}
            </Link>
          }
        >
          <ColumnChart
            className="flex-1"
            max={100}
            accentColorVar="--color-admin-teal"
            items={classes.map((klass) => ({
              label: klass.name,
              value: klass.completionPct ?? 0,
              valueLabel:
                klass.completionPct == null ? "-" : `${klass.completionPct}%`,
            }))}
          />
        </Card>

        <Card
          className="flex h-full flex-col"
          title={t("assignments.title")}
          description={t("assignments.subtitle")}
          action={
            <Link href="/instructor/assignments" className={LINK_CLASS}>
              {t("assignments.linkViewAll")}
            </Link>
          }
        >
          <BlockBody query={topQuery}>
            {topAssignments.length === 0 ? (
              <p className="text-[13px] text-[var(--color-text-muted)]">
                {t("empty")}
              </p>
            ) : (
              <ColumnChart
                className="flex-1"
                accentColorVar="--color-admin-cyan"
                items={topAssignments.map((row) => ({
                  label: row.title,
                  value: row.submissionCount,
                  valueLabel: row.submissionCount.toLocaleString("vi-VN"),
                }))}
              />
            )}
          </BlockBody>
        </Card>

        {/* From 960px the schedule leaves the flow, so the row height comes from the two lists beside it and
            a long week scrolls inside the card instead of stretching the row. */}
        <div className="@min-[960px]:relative @min-[960px]:min-h-[16rem]">
          <Card
            className="flex h-full flex-col @min-[960px]:absolute @min-[960px]:inset-0"
            title={t("schedule.title")}
            description={t("schedule.subtitle")}
          >
            <div className="-m-1 min-h-0 flex-1 overflow-y-auto p-1">
              <AgendaList days={agendaDays} emptyLabel={t("schedule.empty")} />
            </div>
          </Card>
        </div>

        <Card
          className="h-full"
          title={t("submissions.title")}
          description={t("submissions.subtitle")}
          action={
            <Link href="/instructor/submissions" className={LINK_CLASS}>
              {t("submissions.linkViewAll")}
            </Link>
          }
        >
          <BlockBody query={mock ? readableQuery : realPage}>
            <RecentList
              emptyLabel={t("submissions.empty")}
              items={recentRows.map((row) => ({
                id: row.key,
                href: `/instructor/submissions/${row.submissionId}?studentId=${row.studentId}&classId=${row.classId}&from=submissions`,
                leading: (
                  <UserAvatar
                    userId={row.studentId}
                    name={row.studentName}
                    avatarVersion={row.studentAvatarVersion}
                    size="sm"
                  />
                ),
                title: row.studentName,
                subtitle: `${row.problemTitle} · ${new Date(
                  row.submittedAt,
                ).toLocaleString("vi-VN", {
                  dateStyle: "short",
                  timeStyle: "short",
                })}`,
                trailing: (
                  <Badge variant={SUBMISSION_STATUS_VARIANT[row.status]}>
                    {tVerdict(`verdict.${row.status}`)}
                  </Badge>
                ),
              }))}
            />
          </BlockBody>
        </Card>

        <Card
          className="h-full"
          title={t("activity.title")}
          description={t("activity.subtitle", { count: ACTIVITY_LIMIT })}
        >
          <BlockBody query={activityQuery}>
            <RecentList
              emptyLabel={t("empty")}
              items={(activity ?? []).slice(0, ACTIVITY_LIMIT).map((event) => ({
                id: event.id,
                leading: (
                  <span
                    aria-hidden="true"
                    className="block h-2 w-2 rounded-full"
                    style={{
                      background: event.attention
                        ? "var(--color-admin-warn)"
                        : "var(--color-admin-teal)",
                    }}
                  />
                ),
                title: (
                  <span className="whitespace-normal">
                    {event.activityType
                      ? tActivity(`type.${event.activityType}`, {
                          name: event.actorName ?? "",
                        })
                      : event.text}
                  </span>
                ),
                subtitle: event.timeLabel,
              }))}
            />
          </BlockBody>
        </Card>

        {/* ponytail: real mode hides the daily chart (owner, 2026-11-07): no endpoint counts submissions per day
            for an instructor, and one 20-row page of #13 would draw it wrong. Add a F4 daily-counts read if the demo needs it. */}
        {mock ? (
          <Card
            className="h-full @min-[640px]:col-span-full"
            title={t("chart.title")}
            description={t("chart.subtitle")}
          >
            {dailyBars.length === 0 ? (
              <p className="text-[13px] text-[var(--color-text-muted)]">
                {t("chart.empty")}
              </p>
            ) : (
              <StackedBarChart
                height={150}
                series={[
                  {
                    key: "accepted",
                    label: t("chart.series.accepted"),
                    colorVar: "--color-admin-teal",
                  },
                  {
                    key: "failed",
                    label: t("chart.series.failed"),
                    colorVar: "--color-admin-warn",
                  },
                  {
                    key: "other",
                    label: t("chart.series.other"),
                    colorVar: "--color-admin-slate",
                  },
                ]}
                bars={dailyBars}
              />
            )}
          </Card>
        ) : null}
      </div>
    </div>
  );
}

// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Instructor overview (INS0101), rebuilt 2026-10-18 on the owner's request "đẹp và đầy đủ" from shared parts
// (shared first): row 1 = two figure cards (Lớp phụ trách, Tổng học viên) + the half-donut of learners who are
// active or absent; row 2 = "Lớp của tôi" (completion bars), "Bài giao cho lớp" (submission bars), "Lịch dạy
// trong tuần" (agenda from the class schedules); row 3 = daily submission chart, "Bài nộp gần đây" and "Hoạt động
// gần đây". The old blocks "Cần chấm tay", the card "Điểm TB lớp" and the chart "Tiến độ học viên" stay dropped
// (DEC-2026-1013-remove-instructor-manual-grading, DEC-2026-1014-instructor-overview-trim-and-nav-label).
// Not yet approved by the owner: the three added blocks (daily chart, recent submissions, weekly schedule).
"use client";

import { useMemo, useSyncExternalStore } from "react";
import Link from "next/link";
import { GraduationCap, Users } from "lucide-react";
import { useClassAssignments } from "@/entities/class-assignment";
import {
  initialsOfClassStudent,
  useClassStudents,
  useClassActivity,
  useInstructorClasses,
} from "@/entities/class";
import { SUBMISSION_STATUS_VARIANT } from "@/entities/submission";
import { buildSubmissionRows } from "@/features/submission-rows";
import { useT } from "@/shared/i18n";
import {
  AgendaList,
  Badge,
  Card,
  HalfDonutGauge,
  RankedProgressList,
  RecentList,
  StackedBarChart,
  StatCard,
  type AgendaDay,
} from "@/shared/ui";

const LINK_CLASS =
  "text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline";
const CLASS_COLOR_VARS = [
  "--color-admin-teal",
  "--color-admin-cyan",
  "--color-admin-warn",
];
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

export function InstructorOverviewView() {
  const t = useT("instructorOverview");
  const tVerdict = useT("submissionResult");
  const today = useToday();

  const classes = useInstructorClasses().data ?? [];
  const activity = useClassActivity().data;
  const assignmentsQuery = useClassAssignments();
  const students = useClassStudents().data;

  const totalStudents = classes.reduce((sum, c) => sum + c.studentCount, 0);
  const absentStudents = classes.reduce((sum, c) => sum + c.absentCount, 0);
  const activeStudents = totalStudents - absentStudents;
  const activePercent =
    totalStudents > 0 ? Math.round((activeStudents / totalStudents) * 100) : 0;

  const topAssignments = useMemo(
    () =>
      [...(assignmentsQuery.data ?? [])]
        .sort((a, b) => b.submissionCount - a.submissionCount)
        .slice(0, 5),
    [assignmentsQuery.data],
  );

  const rows = useMemo(() => buildSubmissionRows(students ?? []), [students]);
  const recentRows = rows.slice(0, 5);

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

  return (
    <div className="flex flex-col gap-3.5">
      <h1 className="sr-only">{t("title")}</h1>

      <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-3">
        <div className="grid grid-cols-2 gap-3.5 lg:flex lg:flex-col">
          <StatCard
            className="lg:flex-1"
            icon={GraduationCap}
            iconColorVar="--color-admin-teal"
            label={t("kpi.classes")}
            value={classes.length}
            meta={t("kpi.classesMeta", { count: totalStudents })}
          />
          <StatCard
            className="lg:flex-1"
            icon={Users}
            iconColorVar="--color-admin-cyan"
            label={t("kpi.students")}
            value={totalStudents}
            meta={t("kpi.studentsMeta", { count: absentStudents })}
          />
        </div>
        <Card
          className="h-full lg:col-span-2"
          title={t("students.title")}
          description={t("students.subtitle")}
        >
          <div className="grid items-center gap-5 md:grid-cols-2">
            <div className="relative mx-auto w-full max-w-[300px]">
              <HalfDonutGauge
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
              <p className="pointer-events-none absolute inset-x-0 top-[34px] text-center">
                <span className="block text-[26px] leading-none font-bold">
                  {activePercent}%
                </span>
                <span className="text-[11.5px] text-[var(--color-text-muted)]">
                  {t("students.activeRate")}
                </span>
              </p>
            </div>
            <div>
              <p className="mb-2 text-[12px] font-semibold text-[var(--color-text-muted)]">
                {t("students.byClass")}
              </p>
              <RankedProgressList
                numbered={false}
                accentColorVar="--color-admin-warn"
                items={classes.map((klass) => ({
                  label: klass.name,
                  value: klass.absentCount,
                  valueLabel: t("students.absentCount", {
                    count: klass.absentCount,
                  }),
                }))}
              />
            </div>
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-3">
        <Card
          className="h-full"
          title={t("classes.title")}
          description={t("classes.subtitle")}
          action={
            <Link href="/instructor/classes" className={LINK_CLASS}>
              {t("classes.linkManage")}
            </Link>
          }
        >
          <RankedProgressList
            numbered={false}
            accentColorVar="--color-admin-teal"
            items={classes.map((klass) => ({
              label: `${klass.name} · ${t("classes.students", { count: klass.studentCount })}`,
              value: klass.completionPct,
              valueLabel: `${klass.completionPct}%`,
            }))}
          />
        </Card>

        <Card
          className="h-full"
          title={t("assignments.title")}
          description={t("assignments.subtitle")}
          action={
            <Link href="/instructor/assignments" className={LINK_CLASS}>
              {t("assignments.linkViewAll")}
            </Link>
          }
        >
          {topAssignments.length === 0 ? (
            <p className="text-[13px] text-[var(--color-text-muted)]">
              {t("empty")}
            </p>
          ) : (
            <RankedProgressList
              numbered={false}
              accentColorVar="--color-admin-cyan"
              items={topAssignments.map((row) => ({
                label: row.title,
                value: row.submissionCount,
                valueLabel: row.submissionCount.toLocaleString("vi-VN"),
              }))}
            />
          )}
        </Card>

        <Card
          className="h-full"
          title={t("schedule.title")}
          description={t("schedule.subtitle")}
        >
          <AgendaList days={agendaDays} emptyLabel={t("schedule.empty")} />
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-3">
        <Card
          className="h-full"
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
          <RecentList
            emptyLabel={t("submissions.empty")}
            items={recentRows.map((row) => ({
              id: row.key,
              href: `/instructor/submissions/${row.submissionId}?studentId=${row.studentId}&classId=${row.classId}&from=submissions`,
              leading: (
                <span
                  aria-hidden="true"
                  className="glass-surface flex h-8 w-8 items-center justify-center rounded-full border border-[var(--color-border)] text-[11px] font-semibold"
                >
                  {initialsOfClassStudent(row.studentName)}
                </span>
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
        </Card>

        <Card
          className="h-full"
          title={t("activity.title")}
          description={t("activity.subtitle")}
        >
          <RecentList
            emptyLabel={t("empty")}
            items={(activity ?? []).map((event) => ({
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
              title: <span className="whitespace-normal">{event.text}</span>,
              subtitle: event.timeLabel,
            }))}
          />
        </Card>
      </div>
    </div>
  );
}

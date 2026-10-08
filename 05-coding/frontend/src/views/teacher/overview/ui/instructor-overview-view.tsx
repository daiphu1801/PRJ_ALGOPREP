// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Layout theo 09-layoutBase/Giáo viên - Tổng quan.dc.html: thanh công cụ tìm lớp/học viên/bài tập +
// nút "+ Tạo lớp mới" + avatar (dòng 115-125), dải 4 thẻ thống kê (dòng 127-138), hai khối "Cần chấm
// tay" (dòng 140-158) và "Hoạt động gần đây" (dòng 160-174) xếp hai cột, khối "Lớp của tôi" full
// width (dòng 177-196), hai khối "Tiến độ học viên" (dòng 198-213) và "Bài tập của tôi" (dòng
// 215-231) xếp hai cột [SoT: 01-rd/screens/teacher/INS0101_instructor_overview.md — mục 2.1, các
// điểm 3-4, 5-6, 7-9].
//
// 02-bd/screens/teacher/INS0101_overview.md Sheet 4.4: toolbar (search + "Tạo lớp mới") + 4 stat
// cards + "Cần chấm tay" / "Hoạt động gần đây" (two columns) + "Lớp của tôi" (full width) +
// "Tiến độ học viên" / "Bài tập của tôi" (two columns). Each block reads its own query so one dead
// source does not blank the others (same per-block isolation rule as admin_overview).
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Search } from "lucide-react";
import { useClassAssignments } from "@/entities/class-assignment";
import { useClassScoreTrend, useInstructorClasses } from "@/entities/class";
import { usePendingManualGradingTop } from "@/entities/manual-grading";
import { problemLevelLabel, useProblemLevels } from "@/entities/problem";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  LineChartWithTotal,
  ProgressBar,
  StatCard,
  TextField,
} from "@/shared/ui";

// No dedicated "recent activity" entity exists yet (identity.identity_recent_activity has no query
// endpoint contract — 02-bd/screens/teacher/INS0101_overview.md Sheet 4.3 Q4 is still open), so the
// feed is a small deterministic local list rather than an untyped fetch.
const ACTIVITY_FEED = [
  {
    id: "a1",
    text: "Nguyễn Văn An vừa nộp bài Two Sum — Accepted",
    timeLabel: "12 phút trước",
    attention: false,
  },
  {
    id: "a2",
    text: "Trần Thị Bích chưa nộp bài 9 ngày liên tiếp",
    timeLabel: "1 giờ trước",
    attention: true,
  },
  {
    id: "a3",
    text: "Lê Hoàng Cường tham gia lớp Lập trình Java K21",
    timeLabel: "Hôm qua",
    attention: false,
  },
  {
    id: "a4",
    text: "Đỗ Minh Đức bị AI chấm điểm thấp, cần xem lại",
    timeLabel: "Hôm qua",
    attention: true,
  },
];

export function InstructorOverviewView() {
  const t = useT("instructorOverview");
  const levelList = useProblemLevels();
  const [search, setSearch] = useState("");

  const classesQuery = useInstructorClasses();
  const classes = classesQuery.data ?? [];
  const pendingTopQuery = usePendingManualGradingTop(5);
  const assignmentsQuery = useClassAssignments();
  const trendQuery = useClassScoreTrend();

  const totalStudents = classes.reduce((sum, c) => sum + c.studentCount, 0);
  const pendingGrading = classes.reduce((sum, c) => sum + c.pendingGrading, 0);
  const avgScore = classes.length
    ? Math.round(
        (classes.reduce((sum, c) => sum + (c.avgScore ?? 0), 0) /
          classes.length) *
          10,
      ) / 10
    : null;

  const recentAssignments = useMemo(
    () => (assignmentsQuery.data ?? []).slice(0, 5),
    [assignmentsQuery.data],
  );

  // dc.html:198-213 plots a 4-week average line with week labels under it, not a single number.
  // Same aggregate-of-all-classes simplification as class-progress-view (LineChartWithTotal is
  // single-series); here the mockup itself draws only one line, so nothing is lost.
  const trendPoints = useMemo(() => {
    const series = trendQuery.data?.series ?? [];
    // Last 4 weeks only: the block's own subtitle says "4 tuần gần nhất" (dc.html:203 plots 4
    // points), while the shared trend query returns 6 — class_progress is the screen that shows
    // all of them.
    const labels = (trendQuery.data?.weekLabels ?? []).slice(-4);
    const offset = (trendQuery.data?.weekLabels.length ?? 0) - labels.length;
    return labels.map((label, i) => ({
      label,
      value: series.length
        ? Math.round(
            (series.reduce((sum, s) => sum + (s.points[offset + i] ?? 0), 0) /
              series.length) *
              10,
          ) / 10
        : 0,
    }));
  }, [trendQuery.data]);

  // dc.html:265 puts a week-over-week delta on the "Điểm TB lớp" tile. Derived from the two last
  // points of the trend series that the block below already plots — null when there is no previous
  // week to compare against, rather than a "+0.0" that pretends there was one.
  const avgScoreDelta = useMemo(() => {
    if (trendPoints.length < 2) return null;
    const diff = trendPoints.at(-1)!.value - trendPoints.at(-2)!.value;
    return Math.round(diff * 10) / 10;
  }, [trendPoints]);

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-2.5">
        <TextField
          label={t("toolbar.searchBox")}
          hideLabel
          leadingIcon={<Search className="h-3.5 w-3.5" />}
          placeholder={t("toolbar.searchBox")}
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          wrapperClassName="min-w-[220px] flex-1"
        />
        <Button variant="cta" size="sm" asChild>
          <Link href="/instructor/classes?create=1">
            {t("toolbar.btnCreateClass")}
          </Link>
        </Button>
      </div>

      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(220px,1fr))]">
        <StatCard
          label={t("stat.classCount")}
          value={classes.length}
          meta={t("stat.classCountMeta", { count: totalStudents })}
        />
        <StatCard label={t("stat.studentCount")} value={totalStudents} />
        <StatCard label={t("stat.pendingGrading")} value={pendingGrading} />
        <StatCard
          label={t("stat.avgScore")}
          value={avgScore ?? "-"}
          delta={
            avgScoreDelta === null || avgScoreDelta === 0 ? undefined : (
              <span
                style={{
                  color: `var(${avgScoreDelta > 0 ? "--color-success" : "--color-admin-negative"})`,
                }}
              >
                {avgScoreDelta > 0 ? `+${avgScoreDelta}` : avgScoreDelta}
              </span>
            )
          }
          meta={avgScoreDelta === null ? undefined : t("stat.avgScoreMeta")}
        />
      </div>

      <div className="mb-4 grid grid-cols-1 items-start gap-3.5 lg:grid-cols-[1.4fr_1fr]">
        <Card
          title={t("grading.title")}
          description={t("grading.subtitle")}
          action={
            <Link
              href="/instructor/grading"
              className="text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline"
            >
              {t("grading.linkViewAll")}
            </Link>
          }
        >
          <ul className="flex flex-col gap-2.5">
            {(pendingTopQuery.data ?? []).length === 0 ? (
              <li className="text-[13px] text-[var(--color-text-muted)]">
                {t("empty")}
              </li>
            ) : (
              pendingTopQuery.data!.map((item) => (
                <li
                  key={item.id}
                  className="flex items-center justify-between gap-3 rounded-lg border border-[var(--color-border)] px-3 py-2"
                >
                  <div className="min-w-0">
                    <p className="truncate font-semibold">
                      {item.studentName} · {item.problemTitle}
                    </p>
                    <p className="truncate text-[12px] text-[var(--color-text-muted)]">
                      {t("grading.col.meta", {
                        score: item.aiScore10,
                        time: item.submittedAtLabel,
                      })}
                    </p>
                  </div>
                  <Badge
                    variant={item.aiScore10 <= 4 ? "negative" : "warn"}
                  >{`${item.aiScore10}/10`}</Badge>
                </li>
              ))
            )}
          </ul>
        </Card>

        <Card title={t("activity.title")} description={t("activity.subtitle")}>
          <ul className="flex flex-col gap-2.5">
            {ACTIVITY_FEED.map((event) => (
              <li key={event.id} className="flex items-start gap-2.5">
                <span
                  aria-hidden="true"
                  className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full"
                  style={{
                    background: event.attention
                      ? "var(--color-admin-warn)"
                      : "var(--color-admin-teal)",
                  }}
                />
                <div className="min-w-0">
                  <p className="text-[13px]">{event.text}</p>
                  <p className="text-[11.5px] text-[var(--color-text-muted)]">
                    {event.timeLabel}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      <Card
        title={t("classes.title")}
        className="mb-4"
        action={
          <Link
            href="/instructor/classes"
            className="text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline"
          >
            {t("classes.linkManage")}
          </Link>
        }
      >
        <div className="grid gap-3 [grid-template-columns:repeat(auto-fit,minmax(220px,1fr))]">
          {classes.map((klass) => (
            <div
              key={klass.id}
              className="rounded-xl border border-[var(--color-border)] p-3"
            >
              <p className="mb-2 truncate font-semibold">{klass.name}</p>
              <ProgressBar
                fill="var(--instructor-progress-fill, var(--color-admin-teal))"
                value={klass.completionPct}
                label={klass.name}
                className="mb-1.5"
              />
              <p className="flex justify-between text-[12px] text-[var(--color-text-muted)]">
                <span>
                  {t("classes.col.studentCount", { count: klass.studentCount })}
                </span>
                <span>{klass.completionPct}%</span>
              </p>
            </div>
          ))}
        </div>
      </Card>

      <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-2">
        <Card
          title={t("trend.title")}
          action={
            <Link
              href="/instructor/students"
              className="text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline"
            >
              {t("trend.linkViewAll")}
            </Link>
          }
        >
          {trendPoints.length ? (
            <LineChartWithTotal
              points={trendPoints}
              totalLabel={t("trend.subtitle")}
              totalValue={avgScore !== null ? `${avgScore}/10` : "-"}
            />
          ) : (
            <p className="text-[13px] text-[var(--color-text-muted)]">
              {t("empty")}
            </p>
          )}
        </Card>

        <Card
          title={t("assignments.title")}
          action={
            <Link
              href="/instructor/assignments"
              className="text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline"
            >
              {t("assignments.linkViewAll")}
            </Link>
          }
        >
          <ul className="flex flex-col gap-2">
            {recentAssignments.length === 0 ? (
              <li className="text-[13px] text-[var(--color-text-muted)]">
                {t("empty")}
              </li>
            ) : (
              recentAssignments.map((row) => (
                <li
                  key={row.id}
                  className="flex items-center gap-2.5 text-[13px]"
                >
                  <div className="min-w-0 flex-1">
                    <p className="truncate">{row.title}</p>
                    <p className="truncate text-[11.5px] text-[var(--color-text-muted)]">
                      {problemLevelLabel(levelList, row.difficulty)} ·{" "}
                      {row.topic}
                    </p>
                  </div>
                  <span className="shrink-0 font-mono text-[12.5px] text-[var(--color-text-muted)]">
                    {row.submissionCount.toLocaleString("vi-VN")}
                  </span>
                </li>
              ))
            )}
          </ul>
        </Card>
      </div>
    </div>
  );
}

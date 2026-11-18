// USR0601 (student dashboard) against the REAL backend: both halves of the merged screen with a learner's
// own data. Replaces the mock-mode `student-dashboard.spec.ts` (the number 182 came from mock data). The test
// seeds one published problem and two submissions for a fresh student in the database and derives the expected
// figures from those rows. Needs the stack in e2e/README.md; skips itself otherwise.
import { expect, test } from "@playwright/test";
import { APP_URL, requireRealStack, sql, vi } from "./support/real-stack";
import { loginAsStudent, userId } from "./support/real-session";

requireRealStack();

const m = vi.dashboard;
const mp = vi.myProgress;
const LEVEL = "20000000-0000-4000-8000-000000000001";

function submission(studentId: string, problemId: string, status: string) {
  const accepted = status === "ACCEPTED";
  sql(
    `insert into judge.submissions(id,user_id,problem_id,spec_version,language,submission_model,source_code,source_input_method,status,passed_testcase_count,total_testcase_count,score_ratio,runtime_ms,memory_kb,testcase_set_version_at_grading,submitted_at,finished_at)
     values(gen_random_uuid(),'${studentId}','${problemId}',1,'PYTHON','STANDARD_IO','print(1)','EDITOR','${status}',${accepted ? 2 : 1},2,${accepted ? "1.000" : "0.500"},40,2048,1,now(),now())`,
  );
}

test("a learner with submissions sees both halves with figures that match the database", async ({
  page,
  request,
}) => {
  const student = await loginAsStudent(page, request, "dash-data");
  const id = userId(student.email);
  const problemId = sql(
    `insert into problem.problems(id,code,title,statement_md,author_id,level_id,status,published_at,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
     values(gen_random_uuid(),'E2E-DASH-${Date.now()}','E2E-DASH problem','Read two integers, print the sum.','${id}','${LEVEL}','PUBLISHED',now(),1000,256,64,false)
     returning id`,
  ).split("\n")[0]!;
  submission(id, problemId, "WRONG_ANSWER");
  submission(id, problemId, "ACCEPTED");
  // The progress screens read the personal read models (identity.user_*), which the backend fills from
  // submission events. The test fills them directly so the figures are known: this run covers the screen,
  // not the event pipeline (that has its own backend flow tests).
  sql(
    `insert into identity.user_problem_best_score(user_id,problem_id,best_ratio,best_verdict) values('${id}','${problemId}',1,'ACCEPTED');
     insert into identity.user_submission_stats(user_id,total_submissions,ac_eligible_count,accepted_count,first_try_accepted_count,top_language,language_counts) values('${id}',2,2,1,0,'PYTHON','{"PYTHON":2}');
     insert into identity.user_daily_submission_stats(user_id,day,submission_count) values('${id}',current_date,2);`,
  );
  const solved = Number(
    sql(
      `select count(*) from identity.user_problem_best_score where user_id='${id}' and best_verdict='ACCEPTED'`,
    ),
  );
  const total = Number(
    sql(
      `select total_submissions from identity.user_submission_stats where user_id='${id}'`,
    ),
  );
  const accepted = Number(
    sql(
      `select accepted_count from identity.user_submission_stats where user_id='${id}'`,
    ),
  );

  await page.goto(`${APP_URL}/dashboard`);
  await expect(
    page.getByRole("heading", { name: m.greeting.title }),
  ).toBeVisible({ timeout: 15_000 });

  // Dashboard half: the blocks of the prototype.
  for (const block of [
    m.daily.title,
    m.radar.title,
    m.activity.title,
    m.suggested.title,
    m.interviews.title,
    m.reviews.title,
  ]) {
    await expect(
      page.getByRole("region", { name: block, exact: true }),
    ).toBeVisible();
  }

  // Figures: solved count and the acceptance meta come from this learner's rows.
  const solvedCard = page.getByRole("region", {
    name: m.stats.regionLabel.replace("{metric}", m.stats.solved),
  });
  await expect(solvedCard).toContainText(String(solved), { timeout: 15_000 });
  const acCard = page.getByRole("region", {
    name: m.stats.regionLabel.replace("{metric}", m.stats.acRate),
  });
  await expect(acCard).toContainText(
    m.stats.acRateMeta
      .replace("{submissions}", String(total))
      .replace("{accepted}", String(accepted)),
  );

  // my_progress half merged into the same page.
  await expect(
    page.getByRole("table", { name: mp.topics.title, exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: mp.focus.title }),
  ).toBeVisible();
  // Area chrome: the footer belongs to every student screen.
  await expect(page.getByRole("contentinfo")).toBeVisible();
});

test("a brand-new learner gets honest empty states instead of invented numbers", async ({
  page,
  request,
}) => {
  await loginAsStudent(page, request, "dash-empty");
  await expect(
    page.getByRole("heading", { name: m.greeting.title }),
  ).toBeVisible({ timeout: 15_000 });
  // The catalogue has topics, so the greeting names the weakest one at 0/100 rather than "no data".
  await expect(
    page.getByText(/Điểm yếu tuần này: .+ năng lực 0\/100/),
  ).toBeVisible();
  await expect(
    page.getByText(m.stats.acRateEmpty, { exact: true }),
  ).toBeVisible();
  await expect(page.getByText(m.interviews.empty).first()).toBeVisible();
  await expect(page.getByText(m.reviews.empty)).toBeVisible();
});

test("Tổng quan is the first nav item and the destination the brand links to", async ({
  page,
  request,
}) => {
  await loginAsStudent(page, request, "dash-nav");
  await page.goto(`${APP_URL}/problems`);
  const nav = page.getByRole("navigation", { name: vi.nav.area.student });
  await nav.getByRole("link", { name: vi.nav.dashboard }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(
    nav.getByRole("link", { name: vi.nav.dashboard }),
  ).toHaveAttribute("aria-current", "page");
  await expect(nav.getByRole("link", { name: vi.nav.progress })).toHaveCount(0);
});

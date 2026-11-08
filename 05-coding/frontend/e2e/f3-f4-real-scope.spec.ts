import { randomUUID } from "node:crypto";
import { expect, test, type APIRequestContext } from "@playwright/test";
import {
  APP_URL,
  createUser,
  requireRealStack,
  sql,
  submitLogin,
  type TestUser,
} from "./support/real-stack";

// FE-08: class scope with two instructors and real server-side paging. Seeded by SQL (the same
// way the other real-stack specs seed fixtures); run with E2E_PG_DB set to the backend database.
requireRealStack();
test.setTimeout(120000);

const ROWS = 25;
const PAGE_SIZE = 20;

async function tokenOf(
  request: APIRequestContext,
  user: TestUser,
): Promise<string> {
  const login = await request.post(`${APP_URL}/api/v1/identity/auth/login`, {
    data: { email: user.email, password: user.password },
  });
  expect(login.ok()).toBe(true);
  return (await login.json()).accessToken;
}

const idOf = (email: string) =>
  sql(`select id from identity.users where email='${email}'`);

test("an instructor sees only their own class submissions, paged by the server, and cannot reach another class", async ({
  page,
  request,
}) => {
  const problemId = sql(
    "select id from problem.problems where status='PUBLISHED' order by published_at desc limit 1",
  );
  test.skip(problemId === "", "needs one published problem in the database");
  const teacherA = await createUser(request, {
    tag: "f4-scope-a",
    role: "INSTRUCTOR",
  });
  const teacherB = await createUser(request, {
    tag: "f4-scope-b",
    role: "INSTRUCTOR",
  });
  const student = await createUser(request, { tag: "f4-scope-student" });
  const studentId = idOf(student.email);
  const classA = randomUUID();
  const classB = randomUUID();
  sql(`insert into identity.classes(id,instructor_id,name) values
      ('${classA}','${idOf(teacherA.email)}','F4 scope class A'),
      ('${classB}','${idOf(teacherB.email)}','F4 scope class B');
    insert into identity.class_enrollments(id,class_id,student_id,joined_at)
      values(gen_random_uuid(),'${classA}','${studentId}',now() - interval '2 days');
    insert into problem.class_assignments(id,problem_id,class_id,assigned_by,assigned_at)
      values(gen_random_uuid(),'${problemId}','${classA}','${idOf(teacherA.email)}',now() - interval '2 days');
    insert into judge.submissions(id,user_id,problem_id,spec_version,language,submission_model,source_code,source_input_method,status,passed_testcase_count,total_testcase_count,score_ratio,testcase_set_version_at_grading,submitted_at,finished_at,updated_at)
    select gen_random_uuid(),'${studentId}','${problemId}',1,'PYTHON','FUNCTION_WRAPPER','SCOPE-SOURCE-SENTINEL','EDITOR','ACCEPTED',4,4,1,1,
      now() - (i || ' minutes')::interval, now() - (i || ' minutes')::interval, now() - (i || ' minutes')::interval
    from generate_series(1,${ROWS}) i;`);
  const headersA = {
    Authorization: `Bearer ${await tokenOf(request, teacherA)}`,
  };
  const headersB = {
    Authorization: `Bearer ${await tokenOf(request, teacherB)}`,
  };
  const list = (headers: object, query = "") =>
    request.get(`${APP_URL}/api/v1/judge/instructor/submissions${query}`, {
      headers: headers as Record<string, string>,
    });

  // Server paging: a fixed 20-row page, then the remainder.
  const first = await (await list(headersA, "?page=1")).json();
  expect(first.items).toHaveLength(PAGE_SIZE);
  expect(first.totalCount).toBe(ROWS);
  expect(first.pageSize).toBe(PAGE_SIZE);
  const second = await (await list(headersA, "?page=2")).json();
  expect(second.items).toHaveLength(ROWS - PAGE_SIZE);
  const ids = new Set(
    [...first.items, ...second.items].map(
      (item: { submissionId: string }) => item.submissionId,
    ),
  );
  expect(ids.size).toBe(ROWS);

  // The other instructor: no rows, class filter refused, detail refused.
  const other = await (await list(headersB)).json();
  expect(other.items).toHaveLength(0);
  const foreignClass = await list(headersB, `?classId=${classA}`);
  expect(foreignClass.status()).toBe(404);
  const sample = first.items[0].submissionId as string;
  const foreignDetail = await request.get(
    `${APP_URL}/api/v1/judge/instructor/submissions/${sample}`,
    { headers: headersB },
  );
  expect([403, 404]).toContain(foreignDetail.status());
  expect(await foreignDetail.text()).not.toContain("SCOPE-SOURCE-SENTINEL");
  const ownDetail = await request.get(
    `${APP_URL}/api/v1/judge/instructor/submissions/${sample}`,
    { headers: headersA },
  );
  expect(ownDetail.status()).toBe(200);

  // The same paging through the screen.
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, teacherA.email, teacherA.password);
  await expect(page).toHaveURL(/instructor\/overview/);
  await page.goto(`${APP_URL}/instructor/submissions`);
  await expect(page.getByText("Trang 1 / 2")).toBeVisible();
  await page
    .getByRole("button", { name: /Sau|Next/ })
    .first()
    .click();
  await expect(page.getByText("Trang 2 / 2")).toBeVisible();

  // The student's own history pages the same way (API #7), on the API and on the screen.
  const mine = await request.get(
    `${APP_URL}/api/v1/judge/me/submissions?page=1`,
    {
      headers: { Authorization: `Bearer ${await tokenOf(request, student)}` },
    },
  );
  expect(mine.status()).toBe(200);
  expect((await mine.json()).items).toHaveLength(PAGE_SIZE);
  await page.context().clearCookies();
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, student.email, student.password);
  await expect(page).toHaveURL(/dashboard/);
  await page.goto(`${APP_URL}/submissions`);
  await expect(
    page.getByText(`Hiển thị 1 - ${PAGE_SIZE} trong số ${ROWS} bài nộp`),
  ).toBeVisible();
  await page.getByRole("button", { name: "Trang 2", exact: true }).click();
  await expect(
    page.getByText(
      `Hiển thị ${PAGE_SIZE + 1} - ${ROWS} trong số ${ROWS} bài nộp`,
    ),
  ).toBeVisible();
});

test("the F1 admin dashboard reads the same submission totals F4 holds in the database", async ({
  request,
}) => {
  const admin = await createUser(request, { tag: "int-admin", role: "ADMIN" });
  const headers = { Authorization: `Bearer ${await tokenOf(request, admin)}` };
  const verdicts = (await (
    await request.get(
      `${APP_URL}/api/v1/identity/admin/dashboard/submissions/verdicts`,
      { headers },
    )
  ).json()) as { slices: { verdict: string; count: number }[] };
  const fromApi = Object.fromEntries(
    verdicts.slices.map((slice) => [slice.verdict, slice.count]),
  );
  // The dashboard window is wider than the age of this database, so every row is inside it.
  const dbRows = sql(
    "select status||':'||count(*) from judge.submissions where status in ('ACCEPTED','WRONG_ANSWER','TIME_LIMIT_EXCEEDED','RUNTIME_ERROR','COMPILE_ERROR') and submitted_at > now() - interval '20 days' group by status",
  )
    .split("\n")
    .filter(Boolean);
  const fromDb = Object.fromEntries(
    dbRows.map((row) => {
      const [status, count] = row.split(":");
      return [status, Number(count)];
    }),
  );
  for (const [status, count] of Object.entries(fromDb)) {
    expect(fromApi[status], status).toBe(count);
  }
});

import { randomUUID } from "node:crypto";
import { expect, test } from "@playwright/test";
import {
  APP_URL,
  createUser,
  requireRealStack,
  sql,
  submitLogin,
  vi,
} from "./support/real-stack";

requireRealStack();
test.setTimeout(120000);

test("admin reads queue, language configuration and cursor-based infra events from the real APIs", async ({
  page,
  request,
}) => {
  const user = await createUser(request, { tag: "f4-admin", role: "ADMIN" });
  await page.goto(`${APP_URL}/admin/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/admin\/overview/);
  const queue = page.waitForResponse((response) =>
    response.url().includes("/judge/admin/queue/snapshot"),
  );
  await page.goto(`${APP_URL}/admin/queue`);
  expect((await queue).status()).toBe(200);
  await expect(
    page.getByRole("heading", {
      name: vi.adminQueueMonitor.title,
      exact: true,
    }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", {
      name: vi.adminQueueMonitor.params.open,
      exact: true,
    }),
  ).toBeVisible();
  const config = page.waitForResponse((response) =>
    response.url().includes("/judge/admin/language-config"),
  );
  await page.goto(`${APP_URL}/admin/language-config`);
  expect((await config).status()).toBe(200);
  await expect(page.getByText("Python", { exact: true })).toBeVisible();
  const events = page.waitForResponse((response) =>
    response.url().includes("/judge/admin/infra-events"),
  );
  await page.goto(`${APP_URL}/admin/infra-events`);
  expect((await events).status()).toBe(200);
  await expect(
    page.getByRole("heading", { name: vi.adminInfraEvents.title, exact: true }),
  ).toBeVisible();
});

test("instructor submission list reads the scoped server page", async ({
  page,
  request,
}) => {
  const user = await createUser(request, {
    tag: "f4-instructor",
    role: "INSTRUCTOR",
  });
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/instructor\/overview/);
  const listing = page.waitForResponse((response) =>
    response.url().includes("/judge/instructor/submissions"),
  );
  await page.goto(`${APP_URL}/instructor/submissions`);
  expect((await listing).status()).toBe(200);
  await expect(
    page.getByRole("heading", {
      name: vi.instructorSubmissions.header.title,
      exact: true,
    }),
  ).toBeVisible();
});

test("student loads a real starter, runs samples, submits and opens its persisted result", async ({
  page,
  request,
}) => {
  test.skip(
    process.env.E2E_F3F4_GRADING !== "true",
    "Requires a backend configured with judge.go-judge.base-url pointing to the toolchain sandbox, not the infrastructure-only engine",
  );
  const teacher = await createUser(request, {
    tag: "f3-author",
    role: "INSTRUCTOR",
  });
  const student = await createUser(request, { tag: "f4-student" });
  const problemId = randomUUID();
  const authorId = sql(
    `select id from identity.users where email='${teacher.email}'`,
  );
  sql(`insert into problem.problems(id,code,title,statement_md,author_id,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
    values('${problemId}','FE-${problemId}','F3 F4 browser echo','Return the integer input.','${authorId}','20000000-0000-4000-8000-000000000001',2000,256,64,true);
    insert into problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,matching_strategy,spec_version,function_name,return_type,parameters)
    values(gen_random_uuid(),'${problemId}','integer','integer','EXACT',1,'echo','{"kind":"INT"}'::jsonb,'[{"name":"value","type":{"kind":"INT"}}]'::jsonb);
    insert into problem.worked_examples(id,problem_id,position,input,output) values(gen_random_uuid(),'${problemId}',1,'1','1'),(gen_random_uuid(),'${problemId}',2,'2','2');
    insert into problem.problem_reference_solutions(id,problem_id,language,source_code) values(gen_random_uuid(),'${problemId}','PYTHON','class Solution:'||chr(10)||'    def echo(self, value: int) -> int:'||chr(10)||'        return value'||chr(10));
    insert into problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline,origin)
    select gen_random_uuid(),'${problemId}',case when i<=2 then 'SAMPLE' else 'HIDDEN' end,i,true,'INLINE',i::text||chr(10),'INLINE',i::text||chr(10),'MANUAL' from generate_series(1,8) i;`);
  const login = await request.post(`${APP_URL}/api/v1/identity/auth/login`, {
    data: { email: teacher.email, password: teacher.password },
  });
  expect(login.ok()).toBe(true);
  const { accessToken } = await login.json();
  const stamp = sql(
    `select to_char(updated_at at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"') from problem.problems where id='${problemId}'`,
  );
  const publish = await request.put(
    `${APP_URL}/api/v1/problem-bank/authoring/problems/${problemId}/status`,
    {
      headers: { Authorization: `Bearer ${accessToken}` },
      data: { status: "PUBLISHED", expectedUpdatedAt: stamp },
    },
  );
  expect(publish.status(), await publish.text()).toBe(200);
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, student.email, student.password);
  await expect(page).toHaveURL(/dashboard/);
  await page.goto(`${APP_URL}/workspace/${problemId}`);
  await expect(
    page.getByRole("heading", { name: "F3 F4 browser echo", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", {
      name: vi.problemDetail.language.python,
      exact: true,
    })
    .click();
  await page
    .getByRole("button", {
      name: vi.problemDetail.inputMethod.upload,
      exact: true,
    })
    .click();
  await page
    .locator('input[type="file"]')
    .setInputFiles({
      name: "echo.py",
      mimeType: "text/plain",
      buffer: Buffer.from(
        "class Solution:\n    def echo(self, value: int) -> int:\n        return value\n",
      ),
    });
  const run = page.waitForResponse((response) =>
    response.url().includes(`/judge/problems/${problemId}/run-sample`),
  );
  await page.getByRole("button", { name: "Run Code", exact: true }).click();
  const sampleResponse = await run;
  expect(sampleResponse.status()).toBe(200);
  expect(await sampleResponse.json()).toMatchObject({
    outcome: "COMPLETED",
    passedCount: 2,
    totalCount: 2,
  });
  await page
    .getByRole("button", {
      name: vi.problemDetail.console.dialog.close,
      exact: true,
    })
    .click();
  const admission = page.waitForResponse(
    (response) =>
      response.url().endsWith(`/judge/problems/${problemId}/submissions`) &&
      response.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Submit", exact: true }).click();
  const ticket = await admission;
  // DD F4 API #3 returns 201 with the ticket; the grading result arrives later over STOMP.
  expect(ticket.status()).toBe(201);
  const { submissionId } = await ticket.json();
  await expect(
    page
      .getByRole("dialog")
      .getByText(/Accepted/)
      .first(),
  ).toBeVisible({ timeout: 60000 });
  await page.goto(`${APP_URL}/submissions/${submissionId}`);
  await expect(
    page.getByText("def echo(self, value: int)", { exact: false }),
  ).toBeVisible();
  expect(
    sql(`select status from judge.submissions where id='${submissionId}'`),
  ).toBe("ACCEPTED");
});

test("student workspace loads the F3 starter and keeps hidden testcase data out of the UI", async ({
  page,
  request,
}) => {
  const teacher = await createUser(request, {
    tag: "f3-read-author",
    role: "INSTRUCTOR",
  });
  const student = await createUser(request, { tag: "f3-reader" });
  const id = randomUUID();
  const authorId = sql(
    `select id from identity.users where email='${teacher.email}'`,
  );
  // Read-only browser fixture: publishing validation and grading are tested separately above.
  sql(`insert into problem.problems(id,code,title,statement_md,author_id,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported,status,published_at)
    values('${id}','FE-${id}','F3 starter browser fixture','Return the integer input.','${authorId}','20000000-0000-4000-8000-000000000001',2000,256,64,true,'PUBLISHED',clock_timestamp());
    insert into problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,matching_strategy,spec_version,function_name,return_type,parameters)
    values(gen_random_uuid(),'${id}','integer','integer','EXACT',1,'echo','{"kind":"INT"}'::jsonb,'[{"name":"value","type":{"kind":"INT"}}]'::jsonb);
    insert into problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline,origin)
    values(gen_random_uuid(),'${id}','SAMPLE',1,true,'INLINE','1','INLINE','1','MANUAL'),(gen_random_uuid(),'${id}','HIDDEN',2,true,'INLINE','PRIVATE-INPUT-SENTINEL','INLINE','PRIVATE-EXPECTED-SENTINEL','MANUAL');`);
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, student.email, student.password);
  await expect(page).toHaveURL(/dashboard/);
  await page.goto(`${APP_URL}/workspace/${id}`);
  await expect(
    page.getByRole("heading", {
      name: "F3 starter browser fixture",
      exact: true,
    }),
  ).toBeVisible();
  const starter = page.waitForResponse(
    (response) =>
      response.url().includes(`/harness/problems/${id}/starter-code`) &&
      response.url().includes("language=PYTHON"),
  );
  await page
    .getByRole("button", {
      name: vi.problemDetail.language.python,
      exact: true,
    })
    .click();
  const response = await starter;
  expect(response.status()).toBe(200);
  expect(await response.json()).toMatchObject({
    language: "PYTHON",
    submissionModel: "FUNCTION_WRAPPER",
  });
  const token = response.request().headers()["authorization"];
  expect(
    (
      await request.get(`${APP_URL}/api/v1/judge/admin/queue/snapshot`, {
        headers: { Authorization: token! },
      })
    ).status(),
  ).toBe(403);
  await expect(page.locator("body")).not.toContainText(
    "PRIVATE-INPUT-SENTINEL",
  );
  await expect(page.locator("body")).not.toContainText(
    "PRIVATE-EXPECTED-SENTINEL",
  );
});

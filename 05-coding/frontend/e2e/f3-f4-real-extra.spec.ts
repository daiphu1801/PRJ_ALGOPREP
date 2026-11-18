import { randomUUID } from "node:crypto";
import {
  expect,
  test,
  type APIRequestContext,
  type Page,
} from "@playwright/test";
import {
  APP_URL,
  createUser,
  requireRealStack,
  sql,
  submitLogin,
  vi,
  type TestUser,
} from "./support/real-stack";

// FE-08 remainder: admin writes, stale 409, cross-account/scope refusals, offline and refresh while
// grading, error state. Run with E2E_PG_DB set to the database the backend uses.
requireRealStack();
test.setTimeout(120000);

const queue = vi.adminQueueMonitor;
const PYTHON_ECHO =
  "class Solution:\n    def echo(self, value: int) -> int:\n        return value\n";

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

/** Clicks a queue switch to the opposite state and waits for the PUT; returns the new state. */
async function flip(
  page: Page,
  key: "paused" | "autoscale",
  endpoint: "consumption" | "autoscale",
): Promise<boolean> {
  const toggle = page.getByRole("switch", {
    name: queue.control[key].label,
    exact: true,
  });
  const next = (await toggle.getAttribute("aria-checked")) !== "true";
  // Pausing and turning autoscale off ask for confirmation first (the view's `risky` rule).
  const risky = key === "paused" ? next : !next;
  const put = page.waitForResponse(
    (r) =>
      r.url().includes(`/judge/admin/queue/${endpoint}`) &&
      r.request().method() === "PUT",
  );
  await toggle.click();
  if (risky) {
    await page
      .getByRole("button", { name: queue.confirm.yes, exact: true })
      .click();
  }
  expect((await put).status()).toBe(200);
  await expect(toggle).toHaveAttribute("aria-checked", String(next));
  return next;
}

async function adminOnQueue(page: Page, request: APIRequestContext) {
  const admin = await createUser(request, { tag: "f4-q-admin", role: "ADMIN" });
  await page.goto(`${APP_URL}/admin/login`);
  await submitLogin(page, admin.email, admin.password);
  await expect(page).toHaveURL(/admin\/overview/);
  await page.goto(`${APP_URL}/admin/queue`);
  await expect(
    page.getByRole("heading", { name: queue.title, exact: true }),
  ).toBeVisible();
  return admin;
}

test("admin pauses and resumes consumption from the queue screen, and the state is restored", async ({
  page,
  request,
}) => {
  const admin = await adminOnQueue(page, request);
  const toggle = page.getByRole("switch", {
    name: queue.control.paused.label,
    exact: true,
  });
  const initial = (await toggle.getAttribute("aria-checked")) === "true";
  try {
    const first = await flip(page, "paused", "consumption");
    expect(first).toBe(!initial);
    if (first) {
      await expect(page.getByText(queue.state.paused).first()).toBeVisible();
    }
    expect(await flip(page, "paused", "consumption")).toBe(initial);
  } finally {
    // A failed run must never leave the shared grading queue paused.
    const token = await tokenOf(request, admin);
    await request.put(`${APP_URL}/api/v1/judge/admin/queue/consumption`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { paused: initial, expectedPaused: !initial },
    });
  }
});

test("admin toggles autoscale and the state is restored", async ({
  page,
  request,
}) => {
  const admin = await adminOnQueue(page, request);
  const toggle = page.getByRole("switch", {
    name: queue.control.autoscale.label,
    exact: true,
  });
  const initial = (await toggle.getAttribute("aria-checked")) === "true";
  try {
    expect(await flip(page, "autoscale", "autoscale")).toBe(!initial);
    expect(await flip(page, "autoscale", "autoscale")).toBe(initial);
  } finally {
    const token = await tokenOf(request, admin);
    await request.put(`${APP_URL}/api/v1/judge/admin/queue/autoscale`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { enabled: initial, expectedEnabled: !initial },
    });
  }
});

test("a control change with a stale expected state is refused with 409 and changes nothing", async ({
  request,
}) => {
  const admin = await createUser(request, { tag: "f4-409", role: "ADMIN" });
  const headers = { Authorization: `Bearer ${await tokenOf(request, admin)}` };
  const read = async () =>
    (await (
      await request.get(`${APP_URL}/api/v1/judge/admin/queue/snapshot`, {
        headers,
      })
    ).json()) as Record<string, unknown>;
  const before = JSON.stringify(
    Object.entries(await read()).filter(([key]) =>
      /paused|autoscale/i.test(key),
    ),
  );
  expect(before).not.toBe("[]");
  // Real state is unknown to the caller: claim the opposite of both values the snapshot shows.
  const snapshot = (await read()) as { consumption?: { paused?: boolean } };
  const actualPaused = Boolean(
    snapshot.consumption?.paused ?? (snapshot as { paused?: boolean }).paused,
  );
  const stale = await request.put(
    `${APP_URL}/api/v1/judge/admin/queue/consumption`,
    {
      headers,
      data: { paused: !actualPaused, expectedPaused: !actualPaused },
    },
  );
  expect(stale.status(), await stale.text()).toBe(409);
  expect(
    JSON.stringify(
      Object.entries(await read()).filter(([key]) =>
        /paused|autoscale/i.test(key),
      ),
    ),
  ).toBe(before);
});

test("another student and an instructor outside the class cannot read a submission or its source", async ({
  page,
  request,
}) => {
  const row = sql(
    "select id from judge.submissions where status='ACCEPTED' order by updated_at desc limit 1",
  );
  test.skip(
    row === "",
    "needs one graded submission; run the grading case first",
  );
  const other = await createUser(request, { tag: "f4-other-student" });
  const outsider = await createUser(request, {
    tag: "f4-outsider",
    role: "INSTRUCTOR",
  });
  const asStudent = await request.get(
    `${APP_URL}/api/v1/judge/submissions/${row}`,
    { headers: { Authorization: `Bearer ${await tokenOf(request, other)}` } },
  );
  expect([403, 404]).toContain(asStudent.status());
  expect(await asStudent.text()).not.toContain("class Solution");
  const asInstructor = await request.get(
    `${APP_URL}/api/v1/judge/instructor/submissions/${row}`,
    {
      headers: { Authorization: `Bearer ${await tokenOf(request, outsider)}` },
    },
  );
  expect([403, 404]).toContain(asInstructor.status());
  expect(await asInstructor.text()).not.toContain("class Solution");
  // The empty state for an instructor without a class.
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, outsider.email, outsider.password);
  await expect(page).toHaveURL(/instructor\/overview/);
  await page.goto(`${APP_URL}/instructor/submissions`);
  await expect(
    page.getByText(vi.instructorSubmissions.emptyNoClass),
  ).toBeVisible();
});

test("the queue screen shows a retryable error when the snapshot API fails", async ({
  page,
  request,
}) => {
  await adminOnQueue(page, request);
  // Browser-level interception: the backend itself is healthy, only this response is replaced.
  await page.route("**/judge/admin/queue/snapshot*", (route) =>
    route.fulfill({
      status: 500,
      contentType: "application/json",
      body: JSON.stringify({ errorCode: "JDG-505" }),
    }),
  );
  await page.reload();
  const retry = page.getByRole("button", { name: "Thử lại", exact: true });
  await expect(retry).toBeVisible();
  await page.unroute("**/judge/admin/queue/snapshot*");
  await retry.click();
  await expect(
    page.getByRole("heading", { name: queue.title, exact: true }),
  ).toBeVisible();
});

/** Publishes a one-function Python problem and returns the student that is logged in on `page`. */
async function submitEcho(page: Page, request: APIRequestContext) {
  const teacher = await createUser(request, {
    tag: "f3-x-author",
    role: "INSTRUCTOR",
  });
  const student = await createUser(request, { tag: "f4-x-student" });
  const problemId = randomUUID();
  const authorId = sql(
    `select id from identity.users where email='${teacher.email}'`,
  );
  sql(`insert into problem.problems(id,code,title,statement_md,author_id,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
    values('${problemId}','FE-${problemId}','F3 F4 resilience echo','Return the integer input.','${authorId}','20000000-0000-4000-8000-000000000001',2000,256,64,true);
    insert into problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,matching_strategy,spec_version,function_name,return_type,parameters)
    values(gen_random_uuid(),'${problemId}','integer','integer','EXACT',1,'echo','{"kind":"INT"}'::jsonb,'[{"name":"value","type":{"kind":"INT"}}]'::jsonb);
    insert into problem.worked_examples(id,problem_id,position,input,output) values(gen_random_uuid(),'${problemId}',1,'1','1'),(gen_random_uuid(),'${problemId}',2,'2','2');
    insert into problem.problem_reference_solutions(id,problem_id,language,source_code) values(gen_random_uuid(),'${problemId}','PYTHON','${PYTHON_ECHO.replaceAll("\n", "'||chr(10)||'")}');
    insert into problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline,origin)
    select gen_random_uuid(),'${problemId}',case when i<=2 then 'SAMPLE' else 'HIDDEN' end,i,true,'INLINE',i::text||chr(10),'INLINE',i::text||chr(10),'MANUAL' from generate_series(1,8) i;`);
  const token = await tokenOf(request, teacher);
  const stamp = sql(
    `select to_char(updated_at at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"') from problem.problems where id='${problemId}'`,
  );
  const publish = await request.put(
    `${APP_URL}/api/v1/problem-bank/authoring/problems/${problemId}/status`,
    {
      headers: { Authorization: `Bearer ${token}` },
      data: { status: "PUBLISHED", expectedUpdatedAt: stamp },
    },
  );
  expect(publish.status(), await publish.text()).toBe(200);
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, student.email, student.password);
  await expect(page).toHaveURL(/dashboard/);
  await page.goto(`${APP_URL}/workspace/${problemId}`);
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
  await page.locator('input[type="file"]').setInputFiles({
    name: "echo.py",
    mimeType: "text/plain",
    buffer: Buffer.from(PYTHON_ECHO),
  });
  const admission = page.waitForResponse(
    (r) =>
      r.url().endsWith(`/judge/problems/${problemId}/submissions`) &&
      r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Submit", exact: true }).click();
  const ticket = await admission;
  expect(ticket.status()).toBe(201);
  return (await ticket.json()).submissionId as string;
}

test("losing the network while grading still ends at the final verdict", async ({
  page,
  request,
}) => {
  const submissionId = await submitEcho(page, request);
  await page.context().setOffline(true);
  await page.waitForTimeout(5000);
  await page.context().setOffline(false);
  await expect(
    page
      .getByRole("dialog")
      .getByText(/Accepted/)
      .first(),
  ).toBeVisible({ timeout: 60000 });
  expect(
    sql(`select status from judge.submissions where id='${submissionId}'`),
  ).toBe("ACCEPTED");
});

test("refreshing the page while grading shows the same final result", async ({
  page,
  request,
}) => {
  const submissionId = await submitEcho(page, request);
  await page.reload();
  await expect
    .poll(
      () =>
        sql(`select status from judge.submissions where id='${submissionId}'`),
      { timeout: 60000 },
    )
    .toBe("ACCEPTED");
  await page.goto(`${APP_URL}/submissions/${submissionId}`);
  // The result page labels the verdict in Vietnamese; the workspace dialog uses the English label.
  await expect(page.getByText(/Đã chấp nhận|Accepted/).first()).toBeVisible();
});

test("the queue screen follows a change made by another admin without a reload (STOMP batch)", async ({
  page,
  request,
}) => {
  const admin = await adminOnQueue(page, request);
  const other = await createUser(request, {
    tag: "f4-live-admin",
    role: "ADMIN",
  });
  const toggle = page.getByRole("switch", {
    name: queue.control.paused.label,
    exact: true,
  });
  const initial = (await toggle.getAttribute("aria-checked")) === "true";
  const otherHeaders = {
    Authorization: `Bearer ${await tokenOf(request, other)}`,
  };
  try {
    const change = await request.put(
      `${APP_URL}/api/v1/judge/admin/queue/consumption`,
      {
        headers: otherHeaders,
        data: { paused: !initial, expectedPaused: initial },
      },
    );
    expect(change.status(), await change.text()).toBe(200);
    // The STOMP batch is every 5 s and the REST fallback only polls every 15 s, so a change that
    // lands inside 10 s without a reload came over the live channel.
    await expect(toggle).toHaveAttribute("aria-checked", String(!initial), {
      timeout: 10000,
    });
  } finally {
    const token = await tokenOf(request, admin);
    await request.put(`${APP_URL}/api/v1/judge/admin/queue/consumption`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { paused: initial, expectedPaused: !initial },
    });
  }
});

test("a 409 on a control change shows an error and leaves the switch unchanged", async ({
  page,
  request,
}) => {
  await adminOnQueue(page, request);
  const toggle = page.getByRole("switch", {
    name: queue.control.autoscale.label,
    exact: true,
  });
  const initial = await toggle.getAttribute("aria-checked");
  // Browser-level interception: the backend is not touched, so shared state cannot change.
  await page.route("**/judge/admin/queue/autoscale", (route) =>
    route.request().method() === "PUT"
      ? route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({ errorCode: "JDG-409", detail: "stale" }),
        })
      : route.continue(),
  );
  const put = page.waitForResponse(
    (r) =>
      r.url().includes("/judge/admin/queue/autoscale") &&
      r.request().method() === "PUT",
  );
  await toggle.click();
  if (initial === "true") {
    await page
      .getByRole("button", { name: queue.confirm.yes, exact: true })
      .click();
  }
  expect((await put).status()).toBe(409);
  await expect(page.getByText(queue.control.autoscale.doneOff)).toHaveCount(0);
  await expect(page.getByText(queue.control.autoscale.doneOn)).toHaveCount(0);
  await expect(toggle).toHaveAttribute("aria-checked", initial ?? "false");
});

test("an expired session on the queue screen sends the admin back to the login page", async ({
  page,
  request,
}) => {
  await adminOnQueue(page, request);
  // Browser-level interception: every admin read and the token refresh answer 401.
  await page.route("**/judge/admin/queue/**", (route) =>
    route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ errorCode: "IDT-401" }),
    }),
  );
  await page.route("**/identity/auth/refresh", (route) =>
    route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ errorCode: "IDT-401" }),
    }),
  );
  await page.reload();
  await expect(page).toHaveURL(/login/, { timeout: 15000 });
});

test("saving language config after another admin changed it is refused with 409 and keeps the draft", async ({
  page,
  request,
}) => {
  const admin = await createUser(request, { tag: "f4-lc-a", role: "ADMIN" });
  const other = await createUser(request, { tag: "f4-lc-b", role: "ADMIN" });
  const otherHeaders = {
    Authorization: `Bearer ${await tokenOf(request, other)}`,
  };
  const url = `${APP_URL}/api/v1/judge/admin/language-config`;
  type Wire = {
    languages: { language: string; timeLimitMultiplier: number }[];
    gradingDefaults: object;
    sandbox: object;
    generatorSandbox: object;
  };
  const read = async () =>
    (await (await request.get(url, { headers: otherHeaders })).json()) as Wire;
  const put = (wire: Wire) =>
    request.put(url, {
      headers: otherHeaders,
      data: {
        languages: wire.languages,
        gradingDefaults: wire.gradingDefaults,
        sandbox: wire.sandbox,
        generatorSandbox: wire.generatorSandbox,
      },
    });
  await page.goto(`${APP_URL}/admin/login`);
  await submitLogin(page, admin.email, admin.password);
  await expect(page).toHaveURL(/admin\/overview/);
  await page.goto(`${APP_URL}/admin/language-config`);
  const field = page.getByLabel("Hệ số thời gian cho Python", { exact: true });
  await expect(field).toBeVisible();
  const original = await read();
  const python = original.languages.find((l) => l.language === "PYTHON")!;
  try {
    // Another admin saves first, so the version the open page holds is now stale.
    const changed = structuredClone(original);
    changed.languages.find(
      (l) => l.language === "PYTHON",
    )!.timeLimitMultiplier =
      Math.round((python.timeLimitMultiplier + 0.01) * 100) / 100;
    const first = await put(changed);
    expect(first.status(), await first.text()).toBe(200);
    // This admin edits a different value and saves with the old version.
    const draft = String(python.timeLimitMultiplier + 0.02);
    await field.fill(draft);
    const save = page.waitForResponse(
      (r) =>
        r.url().includes("/judge/admin/language-config") &&
        r.request().method() === "PUT",
    );
    await page
      .getByRole("button", { name: vi.adminLanguageConfig.save, exact: true })
      .click();
    expect((await save).status()).toBe(409);
    await expect(page.getByText(vi.adminLanguageConfig.saveDone)).toHaveCount(
      0,
    );
    // The draft is kept, not replaced by the newer server version.
    await expect(field).toHaveValue(draft);
  } finally {
    const current = await read();
    const restore = structuredClone(original);
    restore.languages.forEach((l) => {
      const now = current.languages.find(
        (c) => c.language === l.language,
      ) as unknown as { updatedAt: string };
      (l as unknown as { updatedAt: string }).updatedAt = now.updatedAt;
    });
    (restore.gradingDefaults as { updatedAt: string }).updatedAt = (
      current.gradingDefaults as { updatedAt: string }
    ).updatedAt;
    (restore.sandbox as { updatedAt: string }).updatedAt = (
      current.sandbox as { updatedAt: string }
    ).updatedAt;
    (restore.generatorSandbox as { updatedAt: string }).updatedAt = (
      current.generatorSandbox as { updatedAt: string }
    ).updatedAt;
    await put(restore);
  }
});

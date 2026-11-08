import { expect, test, type APIRequestContext } from "@playwright/test";
import {
  APP_URL,
  createUser,
  requireRealStack,
  sql,
  submitLogin,
  vi,
  type TestUser,
} from "./support/real-stack";

// FE-08: server paging of the admin job table and cursor paging of infra events, plus a session
// that is really revoked on the server. Seeds failed submissions by SQL and removes them after.
// Run with E2E_PG_DB set to the backend database.
requireRealStack();
test.setTimeout(120000);

const SEEDED = 25;
const PAGE = 20;

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

test("the job table and the infra event list page through the server data", async ({
  page,
  request,
}) => {
  const problemId = sql(
    "select id from problem.problems where status='PUBLISHED' order by published_at desc limit 1",
  );
  test.skip(problemId === "", "needs one published problem in the database");
  const admin = await createUser(request, {
    tag: "f4-page-admin",
    role: "ADMIN",
  });
  const learner = await createUser(request, { tag: "f4-page-learner" });
  const learnerId = sql(
    `select id from identity.users where email='${learner.email}'`,
  );
  const headers = { Authorization: `Bearer ${await tokenOf(request, admin)}` };
  const jobs = async (pageNo: number) =>
    (await (
      await request.get(
        `${APP_URL}/api/v1/judge/admin/queue/jobs?tab=failed&page=${pageNo}`,
        { headers },
      )
    ).json()) as { items: unknown[]; totalCount: number; pageSize: number };
  const events = async (cursor?: string) =>
    (await (
      await request.get(
        `${APP_URL}/api/v1/judge/admin/infra-events?q=${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ""}`,
        { headers },
      )
    ).json()) as { events: unknown[]; nextCursor: string | null };
  const failedBefore = (await jobs(1)).totalCount;
  try {
    sql(`insert into judge.submissions(id,user_id,problem_id,spec_version,language,submission_model,source_code,source_input_method,status,failure_reason,testcase_set_version_at_grading,submitted_at,finished_at,updated_at)
      select gen_random_uuid(),'${learnerId}','${problemId}',1,'PYTHON','FUNCTION_WRAPPER','x','EDITOR','SYSTEM_ERROR','SANDBOX_FAULT',1,
        now() - (i || ' minutes')::interval, now() - (i || ' minutes')::interval, now() - (i || ' minutes')::interval
      from generate_series(1,${SEEDED}) i;`);

    // Job table: the failed tab pages by the configured size.
    const first = await jobs(1);
    expect(first.totalCount).toBe(failedBefore + SEEDED);
    expect(first.pageSize).toBe(PAGE);
    expect(first.items).toHaveLength(PAGE);
    const last = await jobs(Math.ceil(first.totalCount / PAGE));
    expect(last.items.length).toBe(
      first.totalCount - PAGE * (Math.ceil(first.totalCount / PAGE) - 1),
    );

    await page.goto(`${APP_URL}/admin/login`);
    await submitLogin(page, admin.email, admin.password);
    await expect(page).toHaveURL(/admin\/overview/);
    await page.goto(`${APP_URL}/admin/queue`);
    await page.getByRole("button", { name: /^Lỗi/ }).click();
    await expect(
      page.getByText(`1-${PAGE} trên ${first.totalCount}`),
    ).toBeVisible();
    await page
      .getByRole("button", { name: vi.adminQueueMonitor.pageNext })
      .click();
    await expect(
      page.getByText(
        `${PAGE + 1}-${Math.min(2 * PAGE, first.totalCount)} trên ${first.totalCount}`,
      ),
    ).toBeVisible();

    // Infra events: 20 rows and a cursor, then the cursor returns more rows.
    const eventsFirst = await events();
    expect(eventsFirst.events).toHaveLength(PAGE);
    expect(eventsFirst.nextCursor).not.toBeNull();
    const eventsSecond = await events(eventsFirst.nextCursor!);
    expect(eventsSecond.events.length).toBeGreaterThan(0);
    await page.goto(`${APP_URL}/admin/infra-events`);
    const more = page.getByRole("button", {
      name: vi.adminInfraEvents.loadMore.replace("{count}", String(PAGE)),
    });
    await expect(more).toBeVisible();
    const shown = () =>
      page
        .getByRole("list", { name: vi.adminInfraEvents.listLabel })
        .getByRole("listitem")
        .count();
    expect(await shown()).toBe(PAGE);
    await more.click();
    await expect.poll(shown).toBeGreaterThan(PAGE);
  } finally {
    sql(`delete from judge.submissions where user_id='${learnerId}'`);
  }
});

test("a session revoked on the server sends the admin back to the login page", async ({
  page,
  request,
}) => {
  const admin = await createUser(request, { tag: "f4-revoked", role: "ADMIN" });
  await page.goto(`${APP_URL}/admin/login`);
  await submitLogin(page, admin.email, admin.password);
  await expect(page).toHaveURL(/admin\/overview/);
  await page.goto(`${APP_URL}/admin/queue`);
  await expect(
    page.getByRole("heading", {
      name: vi.adminQueueMonitor.title,
      exact: true,
    }),
  ).toBeVisible();
  // The account is deactivated and its refresh tokens are removed: both the live status check on
  // the access token and the refresh must now fail, with no browser-side interception.
  sql(`update identity.users set status='DEACTIVATED' where email='${admin.email}';
    delete from identity.refresh_tokens where user_id=(select id from identity.users where email='${admin.email}')`);
  await page.reload();
  await expect(page).toHaveURL(/login/, { timeout: 20000 });
});

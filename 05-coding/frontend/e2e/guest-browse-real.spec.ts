// Guest browsing (DEC-2026-1126-guest-browse-published-problems) against the REAL backend: what a
// visitor without a session may read, what asks them to sign in, and that signing in brings them back.
// Needs the stack in e2e/README.md and at least one published problem; skips itself otherwise.
import { expect, test, type Page } from "@playwright/test";
import {
  APP_URL,
  createUser,
  readOtp,
  requireRealStack,
  sql,
  submitLogin,
  uniqueEmail,
  PASSWORD,
  vi,
} from "./support/real-stack";

requireRealStack();

const nav = vi.nav;
const prompt = vi.loginPrompt;
const list = vi.problemList;
const overview = vi.problemOverview;
const detail = vi.problemDetail;

/** One published problem of the database behind the stack, as `[id, title]`. */
function publishedProblem(): { id: string; title: string } {
  const row = sql(
    // Other specs insert bare rows by SQL; only a problem with a spec and testcases is a real one.
    "select id || '|' || title from problem.problems p where status='PUBLISHED' and not deleted" +
      " and exists (select 1 from problem.problem_specs s where s.problem_id = p.id)" +
      " and exists (select 1 from problem.testcases t where t.problem_id = p.id)" +
      " and (select count(*) from problem.problems o where o.title = p.title and o.status='PUBLISHED') = 1" +
      " order by code limit 1",
  );
  test.skip(!row, "no published problem in the database");
  const [id, title] = row.split("|") as [string, string];
  return { id, title };
}

/** API answers >= 400. The session restore probe of a visitor without a cookie is expected. */
function watchApiFailures(page: Page): string[] {
  const failures: string[] = [];
  page.on("response", (response) => {
    const url = new URL(response.url());
    if (
      url.pathname.startsWith("/api/") &&
      response.status() >= 400 &&
      !url.pathname.endsWith("/identity/auth/refresh")
    )
      failures.push(`${response.status()} ${url.pathname}`);
  });
  return failures;
}

/** The list searched by title: on a shared database the problem need not be on the first page. */
const listWith = (title: string) =>
  `${APP_URL}/problems?q=${encodeURIComponent(title)}`;

const loginHref = (path: string) =>
  `/login?redirect=${encodeURIComponent(path)}`;

test("a guest lands on the problem list, sees no personal part and no API call fails", async ({
  page,
}) => {
  const problem = publishedProblem();
  const failures = watchApiFailures(page);

  await page.goto(`${APP_URL}/`);
  await expect(page).toHaveURL(/\/problems$/);

  const header = page.getByRole("banner");
  await expect(header.getByRole("link", { name: nav.login })).toHaveAttribute(
    "href",
    loginHref("/problems"),
  );
  await expect(
    header.getByRole("button", { name: nav.accountMenu }),
  ).toHaveCount(0);
  await expect(page.getByText(list.inProgress.title)).toHaveCount(0);
  await expect(page.getByText(list.assignment.title)).toHaveCount(0);
  await expect(
    page.getByRole("group", { name: list.topicNav.label }),
  ).toHaveCount(0);
  await expect(page.getByText(list.table.colState)).toHaveCount(0);
  await expect(page.getByText(list.degraded.solveState)).toHaveCount(0);

  await page.goto(listWith(problem.title));
  await expect(
    page.getByRole("link", { name: problem.title, exact: true }),
  ).toBeVisible();

  await page.getByRole("button", { name: list.summary.btnRandom }).click();
  await expect(page).toHaveURL(/\/problems\/[0-9a-f-]{36}$/);
  await page.waitForLoadState("networkidle");
  expect(failures).toEqual([]);
});

test("Solve and Save ask a guest to sign in, and signing in returns to the same problem", async ({
  page,
  request,
}) => {
  const problem = publishedProblem();
  const student = await createUser(request, { tag: "guest-return" });
  const failures = watchApiFailures(page);
  const path = `/problems/${problem.id}`;

  await page.goto(listWith(problem.title));
  await page.getByRole("link", { name: problem.title, exact: true }).click();
  await expect(
    page.getByRole("heading", { level: 1, name: problem.title }),
  ).toBeVisible();
  await expect(page.getByText(overview.recentTitle)).toHaveCount(0);
  await expect(page.getByText(overview.colTime)).toHaveCount(0);

  const dialog = page.getByRole("dialog", { name: prompt.title });
  await page.getByRole("button", { name: overview.btnSolve }).click();
  await expect(dialog).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(dialog).toHaveCount(0);
  await page.getByRole("button", { name: detail.panel.btnSave }).click();
  await expect(
    dialog.getByRole("link", { name: prompt.login }),
  ).toHaveAttribute("href", loginHref(path));
  expect(failures).toEqual([]);

  await dialog.getByRole("link", { name: prompt.login }).click();
  await expect(page).toHaveURL(/\/login\?redirect=/);
  await submitLogin(page, student.email, student.password);
  await expect(page).toHaveURL(`${APP_URL}${path}`, { timeout: 20_000 });
  await expect(page.getByText(overview.recentTitle)).toBeVisible();
  await expect(page.getByText(overview.colTime)).toBeVisible();
  await expect(
    page.getByRole("link", { name: overview.btnSolve }),
  ).toHaveAttribute("href", `/workspace/${problem.id}`);
});

test("signing up from the dialog also returns to the same problem", async ({
  page,
  request,
}) => {
  const problem = publishedProblem();
  const path = `/problems/${problem.id}`;
  const email = uniqueEmail("guest-signup");

  await page.goto(`${APP_URL}${path}`);
  await page.getByRole("button", { name: overview.btnSolve }).click();
  await page
    .getByRole("dialog", { name: prompt.title })
    .getByRole("link", { name: prompt.register })
    .click();
  await expect(page).toHaveURL(/\/register\?redirect=/);

  // The account is created through the API and verified with the mailed code, then signed in on the
  // very page the dialog opened (its address still carries ?redirect=).
  const registered = await request.post(
    `${APP_URL}/api/v1/identity/auth/register`,
    { data: { email, password: PASSWORD, displayName: "E2E guest signup" } },
  );
  expect(registered.status()).toBe(201);
  const otp = await readOtp(request, email);
  const verified = await request.post(
    `${APP_URL}/api/v1/identity/auth/email/verify`,
    { data: { email, otp } },
  );
  expect(verified.ok()).toBe(true);
  await page
    .getByRole("button", { name: vi.auth.switchToLogin })
    .first()
    .click();
  await submitLogin(page, email, PASSWORD);
  await expect(page).toHaveURL(`${APP_URL}${path}`, { timeout: 20_000 });
});

test("a personal menu item asks a guest to sign in and returns to that page", async ({
  page,
  request,
}) => {
  const student = await createUser(request, { tag: "guest-menu" });
  await page.goto(`${APP_URL}/problems`);
  const bar = page.getByRole("navigation", { name: nav.area.student });
  await expect(bar.getByRole("link")).toHaveCount(1);
  await expect(bar.getByRole("button")).toHaveCount(6);

  await bar.getByRole("button", { name: nav.submissions }).click();
  const dialog = page.getByRole("dialog", { name: prompt.title });
  await expect(page).toHaveURL(/\/problems$/);
  await dialog.getByRole("link", { name: prompt.login }).click();
  await submitLogin(page, student.email, student.password);
  await expect(page).toHaveURL(/\/submissions$/, { timeout: 20_000 });
});

test("direct links to personal, instructor and admin pages still require a session", async ({
  page,
}) => {
  const problem = publishedProblem();
  const cases: [string, RegExp][] = [
    [`/workspace/${problem.id}`, /\/login\?redirect=%2Fworkspace%2F/],
    ["/dashboard", /\/login\?redirect=%2Fdashboard/],
    ["/saved", /\/login\?redirect=%2Fsaved/],
    ["/classes", /\/login\?redirect=%2Fclasses/],
    ["/instructor/overview", /\/instructor\/login\?redirect=/],
    ["/instructor/problems", /\/instructor\/login\?redirect=/],
    ["/admin/overview", /\/admin\/login\?redirect=/],
    ["/admin/problems", /\/admin\/login\?redirect=/],
  ];
  for (const [path, expected] of cases) {
    await page.goto(`${APP_URL}${path}`);
    await expect(page, path).toHaveURL(expected);
  }
});

test("a draft problem looks missing to a guest", async ({ page }) => {
  const draft = sql(
    "select id from problem.problems where status='UNPUBLISHED' and not deleted limit 1",
  );
  test.skip(!draft, "no draft problem in the database");
  await page.goto(`${APP_URL}/problems/${draft}`);
  await expect(page.getByText(detail.empty.title)).toBeVisible();
  await expect(page).toHaveURL(new RegExp(`/problems/${draft}$`));
});

test("the API opens only the guest reads, also with a stale token", async ({
  request,
}) => {
  const problem = publishedProblem();
  const api = `${APP_URL}/api/v1/problem-bank`;
  const stale = { Authorization: "Bearer not-a-real-token" };
  for (const path of [
    "/problems",
    "/problems/random",
    `/problems/${problem.id}/overview`,
    `/problems/${problem.id}/sample-testcases`,
    "/levels",
  ]) {
    expect((await request.get(`${api}${path}`)).status(), path).toBe(200);
    expect(
      (await request.get(`${api}${path}`, { headers: stale })).status(),
      `${path} stale`,
    ).toBe(200);
  }
  for (const path of [
    `/problems/${problem.id}/workspace`,
    "/topics",
    "/topics/progress",
    "/me/bookmarks",
    "/suggested-problems",
  ]) {
    expect((await request.get(`${api}${path}`)).status(), path).toBe(401);
  }
  expect(
    (
      await request.get(
        `${APP_URL}/api/v1/judge/problems/${problem.id}/execution-limits`,
      )
    ).status(),
  ).toBe(401);
  expect(
    (
      await request.put(`${api}/me/bookmarks/${problem.id}/state`, {
        data: { bookmarked: true },
      })
    ).status(),
  ).toBe(401);
  const list = (await (await request.get(`${api}/problems`)).json()) as {
    solveStateAvailable: boolean;
    items: { solveState: string | null }[];
  };
  expect(list.solveStateAvailable).toBe(false);
  expect(list.items.every((item) => item.solveState === null)).toBe(true);
});

test("on a narrow screen the guest header fits and keeps the login button on screen", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`${APP_URL}/problems`);
  const login = page.getByRole("banner").getByRole("link", { name: nav.login });
  await expect(login).toBeInViewport({ ratio: 1 });
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - window.innerWidth,
  );
  expect(overflow).toBe(0);
});

test("a session that ends on a public page turns into the guest view without leaving it", async ({
  page,
  request,
}) => {
  const problem = publishedProblem();
  const student = await createUser(request, { tag: "guest-expired" });
  const path = `/problems/${problem.id}`;
  await page.goto(`${APP_URL}/login?redirect=${encodeURIComponent(path)}`);
  await submitLogin(page, student.email, student.password);
  await expect(page.getByText(overview.recentTitle)).toBeVisible({
    timeout: 20_000,
  });

  // End the session on the server: the access token is refused and so is the refresh.
  sql(
    `insert into identity.session_revocations(family_id,revoked_at)
       select distinct t.family_id, now() from identity.refresh_tokens t
       join identity.users u on u.id=t.user_id where u.email='${student.email}';
     delete from identity.refresh_tokens where user_id=(select id from identity.users where email='${student.email}')`,
  );
  await page.getByRole("button", { name: detail.panel.btnSave }).click();

  await expect(
    page.getByRole("banner").getByRole("link", { name: nav.login }),
  ).toBeVisible();
  await expect(page).toHaveURL(`${APP_URL}${path}`);
  await expect(page.getByText(overview.recentTitle)).toHaveCount(0);
});

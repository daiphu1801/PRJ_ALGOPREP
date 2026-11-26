// "Sao chép sang lớp khác" of INS0202 against the REAL backend (F2-12, DEC-2026-1125-copy-class-assignments, F2 #49
// and #52). Needs the stack in e2e/README.md; skips itself otherwise (and in mock mode). The published problems are
// row copies made by SQL in the Postgres container (publishing is not under test), so run it on a demo database.
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
} from "./support/real-stack";

requireRealStack();

const m = vi.classAssignments;
const API = `${APP_URL}/api/v1`;

const fill = (text: string, values: Record<string, string | number>) =>
  text.replace(/\{(\w+)\}/g, (_, key: string) => String(values[key]));

type Instructor = { token: string; email: string; password: string };

async function instructor(request: APIRequestContext): Promise<Instructor> {
  const user = await createUser(request, {
    tag: "copy-inst",
    role: "INSTRUCTOR",
  });
  const login = await request.post(`${API}/identity/auth/login`, {
    data: { email: user.email, password: user.password },
  });
  expect(login.ok(), "login").toBe(true);
  return { ...user, token: (await login.json()).accessToken };
}

async function newClass(
  request: APIRequestContext,
  by: Instructor,
  name: string,
): Promise<{ id: string; name: string }> {
  const res = await request.post(`${API}/identity/classes`, {
    headers: { Authorization: `Bearer ${by.token}` },
    data: { name, description: "e2e", schedule: null },
  });
  expect(res.status(), "create class").toBe(201);
  return { id: (await res.json()).classId, name };
}

async function assign(
  request: APIRequestContext,
  by: Instructor,
  classId: string,
  problemIds: string[],
) {
  const res = await request.post(
    `${API}/problem-bank/instructor/classes/${classId}/assignments`,
    { headers: { Authorization: `Bearer ${by.token}` }, data: { problemIds } },
  );
  expect(res.ok(), "assign").toBe(true);
}

/** `count` published problems of this run, in code order. */
function seedProblems(run: string, count: number): string[] {
  const columns = sql(
    "select string_agg(column_name, ',') from information_schema.columns where table_schema='problem' and table_name='problems' and column_name not in ('id','code','title')",
  );
  const base = sql(
    "select id from problem.problems where status='PUBLISHED' and not deleted limit 1",
  );
  test.skip(base === "", "needs one published problem in the database");
  sql(
    `insert into problem.problems(id,code,title,${columns}) select gen_random_uuid(), '${run}-'||lpad(n::text,3,'0'), '${run} '||n, ${columns} from problem.problems, generate_series(1,${count}) n where id='${base}'`,
  );
  return sql(
    `select id from problem.problems where code like '${run}-%' order by code`,
  ).split("\n");
}

/** Problem codes of a class, newest assignment first, as the list shows them. */
const codesOf = (classId: string) =>
  sql(
    `select p.code from problem.class_assignments a join problem.problems p on p.id=a.problem_id where a.class_id='${classId}' and a.removed_at is null order by a.assigned_at desc`,
  )
    .split("\n")
    .filter(Boolean);

async function signIn(page: Page, user: Instructor) {
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/\/instructor\/overview/, { timeout: 30_000 });
}

async function openCopy(page: Page, sourceId: string, targetName: string) {
  await page.goto(`${APP_URL}/instructor/assignments?classId=${sourceId}`);
  await page.getByRole("button", { name: m.copy.button }).click();
  const dialog = page.getByRole("dialog");
  await dialog
    .getByRole("button", { name: new RegExp(`^${m.copy.target}`) })
    .click();
  await dialog.getByRole("option", { name: targetName }).click();
  return dialog;
}

const confirmName = (count: number) => fill(m.copy.confirm, { count });

// Problems of a run stay withdrawn and flagged deleted, so they never reach a learner's list afterwards.
let runs: string[] = [];
const newRun = () => {
  const run = `E2E-COPY-${randomUUID().slice(0, 8)}`;
  runs.push(run);
  return run;
};
test.afterEach(() => {
  for (const run of runs)
    sql(
      `update problem.problems set status='UNPUBLISHED', deleted=true where code like '${run}-%'`,
    );
  runs = [];
});

test("copies a class in one confirmation: same order, no withdrawn problem, no duplicate, source unchanged", async ({
  page,
  request,
}) => {
  test.setTimeout(120_000);
  const run = newRun();
  const ids = seedProblems(run, 5);
  const by = await instructor(request);
  const source = await newClass(request, by, `${run} nguon`);
  const empty = await newClass(request, by, `${run} trong`);
  const partial = await newClass(request, by, `${run} co san`);
  await assign(request, by, source.id, ids);
  await assign(request, by, partial.id, [ids[0]!, ids[3]!]);
  sql(`update problem.problems set status='UNPUBLISHED' where id='${ids[2]}'`);
  const before = codesOf(source.id);
  const expected = before.filter((code) => code !== `${run}-003`);
  await signIn(page, by);

  // No button until one class is chosen.
  await page.goto(`${APP_URL}/instructor/assignments`);
  await expect(page.getByText(`${run} 5`)).toBeVisible();
  await expect(page.getByRole("button", { name: m.copy.button })).toHaveCount(
    0,
  );

  // Into an empty class.
  const dialog = await openCopy(page, source.id, empty.name);
  const figure = (label: string) =>
    dialog.locator("dl > div").filter({ hasText: label }).locator("dd");
  await expect(
    figure(fill(m.copy.sourceCount, { className: source.name })),
  ).toHaveText("5");
  await expect(
    figure(fill(m.copy.alreadyHas, { className: empty.name })),
  ).toHaveText("0");
  await expect(figure(m.copy.withdrawn)).toHaveText("1");
  await expect(figure(m.copy.willAssign)).toHaveText("4");
  await dialog.getByRole("button", { name: confirmName(4) }).click();
  await expect(dialog).toHaveCount(0);
  await expect(page).toHaveURL(new RegExp(`classId=${empty.id}$`));
  await expect(
    page.getByText(fill(m.copy.done, { count: 4, className: empty.name })),
  ).toBeVisible();
  expect(codesOf(empty.id)).toEqual(expected);

  // Again: nothing left.
  await openCopy(page, source.id, empty.name);
  await expect(
    dialog.getByText(fill(m.copy.nothingLeft, { className: empty.name })),
  ).toBeVisible();
  await expect(
    dialog.getByRole("button", { name: m.copy.confirmEmpty, exact: true }),
  ).toBeDisabled();

  // Into a class that has two of them.
  await openCopy(page, source.id, partial.name);
  await dialog.getByRole("button", { name: confirmName(2) }).click();
  await expect(dialog).toHaveCount(0);
  expect(codesOf(partial.id).sort()).toEqual([...expected].sort());
  expect(codesOf(source.id)).toEqual(before);
});

test("sends 51 problems in two calls, and after a lost call a second press sends only the rest", async ({
  page,
  request,
}) => {
  test.setTimeout(120_000);
  const run = newRun();
  const ids = seedProblems(run, 51);
  const by = await instructor(request);
  const source = await newClass(request, by, `${run} nguon`);
  const target = await newClass(request, by, `${run} nhan`);
  await assign(request, by, source.id, ids.slice(0, 50));
  await assign(request, by, source.id, ids.slice(50));
  await signIn(page, by);

  const sizes: number[] = [];
  let calls = 0;
  await page.route("**/classes/*/assignments", async (route) => {
    if (route.request().method() !== "POST") return route.continue();
    sizes.push(route.request().postDataJSON().problemIds.length);
    calls += 1;
    return calls === 2 ? route.abort() : route.continue();
  });

  const dialog = await openCopy(page, source.id, target.name);
  await dialog.getByRole("button", { name: confirmName(51) }).click();
  await expect(
    page.getByText(fill(m.copy.partial, { done: 50, total: 51 })),
  ).toBeVisible({ timeout: 30_000 });
  await expect(dialog).toBeVisible();

  await dialog.getByRole("button", { name: confirmName(1) }).click();
  await expect(dialog).toHaveCount(0);
  expect(sizes).toEqual([50, 1, 1]);
  expect(codesOf(target.id)).toEqual(codesOf(source.id));
});

test("a problem withdrawn while the dialog is open is refused by name, the others still go", async ({
  page,
  request,
}) => {
  test.setTimeout(120_000);
  const run = newRun();
  const ids = seedProblems(run, 3);
  const by = await instructor(request);
  const source = await newClass(request, by, `${run} nguon`);
  const target = await newClass(request, by, `${run} nhan`);
  await assign(request, by, source.id, ids);
  await signIn(page, by);

  const dialog = await openCopy(page, source.id, target.name);
  const confirm = dialog.getByRole("button", { name: confirmName(3) });
  await expect(confirm).toBeEnabled();
  sql(`update problem.problems set status='UNPUBLISHED' where id='${ids[1]}'`);
  await confirm.click();
  await expect(
    page.getByText(fill(m.copy.refused, { count: 1, titles: `${run} 2` })),
  ).toBeVisible();
  await expect(
    page.getByText(fill(m.copy.done, { count: 2, className: target.name })),
  ).toBeVisible();
  expect(codesOf(target.id)).toEqual([`${run}-003`, `${run}-001`]);
});

test("the button is blocked with its reason for an instructor with a single class, in English too", async ({
  page,
  request,
}) => {
  const by = await instructor(request);
  const only = await newClass(request, by, `E2E mot lop ${Date.now()}`);
  await signIn(page, by);
  await page.goto(`${APP_URL}/instructor/assignments?classId=${only.id}`);
  const button = page.getByRole("button", { name: m.copy.button });
  await expect(button).toHaveAttribute("aria-disabled", "true");
  await expect(button).toHaveAttribute("title", m.copy.noOtherClass);
  await button.click({ force: true });
  await expect(page.getByRole("dialog")).toHaveCount(0);

  await page
    .context()
    .addCookies([{ name: "NEXT_LOCALE", value: "en", url: APP_URL }]);
  await page.reload();
  await expect(
    page.getByRole("button", { name: "Copy to another class" }),
  ).toHaveAttribute("title", "You have no other class to copy to");
});

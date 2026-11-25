// "Thêm nhiều bài" (SHR0204, F2-19, DEC-2026-1125-bulk-problem-import) against the REAL backend: the shipped
// template goes in, drafts are created, the reference solution runs in the sandbox, the passing ones are published
// and given to a class, and a learner gets Accepted. Needs the stack in e2e/README.md including go-judge; skips
// itself otherwise (and in mock mode). Problems it creates are flagged deleted afterwards.
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

const m = vi.problemImport;
const API = `${APP_URL}/api/v1`;
const TEMPLATE = "public/templates/mau-nhap-bai-toan.xlsx";

const fill = (text: string, values: Record<string, string | number>) =>
  text.replace(/\{(\w+)\}/g, (_, key: string) => String(values[key]));

type Person = { id: string; token: string; email: string; password: string };

async function person(
  request: APIRequestContext,
  role: "INSTRUCTOR" | "STUDENT",
): Promise<Person> {
  const user = await createUser(request, {
    tag: `import-${role.toLowerCase()}`,
    role,
  });
  const login = await request.post(`${API}/identity/auth/login`, {
    data: { email: user.email, password: user.password },
  });
  expect(login.ok(), "login").toBe(true);
  return {
    ...user,
    token: (await login.json()).accessToken,
    id: sql(`select id from identity.users where email='${user.email}'`),
  };
}

let authors: string[] = [];
test.afterEach(() => {
  for (const id of authors)
    sql(
      `update problem.problems set status='UNPUBLISHED', deleted=true where author_id='${id}'`,
    );
  authors = [];
});

const rows = (page: Page) => page.getByRole("table").locator("tbody tr");
const own = (by: Person, more = "") =>
  Number(
    sql(
      `select count(*) from problem.problems where author_id='${by.id}' and not deleted ${more}`,
    ),
  );

/** Until nothing is saving, running, waiting or publishing. */
async function settled(page: Page) {
  await expect(page.getByRole("button", { name: m.stop })).toHaveCount(0, {
    timeout: 5 * 60_000,
  });
  await expect(page.getByRole("table")).not.toContainText(
    new RegExp(
      [m.status.saving, m.status.running, m.status.waiting, m.status.publishing]
        .map((text) => text.replace(/\.+$|\s*\(.*$/, ""))
        .join("|"),
    ),
  );
}

test("template in, drafts and runs, fix one, publish to a class, learner gets Accepted", async ({
  page,
  request,
}) => {
  test.setTimeout(8 * 60_000);
  const by = await person(request, "INSTRUCTOR");
  const learner = await person(request, "STUDENT");
  authors.push(by.id);
  const made = await request.post(`${API}/identity/classes`, {
    headers: { Authorization: `Bearer ${by.token}` },
    data: { name: `Import ${Date.now()}`, description: "e2e", schedule: null },
  });
  expect(made.status(), "create class").toBe(201);
  const klass = await made.json();
  const joined = await request.post(`${API}/identity/classes/join`, {
    headers: { Authorization: `Bearer ${learner.token}` },
    data: {
      code: sql(
        `select code from identity.class_invite_codes where class_id='${klass.classId}' limit 1`,
      ),
    },
  });
  expect(joined.ok(), "join class").toBe(true);

  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, by.email, by.password);
  await expect(page).toHaveURL(/\/instructor\/overview/, { timeout: 30_000 });
  await page.goto(`${APP_URL}/instructor/problems`);
  await page
    .getByRole("link", { name: vi.problemManagement.importProblems })
    .click();
  await expect(page).toHaveURL(/\/instructor\/problems\/import$/);

  // The template: three complete problems (one with a wrong solution) and two that need fixing.
  await page.getByTestId("problem-import-file").setInputFiles(TEMPLATE);
  await expect(rows(page)).toHaveCount(5);
  await expect(rows(page).filter({ hasText: m.status.blocked })).toHaveCount(2);

  await page.getByRole("button", { name: fill(m.run, { count: 3 }) }).click();
  await settled(page);
  expect(own(by), "one problem per complete row").toBe(3);
  expect(own(by, "and status='PUBLISHED'"), "nothing published yet").toBe(0);
  const wrong = rows(page).filter({
    hasText: fill(m.status.fail, { position: 1 }),
  });
  await expect(wrong).toHaveCount(1);
  await expect(
    rows(page).filter({ hasText: fill(m.status.pass, { total: 8 }) }),
  ).toHaveCount(2);

  // Fix the wrong one with the solution of a passing row and run again: updated, not created twice.
  const solution = page.getByLabel(m.field.solution);
  await rows(page).first().locator("td").nth(1).getByRole("button").click();
  await page
    .getByRole("button", { name: m.editor.solution, exact: true })
    .click();
  const good = await solution.inputValue();
  await wrong.locator("td").nth(1).getByRole("button").click();
  await page
    .getByRole("button", { name: m.editor.solution, exact: true })
    .click();
  await solution.fill(good);
  await page.getByRole("button", { name: fill(m.run, { count: 1 }) }).click();
  await settled(page);
  await expect(
    rows(page).filter({ hasText: fill(m.status.pass, { total: 8 }) }),
  ).toHaveCount(3);
  expect(own(by), "the fix updated the same problem").toBe(3);
  expect(
    sql(
      `select string_agg(c::text, ',') from (select count(t.id) c from problem.problems p join problem.testcases t on t.problem_id=p.id where p.author_id='${by.id}' and not p.deleted group by p.id) x`,
    ),
    "eight testcases each, none repeated",
  ).toBe("8,8,8");

  // Publish one call per problem and give them to the class.
  const calls: number[] = [];
  page.on("request", (r) => {
    if (r.method() === "POST" && r.url().endsWith("/bulk/publish"))
      calls.push(
        (r.postDataJSON() as { problemIds: string[] }).problemIds.length,
      );
  });
  await page
    .getByRole("button", { name: fill(m.publishCount, { count: 3 }) })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel(m.publish.classLabel).selectOption(klass.classId);
  await dialog
    .getByRole("button", { name: fill(m.publish.confirm, { count: 3 }) })
    .click();
  await settled(page);
  await expect(rows(page).filter({ hasText: m.status.published })).toHaveCount(
    3,
  );
  expect(calls).toEqual([1, 1, 1]);
  expect(own(by, "and status='PUBLISHED'")).toBe(3);
  expect(
    sql(
      `select count(*) from problem.class_assignments where class_id='${klass.classId}' and removed_at is null`,
    ),
  ).toBe("3");

  // The learner gets Accepted with the stored reference solution.
  const [problemId, language, code] = sql(
    `select p.id||chr(31)||r.language||chr(31)||r.source_code from problem.problems p join problem.problem_reference_solutions r on r.problem_id=p.id where p.author_id='${by.id}' and p.status='PUBLISHED' limit 1`,
  ).split("\u001f");
  const sent = await request.post(
    `${API}/judge/problems/${problemId}/submissions`,
    {
      headers: { Authorization: `Bearer ${learner.token}` },
      data: {
        language,
        submissionModel: "STANDARD_IO",
        sourceCode: code,
        sourceInputMethod: "EDITOR",
      },
    },
  );
  expect(sent.ok(), "submit").toBe(true);
  const { submissionId } = await sent.json();
  await expect
    .poll(
      () =>
        sql(`select status from judge.submissions where id='${submissionId}'`),
      {
        timeout: 90_000,
      },
    )
    .toBe("ACCEPTED");
});

test("a create whose answer is lost is not repeated until the author confirms", async ({
  page,
  request,
}) => {
  test.setTimeout(3 * 60_000);
  const by = await person(request, "INSTRUCTOR");
  authors.push(by.id);
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, by.email, by.password);
  await expect(page).toHaveURL(/\/instructor\/overview/, { timeout: 30_000 });
  await page.goto(`${APP_URL}/instructor/problems/import`);
  await page.getByTestId("problem-import-file").setInputFiles(TEMPLATE);
  await expect(rows(page)).toHaveCount(5);

  let cut = true;
  await page.route("**/api/v1/problem-bank/authoring/problems", (route) => {
    if (cut && route.request().method() === "POST") {
      cut = false;
      return route.abort("connectionreset");
    }
    return route.continue();
  });
  await page.getByRole("button", { name: fill(m.run, { count: 3 }) }).click();
  await settled(page);
  await expect(rows(page).first()).toContainText(m.status.unknown);
  expect(own(by), "the other two were still created").toBe(2);

  await rows(page).first().getByRole("button", { name: m.createAgain }).click();
  // The row with the wrong solution is sent again too; it updates its problem.
  await page.getByRole("button", { name: fill(m.run, { count: 2 }) }).click();
  await settled(page);
  await expect(rows(page).first()).toContainText(
    fill(m.status.pass, { total: 8 }),
  );
  expect(own(by)).toBe(3);
});

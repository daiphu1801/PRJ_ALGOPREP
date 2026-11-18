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

test("F2 learner closure: whole-class meta, lenient URL, private note guard and account switch", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(180_000);
  const learner = await createUser(request, { tag: "f2student-a" });
  const other = await createUser(request, { tag: "f2student-b" });
  const teacher = await createUser(request, {
    tag: "f2student-teacher",
    role: "INSTRUCTOR",
  });
  const learnerId = sql(
    `select id from identity.users where email='${learner.email}'`,
  );
  const teacherId = sql(
    `select id from identity.users where email='${teacher.email}'`,
  );
  const classId = randomUUID();
  const prefix = `F2-STUDENT-${Date.now()}`;
  const problemIds = Array.from({ length: 25 }, () => randomUUID());
  sql(`insert into identity.classes(id,instructor_id,name) values('${classId}','${teacherId}','${prefix} class');
       insert into identity.class_enrollments(id,class_id,student_id) values(gen_random_uuid(),'${classId}','${learnerId}');
       insert into problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported,published_at)
       values ${problemIds.map((id, i) => `('${id}','${prefix}-${i}','${prefix} problem ${i}','Read two numbers.','${teacherId}',(select id from problem.problem_levels order by sort_order limit 1),'PUBLISHED',1000,256,64,false,now())`).join(",")};
       insert into problem.class_assignments(id,problem_id,class_id,assigned_by)
       values ${problemIds.map((id) => `(gen_random_uuid(),'${id}','${classId}','${teacherId}')`).join(",")};
       insert into problem.bookmarks(user_id,problem_id,note) values('${learnerId}','${problemIds[0]}','${prefix} private original');
       update problem.catalog_cache_epoch set version=version+1;`);

  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, learner.email, learner.password);
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 20_000 });

  const progressResponses: {
    items: unknown[];
    page: { totalItems: number };
    meta: { totalAssigned: number; solvedCount: number | null };
  }[] = [];
  page.on("response", async (response) => {
    if (
      response.url().includes("/problem-bank/me/class-problems") &&
      response.ok()
    )
      progressResponses.push(await response.json());
  });
  await page.getByRole("link", { name: vi.nav.myClasses, exact: true }).click();
  await expect(
    page.getByText(`${prefix} class`, { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("Đã xong 0/25 bài", { exact: true }),
  ).toBeVisible();
  await expect.poll(() => progressResponses.length).toBe(1);
  expect(progressResponses[0]?.items).toHaveLength(20);
  expect(progressResponses[0]?.meta).toEqual({
    totalAssigned: 25,
    solvedCount: 0,
  });
  await page.screenshot({
    path: testInfo.outputPath("whole-class-progress.png"),
    fullPage: true,
  });

  const catalogResponse = page.waitForResponse(
    (response) =>
      new URL(response.url()).pathname.endsWith("/problem-bank/problems") &&
      response.request().method() === "GET" &&
      response.ok(),
  );
  await page.goto(
    `${APP_URL}/problems?status=BAD&level=unknown-level&topic=unknown-topic&sort=invalid&dir=invalid&page=-5`,
  );
  const catalog = await catalogResponse;
  const query = new URL(catalog.url()).searchParams;
  expect(query.get("status")).toBeNull();
  expect(query.get("difficulty")).toBeNull();
  expect(query.get("topic")).toBeNull();
  expect(query.get("page")).toBe("1");
  expect(query.get("sort")).toBe("level");
  expect(query.get("dir")).toBe("asc");
  await expect(page.getByRole("table")).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("catalog-invalid-url.png"),
    fullPage: true,
  });

  await page.getByRole("button", { name: vi.nav.accountMenu }).click();
  await page
    .getByRole("banner")
    .getByRole("link", { name: vi.nav.saved, exact: true })
    .click();
  await page.getByText(`${prefix} private original`, { exact: true }).click();
  const note = page.locator("textarea");
  await note.fill(`${prefix} private changed`);
  await page.getByRole("link", { name: vi.nav.problems, exact: true }).click();
  const guard = page.getByRole("dialog");
  await expect(guard.getByText(vi.savedProblems.unsaved.title)).toBeVisible();
  await guard
    .getByRole("button", { name: vi.savedProblems.unsaved.stay })
    .click();
  await expect(note).toHaveValue(`${prefix} private changed`);
  const noteSaved = page.waitForResponse(
    (response) =>
      response.url().endsWith(`/bookmarks/${problemIds[0]}`) &&
      response.request().method() === "PUT",
  );
  await page
    .getByRole("button", { name: vi.savedProblems.table.noteSave, exact: true })
    .click();
  expect((await noteSaved).ok()).toBe(true);
  await expect(
    page.getByText(`${prefix} private changed`, { exact: true }),
  ).toBeVisible();
  expect(
    sql(
      `select note from problem.bookmarks where user_id='${learnerId}' and problem_id='${problemIds[0]}'`,
    ),
  ).toBe(`${prefix} private changed`);
  await page.screenshot({
    path: testInfo.outputPath("saved-private-note.png"),
    fullPage: true,
  });

  await page.getByRole("button", { name: vi.nav.accountMenu }).click();
  await page.getByRole("link", { name: vi.nav.logout, exact: true }).click();
  await expect(page).toHaveURL(/\/login/, { timeout: 20_000 });
  await submitLogin(page, other.email, other.password);
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 20_000 });
  await page.getByRole("button", { name: vi.nav.accountMenu }).click();
  await page
    .getByRole("banner")
    .getByRole("link", { name: vi.nav.saved, exact: true })
    .click();
  await expect(
    page.getByText(vi.savedProblems.emptyAll, { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText(`${prefix} private changed`, { exact: true }),
  ).toHaveCount(0);
  await page.getByRole("link", { name: vi.nav.myClasses, exact: true }).click();
  await expect(
    page.getByText(vi.myClasses.empty, { exact: true }),
  ).toBeVisible();
  await expect(page.getByText(`${prefix} class`, { exact: true })).toHaveCount(
    0,
  );
  await page.screenshot({
    path: testInfo.outputPath("second-account-empty.png"),
    fullPage: true,
  });
});

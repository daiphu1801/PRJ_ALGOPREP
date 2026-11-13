import { randomUUID } from "node:crypto";
import { readFile } from "node:fs/promises";
import { expect, test, type Page } from "@playwright/test";
import {
  APP_URL,
  createUser,
  requireRealStack,
  sql,
  submitLogin,
  vi,
} from "./support/real-stack";

requireRealStack();
test.setTimeout(180_000);

async function menu(page: Page, label: string) {
  await page.getByRole("button", { name: vi.nav.accountMenu }).click();
  await page
    .getByRole("banner")
    .getByRole("link", { name: label, exact: true })
    .click();
}

test("session cache isolation, real interview summary and transcript download", async ({
  page,
  request,
}, info) => {
  const first = await createUser(request, { tag: "followup-cache-a" });
  const second = await createUser(request, { tag: "followup-cache-b" });
  const summaries: number[] = [];
  const profiles: string[] = [];
  page.on("response", async (response) => {
    if (response.url().includes("/ai/me/interview-sessions/summary"))
      summaries.push(response.status());
    if (response.url().endsWith("/identity/me/profile") && response.ok())
      profiles.push((await response.json()).email);
  });
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, first.email, first.password);
  await expect(page).toHaveURL(/\/dashboard/);
  await menu(page, vi.nav.profile);
  await expect(page.getByText(first.email, { exact: true })).toBeVisible();
  await expect.poll(() => summaries.includes(200)).toBe(true);
  await page
    .getByRole("link", { name: vi.nav.submissions, exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: vi.mySubmissions.title }),
  ).toBeVisible();
  await page.evaluate(() => {
    (
      window as Window & { followupSessionMarker?: string }
    ).followupSessionMarker = "same-SPA";
  });
  const started = Date.now();
  await menu(page, vi.nav.logout);
  await expect(page).toHaveURL(/\/login/);
  await submitLogin(page, second.email, second.password);
  await expect(page).toHaveURL(/\/dashboard/);
  await menu(page, vi.nav.profile);
  await expect(page.getByText(second.email, { exact: true })).toBeVisible();
  await expect(page.getByText(first.email, { exact: true })).toHaveCount(0);
  expect(
    await page.evaluate(
      () =>
        (window as Window & { followupSessionMarker?: string })
          .followupSessionMarker,
    ),
  ).toBe("same-SPA");
  expect(Date.now() - started).toBeLessThan(30_000);
  expect(profiles).toContain(first.email);
  expect(profiles).toContain(second.email);
  await page.screenshot({
    path: info.outputPath("account-b-profile.png"),
    fullPage: true,
  });
  await menu(page, vi.nav.settings);
  const response = page.waitForResponse((value) =>
    value.url().endsWith("/identity/me/export/interview-transcripts"),
  );
  const download = page.waitForEvent("download");
  await page
    .getByRole("button", {
      name: new RegExp(vi.settings.dataExport.exportInterviews),
    })
    .click();
  expect((await response).status()).toBe(200);
  const file = await download;
  expect(JSON.parse(await readFile((await file.path())!, "utf8"))).toEqual([]);
});

test("submission filters hydrate from URL, survive refresh and browser history", async ({
  page,
  request,
}, info) => {
  const learner = await createUser(request, { tag: "followup-url" });
  const id = sql(
    `select id from identity.users where email='${learner.email}'`,
  );
  const problem = randomUUID();
  const title = `FOLLOWUP-URL-${Date.now()}`;
  sql(`insert into problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported,published_at)
       values('${problem}','${title}','${title}','URL filter fixture','${id}',(select id from problem.problem_levels order by sort_order limit 1),'PUBLISHED',1000,256,64,false,now());
       insert into judge.submissions(id,user_id,problem_id,spec_version,language,submission_model,source_code,source_input_method,status,passed_testcase_count,total_testcase_count,score_ratio,testcase_set_version_at_grading,submitted_at,finished_at,updated_at)
       select gen_random_uuid(),'${id}','${problem}',1,'PYTHON','STANDARD_IO','print(1)','EDITOR','ACCEPTED',1,1,1,1,now() - (i || ' seconds')::interval,now(),now() from generate_series(1,25) i;`);
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, learner.email, learner.password);
  await expect(page).toHaveURL(/\/dashboard/);
  const target = `/submissions?verdict=ACCEPTED&language=PYTHON&q=${title}&page=2`;
  const loaded = page.waitForResponse(
    (value) =>
      value.url().includes("/judge/me/submissions?") &&
      new URL(value.url()).searchParams.get("page") === "2",
  );
  await page.goto(`${APP_URL}${target}`);
  const payload = await (await loaded).json();
  expect(payload.items).toHaveLength(5);
  expect(payload.totalCount).toBe(25);
  const search = page.getByRole("textbox", {
    name: vi.mySubmissions.filter.searchLabel,
  });
  await expect(search).toHaveValue(title);
  await expect(
    page
      .getByRole("table")
      .getByRole("link", { name: new RegExp(title) }),
  ).toHaveCount(5);
  await page.reload();
  await expect(search).toHaveValue(title);
  await expect(
    page
      .getByRole("table")
      .getByRole("link", { name: new RegExp(title) }),
  ).toHaveCount(5);
  await page
    .getByRole("button", {
      name: new RegExp(vi.mySubmissions.filter.verdictLabel),
    })
    .click();
  await page
    .getByRole("option", { name: vi.mySubmissions.filter.all, exact: true })
    .click();
  await expect
    .poll(() => new URL(page.url()).searchParams.get("page"))
    .toBeNull();
  await expect
    .poll(() => new URL(page.url()).searchParams.get("verdict"))
    .toBeNull();
  await page.goBack();
  await expect
    .poll(() => new URL(page.url()).searchParams.get("page"))
    .toBe("2");
  await expect(
    page
      .getByRole("table")
      .getByRole("link", { name: new RegExp(title) }),
  ).toHaveCount(5);
  await page.screenshot({
    path: info.outputPath("submission-url-page2.png"),
    fullPage: true,
  });
});

// F2 closure on the real HTTP/PostgreSQL/MinIO stack. Fixtures belong to a unique instructor.
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
test.setTimeout(150_000);
const LEVEL = "20000000-0000-4000-8000-000000000001";
const m = vi.problemManagement;
const a = vi.problemAuthoring;

async function teacher(
  page: Page,
  request: Parameters<typeof createUser>[0],
  tag: string,
) {
  const user = await createUser(request, { tag, role: "INSTRUCTOR" });
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/\/instructor\/overview/, { timeout: 20_000 });
  return sql(`select id from identity.users where email='${user.email}'`);
}

function problem(author: string, prefix: string, status = "UNPUBLISHED") {
  const id = randomUUID();
  sql(`insert into problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
       values('${id}','${prefix}-${id.slice(0, 8)}','${prefix}-${id.slice(0, 8)}','Read two integers and print their sum.','${author}','${LEVEL}','${status}',1000,256,64,false)`);
  return id;
}

function topics() {
  const first = randomUUID();
  const second = randomUUID();
  const names = [
    `Closure ${first.slice(0, 8)}`,
    `Closure ${second.slice(0, 8)}`,
  ];
  sql(`insert into problem.topics(id,code,name,sort_order) values
       ('${first}','E2E-${first}','${names[0]}',20),('${second}','E2E-${second}','${names[1]}',21)`);
  return { first, second, names };
}

test("management bulk adds a topic, withdraws only selected rows, exports all filtered rows", async ({
  page,
  request,
}, info) => {
  const owner = await teacher(page, request, "closure-bulk");
  const prefix = `Closure-${randomUUID().slice(0, 8)}`;
  const ids = Array.from({ length: 10 }, () =>
    problem(owner, prefix, "PUBLISHED"),
  );
  const topic = topics();
  sql(
    `insert into problem.problem_topics(problem_id,topic_id) values ${ids.map((id) => `('${id}','${topic.first}')`).join(",")}`,
  );
  await page.goto(`${APP_URL}/instructor/problems`);
  await page.getByLabel(m.searchLabel).fill(prefix);
  await expect(page.getByRole("table").getByRole("row")).toHaveCount(9);
  const visibleRows = page.getByRole("table").getByRole("row");
  const firstTitle = await visibleRows
    .nth(1)
    .getByRole("link")
    .first()
    .innerText();
  const secondTitle = await visibleRows
    .nth(2)
    .getByRole("link")
    .first()
    .innerText();
  await visibleRows.nth(1).getByRole("checkbox").check();
  await visibleRows.nth(2).getByRole("checkbox").check();
  await page
    .getByRole("button", { name: m.bulk.assignTopic, exact: true })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel(m.bulkTopicLabel).selectOption(topic.second);
  const added = page.waitForResponse(
    (r) => r.url().endsWith("/bulk/topic") && r.request().method() === "POST",
  );
  await dialog
    .getByRole("button", { name: m.bulkLevel.apply, exact: true })
    .click();
  const addedResponse = await added;
  expect(addedResponse.status()).toBe(200);
  const selected = (await addedResponse.json()).succeededIds as string[];
  expect(selected).toHaveLength(2);
  expect(
    sql(
      `select count(*) from problem.problem_topics where problem_id in('${selected.join("','")}') and topic_id='${topic.second}'`,
    ),
  ).toBe("2");
  // The original topic is retained wherever it existed: #31 adds, rather than replaces.
  expect(
    sql(
      `select count(*) from problem.problem_topics where problem_id in('${selected.join("','")}') and topic_id='${topic.first}'`,
    ),
  ).toBe("2");
  await expect(dialog).toBeHidden();
  await page
    .getByRole("row")
    .filter({ hasText: firstTitle })
    .getByRole("checkbox")
    .check();
  await page
    .getByRole("row")
    .filter({ hasText: secondTitle })
    .getByRole("checkbox")
    .check();
  await page
    .getByRole("button", { name: m.bulk.unpublish, exact: true })
    .click();
  const withdrawn = page.waitForResponse(
    (r) =>
      r.url().endsWith("/bulk/unpublish") && r.request().method() === "POST",
  );
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.bulk.unpublish, exact: true })
    .click();
  expect((await withdrawn).status()).toBe(200);
  await expect(page.getByRole("dialog")).toBeHidden();
  expect(
    sql(
      `select count(*) from problem.problems where id in('${ids.join("','")}') and status='UNPUBLISHED'`,
    ),
  ).toBe("2");
  const exported = page.waitForResponse(
    (r) => r.url().endsWith("/export") && r.request().method() === "POST",
  );
  const download = page.waitForEvent("download");
  await page
    .getByRole("button", { name: m.exportFiltered, exact: true })
    .click();
  const exportResponse = await exported;
  expect(exportResponse.status()).toBe(200);
  expect(exportResponse.request().postDataJSON()).toEqual({
    filter: { q: prefix },
  });
  const file = await download;
  const path = await file.path();
  expect(path).not.toBeNull();
  const csv = await readFile(path!, "utf8");
  expect(csv.split("\n").filter((line) => line.includes(prefix))).toHaveLength(
    10,
  );
  await page.screenshot({
    path: info.outputPath("management-real.png"),
    fullPage: true,
  });
});

test("authoring persists multiple topics, reorders a full testcase list and edits MinIO content", async ({
  page,
  request,
}, info) => {
  const owner = await teacher(page, request, "closure-editor");
  const id = problem(owner, `Editor-${randomUUID().slice(0, 8)}`);
  const topic = topics();
  sql(
    `insert into problem.problem_topics(problem_id,topic_id) values('${id}','${topic.first}')`,
  );
  await page.goto(`${APP_URL}/instructor/problems/${id}/edit`);
  await page
    .getByRole("checkbox", { name: topic.names[1], exact: true })
    .check();
  const saved = page.waitForResponse(
    (r) =>
      r.url().endsWith(`/authoring/problems/${id}/content`) &&
      r.request().method() === "PUT",
  );
  await page.getByRole("button", { name: a.save, exact: true }).click();
  expect((await saved).status()).toBe(200);
  expect(
    sql(`select count(*) from problem.problem_topics where problem_id='${id}'`),
  ).toBe("2");
  await page.reload();
  await expect(
    page.getByRole("checkbox", { name: topic.names[0], exact: true }),
  ).toBeChecked();
  await expect(
    page.getByRole("checkbox", { name: topic.names[1], exact: true }),
  ).toBeChecked();
  await page.getByRole("button", { name: /^Testcase/ }).click();
  await page
    .getByRole("button", { name: a.testcase.btnBulkUpload, exact: true })
    .click();
  const files = [];
  for (let i = 1; i <= 5; i++) {
    const input = i === 5 ? " x".repeat(4500) + "\n" : `${i} ${i + 1}\n`;
    files.push(
      { name: `0${i}.in`, mimeType: "text/plain", buffer: Buffer.from(input) },
      {
        name: `0${i}.out`,
        mimeType: "text/plain",
        buffer: Buffer.from(`${i + i + 1}\n`),
      },
    );
  }
  await page
    .getByRole("dialog")
    .locator('input[type="file"]')
    .setInputFiles(files);
  await page
    .getByRole("dialog")
    .getByRole("button", {
      name: a.bulk.upload.replace("{count}", "5"),
      exact: true,
    })
    .click();
  await expect(
    page.getByText(a.bulk.done.replace("{count}", "5"), { exact: true }),
  ).toBeVisible({ timeout: 30_000 });
  const before = sql(
    `select string_agg(id::text,',' order by position,id) from problem.testcases where problem_id='${id}'`,
  ).split(",");
  const moves = page.getByRole("button", { name: a.moveDown, exact: true });
  await expect(moves).toHaveCount(5);
  const reordered = page.waitForResponse(
    (r) =>
      r.url().endsWith("/testcases/order") && r.request().method() === "PUT",
  );
  await moves.nth(0).focus();
  await moves.nth(0).press("Enter");
  const reorderResponse = await reordered;
  expect(reorderResponse.status()).toBe(200);
  expect(reorderResponse.request().postDataJSON().orderedIds).toEqual([
    before[1],
    before[0],
    ...before.slice(2),
  ]);
  const minio = sql(
    `select id from problem.testcases where problem_id='${id}' and input_storage='MINIO'`,
  );
  // The only large testcase is identified by its persisted ordinal in the five-row fixture.
  const rows = page.getByRole("table").getByRole("row");
  await expect(rows).toHaveCount(6);
  const refreshed = page.waitForResponse(
    (r) =>
      r.url().endsWith(`/authoring/problems/${id}`) &&
      r.request().method() === "GET",
  );
  await rows.nth(5).getByRole("button", { name: a.edit, exact: true }).click();
  expect((await refreshed).status()).toBe(200);
  const edit = page.getByRole("dialog");
  await expect(edit.getByLabel(a.columnInput)).toHaveValue(
    " x".repeat(4500) + "\n",
  );
  await edit.getByLabel(a.columnInput).fill(" 7 8\n");
  const patched = page.waitForResponse(
    (r) =>
      r.url().endsWith(`/testcases/${minio}`) &&
      r.request().method() === "PATCH",
  );
  await edit.getByRole("button", { name: a.dialog.save, exact: true }).click();
  expect((await patched).status()).toBe(200);
  await expect(edit).toBeHidden();
  expect(
    sql(
      `select input_storage||'|'||encode(convert_to(input_inline,'UTF8'),'hex') from problem.testcases where id='${minio}'`,
    ),
  ).toBe("INLINE|203720380a");
  await page.reload();
  await page.getByRole("button", { name: /^Testcase/ }).click();
  expect(
    sql(
      `select string_agg(id::text,',' order by position,id) from problem.testcases where problem_id='${id}'`,
    ),
  ).toBe([before[1], before[0], ...before.slice(2)].join(","));
  await expect(
    page.getByRole("cell", { name: "7 8", exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: info.outputPath("authoring-real.png"),
    fullPage: true,
  });
});

test("a single unapproved testcase runs through the real reference-solution selector", async ({
  page,
  request,
}, info) => {
  test.skip(
    process.env.E2E_F3F4_GRADING !== "true",
    "Requires toolchain sandbox",
  );
  const owner = await teacher(page, request, "closure-one");
  const id = problem(owner, `Single-${randomUUID().slice(0, 8)}`);
  const testcase = randomUUID();
  sql(`insert into problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,matching_strategy,spec_version)
       values(gen_random_uuid(),'${id}','two integers','sum','EXACT',1);
       insert into problem.problem_reference_solutions(id,problem_id,language,source_code)
       values(gen_random_uuid(),'${id}','PYTHON','a,b=map(int,input().split())'||chr(10)||'print(a+b)'||chr(10));
       insert into problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline,origin)
       values('${testcase}','${id}','HIDDEN',1,false,'INLINE','7731 8842','INLINE','16573','AI')`);
  await page.goto(`${APP_URL}/instructor/problems/${id}/edit`);
  await page.getByRole("button", { name: /^Testcase/ }).click();
  const run = page.waitForResponse(
    (r) =>
      r.url().endsWith("/reference-solution/run") &&
      r.request().method() === "POST",
  );
  await page.getByRole("button", { name: a.runOne, exact: true }).click();
  const response = await run;
  expect(response.status()).toBe(200);
  expect(response.request().postDataJSON().testcaseId).toBe(testcase);
  const body = await response.json();
  expect(body.summary).toMatchObject({ total: 1, passed: 1, allPass: true });
  expect(JSON.stringify(body)).not.toContain("7731 8842");
  expect(
    sql(`select approved from problem.testcases where id='${testcase}'`),
  ).toBe("f");
  await page.screenshot({
    path: info.outputPath("single-draft-real.png"),
    fullPage: true,
  });
});

test("a new authoring form uses the current backend defaults before its first save", async ({
  page,
  request,
}, info) => {
  const owner = await teacher(page, request, "closure-defaults");
  const loaded = page.waitForResponse(
    (r) =>
      r.url().endsWith("/problem-bank/authoring/defaults") &&
      r.request().method() === "GET",
  );
  await page.goto(`${APP_URL}/instructor/problems/new`);
  const response = await loaded;
  expect(response.status()).toBe(200);
  const defaults = (await response.json()) as Record<string, number>;
  expect(Object.keys(defaults).sort()).toEqual(
    [
      "timeLimitMs",
      "memoryLimitMb",
      "maxOutputSizeKb",
      "maxAiGenerationsPerProblem",
    ].sort(),
  );
  expect(
    sql(
      "select time_limit_ms||'|'||memory_limit_mb||'|'||output_limit_kb from judge.grading_defaults where id=1",
    ),
  ).toBe(
    `${defaults.timeLimitMs}|${defaults.memoryLimitMb}|${defaults.maxOutputSizeKb}`,
  );
  expect(
    sql(
      "select max_ai_generations_per_problem from problem.problem_bank_settings where id=1",
    ),
  ).toBe(String(defaults.maxAiGenerationsPerProblem));
  await expect(page.getByLabel(a.limit.timeLimitMs, { exact: true })).toHaveValue(
    String(defaults.timeLimitMs),
  );
  await expect(page.getByLabel(a.limit.memoryLimitMb, { exact: true })).toHaveValue(
    String(defaults.memoryLimitMb),
  );
  await expect(page.getByLabel(a.limit.outputLimitKb, { exact: true })).toHaveValue(
    String(defaults.maxOutputSizeKb),
  );
  await page.getByRole("button", { name: a.tab.ai, exact: true }).click();
  await expect(
    page.getByText(
      a.generateQuota
        .replace("{left}", String(defaults.maxAiGenerationsPerProblem))
        .replace("{limit}", String(defaults.maxAiGenerationsPerProblem)),
      { exact: true },
    ),
  ).toBeVisible();
  expect(
    sql(`select count(*) from problem.problems where author_id='${owner}'`),
  ).toBe("0");
  await page.screenshot({
    path: info.outputPath("new-defaults-real.png"),
    fullPage: true,
  });
});

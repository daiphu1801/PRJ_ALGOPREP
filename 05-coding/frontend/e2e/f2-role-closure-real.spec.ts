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
test.setTimeout(150000);

test("another instructor sees a published problem read-only while ADMIN can edit and read its testcases", async ({
  browser,
  request,
}, info) => {
  const author = await createUser(request, {
    tag: "role-owner",
    role: "INSTRUCTOR",
  });
  const rival = await createUser(request, {
    tag: "role-reader",
    role: "INSTRUCTOR",
  });
  const admin = await createUser(request, { tag: "role-admin", role: "ADMIN" });
  const authorId = sql(
    `select id from identity.users where email='${author.email}'`,
  );
  const id = randomUUID();
  const hidden = `PRIVATE_INPUT_${id}`;
  const output = `PRIVATE_EXPECTED_${id}`;
  const source = `# PRIVATE_SOLUTION_${id}\nprint(3)\n`;
  sql(`insert into problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
    values('${id}','ROLE-${id}','Role privacy ${id.slice(0, 8)}','Read two integers and print their sum.','${authorId}','20000000-0000-4000-8000-000000000001','PUBLISHED',1000,256,64,false);
    insert into problem.problem_reference_solutions(id,problem_id,language,source_code)
    values(gen_random_uuid(),'${id}','PYTHON','${source.replaceAll("'", "''")}');
    insert into problem.testcases(id,problem_id,position,visibility,origin,approved,input_storage,input_inline,expected_output_storage,expected_output_inline)
    values(gen_random_uuid(),'${id}',1,'HIDDEN','MANUAL',true,'INLINE','${hidden}','INLINE','${output}');`);
  const a = vi.problemAuthoring;
  const readerContext = await browser.newContext();
  const reader = await readerContext.newPage();
  await reader.goto(`${APP_URL}/instructor/login`);
  await submitLogin(reader, rival.email, rival.password);
  await expect(reader).toHaveURL(/\/instructor\/overview/);
  const writes: string[] = [];
  reader.on("request", (r) => {
    if (
      r.url().includes(`/authoring/problems/${id}`) &&
      ["POST", "PUT", "PATCH", "DELETE"].includes(r.method())
    )
      writes.push(r.method() + r.url());
  });
  const restrictedResponse = reader.waitForResponse(
    (r) =>
      r.url().endsWith(`/authoring/problems/${id}`) &&
      r.request().method() === "GET",
  );
  await reader.goto(`${APP_URL}/instructor/problems/${id}/edit`);
  const restricted = await restrictedResponse;
  expect(restricted.status()).toBe(200);
  const restrictedBody = await restricted.json();
  expect(restrictedBody.readOnly).toBe(true);
  expect(restrictedBody.referenceSolution).toBeNull();
  expect(restrictedBody.testcases).toEqual([]);
  expect(JSON.stringify(restrictedBody)).not.toContain(hidden);
  expect(JSON.stringify(restrictedBody)).not.toContain(output);
  expect(JSON.stringify(restrictedBody)).not.toContain("PRIVATE_SOLUTION_");
  await expect(
    reader.getByText(a.readOnlyNotice, { exact: true }),
  ).toBeVisible();
  await expect(reader.getByLabel(a.titleLabel, { exact: true })).toBeDisabled();
  await expect(
    reader.getByRole("button", { name: a.save, exact: true }),
  ).toBeDisabled();
  await expect(
    reader.getByRole("button", { name: a.publish, exact: true }),
  ).toBeDisabled();
  await reader
    .getByRole("button", {
      name: a.tab.testcases.replace("{count}", "1"),
      exact: true,
    })
    .click();
  await expect(
    reader.getByText(a.testcasesHiddenNotice.replace("{total}", "1"), {
      exact: true,
    }),
  ).toBeVisible();
  await expect(
    reader.getByText(
      a.testcasesSubtitle.replace("{total}", "1").replace("{publicCount}", "0"),
      { exact: true },
    ),
  ).toBeVisible();
  await expect(reader.getByText(a.emptyTestcases, { exact: true })).toHaveCount(
    0,
  );
  await expect(reader.getByRole("table")).toHaveCount(0);
  await expect(
    reader.getByRole("button", { name: a.addTestcase, exact: true }),
  ).toBeDisabled();
  expect(writes).toEqual([]);
  await reader.screenshot({
    path: info.outputPath("cross-author-read-only.png"),
    fullPage: true,
  });

  const adminContext = await browser.newContext();
  const administrator = await adminContext.newPage();
  await administrator.goto(`${APP_URL}/admin/login`);
  await submitLogin(administrator, admin.email, admin.password);
  await expect(administrator).toHaveURL(/\/admin\/overview/);
  const fullResponse = administrator.waitForResponse(
    (r) =>
      r.url().endsWith(`/authoring/problems/${id}`) &&
      r.request().method() === "GET",
  );
  await administrator.goto(`${APP_URL}/admin/problems/${id}/edit`);
  const full = await fullResponse;
  expect(full.status()).toBe(200);
  const fullBody = await full.json();
  expect(fullBody.readOnly).toBe(false);
  expect(fullBody.testcases).toHaveLength(1);
  expect(fullBody.testcases[0].input).toBe(hidden);
  expect(fullBody.referenceSolution.sourceCode).toBe(source);
  await expect(
    administrator.getByLabel(a.titleLabel, { exact: true }),
  ).toBeEnabled();
  await administrator
    .getByLabel(a.titleLabel, { exact: true })
    .fill(`Admin edit ${id.slice(0, 8)}`);
  await expect(
    administrator.getByRole("button", { name: a.save, exact: true }),
  ).toBeEnabled();
  await administrator
    .getByRole("button", {
      name: a.tab.testcases.replace("{count}", "1"),
      exact: true,
    })
    .click();
  await expect(administrator.getByText(hidden, { exact: true })).toBeVisible();
  await expect(
    administrator.getByRole("button", { name: a.edit, exact: true }),
  ).toBeEnabled();
  const bearer = await full.request().headerValue("authorization");
  expect(bearer).toBeTruthy();
  const settings = await administrator.request.get(
    `${APP_URL}/api/v1/problem-bank/settings/ai-testcase-generation`,
    { headers: { Authorization: bearer! } },
  );
  expect(settings.status()).toBe(200);
  expect((await settings.json()).maxPerProblem).toBe(
    Number(
      sql(
        "select max_ai_generations_per_problem from problem.problem_bank_settings where id=1",
      ),
    ),
  );
  await administrator.screenshot({
    path: info.outputPath("admin-authoring.png"),
    fullPage: true,
  });
  await readerContext.close();
  await adminContext.close();
});

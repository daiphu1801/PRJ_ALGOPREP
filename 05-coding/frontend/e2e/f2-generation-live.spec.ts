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

test("saved authoring generates and persists drafts through the configured live provider", async ({
  page,
  request,
}) => {
  test.skip(
    process.env.E2E_AI_LIVE !== "true",
    "Explicit live provider acceptance only",
  );
  test.setTimeout(240000);
  const user = await createUser(request, {
    tag: "f2-live-gen",
    role: "INSTRUCTOR",
  });
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/\/instructor\/overview/);
  const author = sql(
    `select id from identity.users where email='${user.email}'`,
  );
  const id = randomUUID();
  sql(`insert into problem.problems(id,code,title,statement_md,constraints_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
    values('${id}','LIVE-${id}','Sum of two integers','Read two integers a and b from one line. Print their sum.','-100 <= a,b <= 100','${author}','20000000-0000-4000-8000-000000000001','UNPUBLISHED',1000,256,64,false);
    insert into problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,matching_strategy,spec_version)
    values(gen_random_uuid(),'${id}','Two space separated integers','One integer','EXACT',1);
    insert into problem.problem_reference_solutions(id,problem_id,language,source_code)
    values(gen_random_uuid(),'${id}','PYTHON','a,b=map(int,input().split())'||chr(10)||'print(a+b)'||chr(10));
    insert into problem.testcases(id,problem_id,position,visibility,origin,approved,input_storage,input_inline,expected_output_storage,expected_output_inline)
    values(gen_random_uuid(),'${id}',1,'SAMPLE','MANUAL',true,'INLINE','1 2','INLINE','3'),
    (gen_random_uuid(),'${id}',2,'SAMPLE','MANUAL',true,'INLINE','-2 5','INLINE','3');`);
  const m = vi.problemAuthoring;
  await page.goto(`${APP_URL}/instructor/problems/${id}/edit`);
  await page.getByRole("button", { name: /^Testcase/ }).click();
  const ran = page.waitForResponse((r) =>
    r.url().includes("/reference-solution/run"),
  );
  await page.getByRole("button", { name: m.runSolution, exact: true }).click();
  expect((await ran).status()).toBe(200);
  await expect(
    page.getByRole("button", { name: m.runSolution, exact: true }),
  ).toBeEnabled();
  await page.getByRole("button", { name: m.tab.ai, exact: true }).click();
  const response = page.waitForResponse(
    (r) => r.url().endsWith(`/problems/${id}/testcases/generate`),
    { timeout: 180000 },
  );
  await page
    .getByRole("button", { name: m.generateAction, exact: true })
    .click();
  const generated = await response;
  const responseText = await generated.text();
  const body = JSON.parse(responseText);
  console.warn(
    `LIVE_GENERATION_STATUS=${generated.status()} ERROR_CODE=${body.errorCode ?? "none"}`,
  );
  if (generated.status() === 503) {
    expect(body.errorCode).toBe("PRB-051");
    expect(
      sql(
        `select ai_testcase_generations_used from problem.problems where id='${id}'`,
      ),
    ).toBe("0");
    expect(
      sql(
        `select count(*) from problem.testcases where problem_id='${id}' and origin='AI'`,
      ),
    ).toBe("0");
    await expect(
      page.getByRole("button", { name: m.generateAction, exact: true }),
    ).toBeEnabled();
    await page.screenshot({
      path: "../../06-plan/reports/261110-f2-full-closure/20261111-live-generation-failure.png",
      fullPage: true,
    });
  }
  expect(generated.status(), JSON.stringify(body)).toBe(200);
  expect(body.aiGeneration.used).toBe(1);
  expect(body.testcases.length).toBeGreaterThan(0);
  expect(
    sql(
      `select count(*) from problem.testcases where problem_id='${id}' and origin='AI' and not approved`,
    ),
  ).toBe(String(body.testcases.length));
  await page.screenshot({
    path: "../../06-plan/reports/261110-f2-full-closure/20261111-live-generation.png",
    fullPage: true,
  });
  await page.reload();
  await page.getByRole("button", { name: m.tab.ai, exact: true }).click();
  await expect(
    page.getByText(
      m.draftsSubtitle.replace("{count}", String(body.testcases.length)),
      { exact: true },
    ),
  ).toBeVisible();
});

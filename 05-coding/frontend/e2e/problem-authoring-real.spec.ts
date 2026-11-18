// SHR0202 against the REAL backend: bulk testcase upload straight to MinIO (F2-8a, #45 and #46), running the
// reference solution through the real sandbox (F2-8b, #47), and publishing a standard-I/O-only problem from the
// screen (F-C-01, DEC-2026-1108-f2-spec-declared-server-verdict). Needs the stack in e2e/README.md plus MinIO;
// the two sandbox tests also need E2E_F3F4_GRADING=true and a backend wired to a go-judge with toolchains.
import { randomUUID } from "node:crypto";
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

const m = vi.problemAuthoring;
const LEVEL = "20000000-0000-4000-8000-000000000001";
const NEEDS_SANDBOX =
  "Requires a backend wired to the toolchain sandbox (set E2E_F3F4_GRADING=true)";

async function instructorOn(
  page: Page,
  request: Parameters<typeof createUser>[0],
  tag: string,
) {
  const user = await createUser(request, { tag, role: "INSTRUCTOR" });
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/\/instructor\/overview/, { timeout: 15_000 });
  const id = sql(`select id from identity.users where email='${user.email}'`);
  return { user, id };
}

function draft(authorId: string, code: string): string {
  const id = randomUUID();
  sql(
    `insert into problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
     values('${id}','${code}-${id}','${code} browser check','Read two integers, print the sum.','${authorId}','${LEVEL}','UNPUBLISHED',1000,256,64,false)`,
  );
  return id;
}

const testcasesTab = (page: Page) =>
  page.getByRole("button", { name: /^Testcase/ });

test("bulk upload: five pairs reach the server, only the oversized side stays in MinIO", async ({
  page,
  request,
}) => {
  const { id: authorId } = await instructorOn(page, request, "bulk");
  const problemId = draft(authorId, "E2E-BULK");

  await page.goto(`${APP_URL}/instructor/problems/${problemId}/edit`);
  await testcasesTab(page).click();
  await page.getByRole("button", { name: m.testcase.btnBulkUpload }).click();
  const dialog = page.getByRole("dialog");

  const files = [];
  for (let i = 1; i <= 5; i++) {
    const name = String(i).padStart(2, "0");
    // Pair 05 has an input above the 8 KB inline boundary (DD api F2 section 46), the rest are tiny.
    const input = i === 5 ? "x".repeat(9000) + "\n" : `${i} ${i + 1}\n`;
    files.push(
      {
        name: `${name}.in`,
        mimeType: "text/plain",
        buffer: Buffer.from(input),
      },
      {
        name: `${name}.out`,
        mimeType: "text/plain",
        buffer: Buffer.from(i === 5 ? "9000\n" : `${i + i + 1}\n`),
      },
    );
  }
  await dialog.locator('input[type="file"]').setInputFiles(files);
  const upload = dialog.getByRole("button", {
    name: m.bulk.upload.replace("{count}", "5"),
  });
  await expect(upload).toBeEnabled();
  await upload.click();

  await expect(page.getByText(m.bulk.done.replace("{count}", "5"))).toBeVisible(
    { timeout: 30_000 },
  );
  await expect(
    page.getByRole("button", { name: /^Testcase \(5\)/ }),
  ).toBeVisible();
  // Four pairs are read back into the inline columns, the 9000 character input stays in MinIO.
  expect(
    sql(
      `select count(*)||'|'||count(*) filter (where input_storage='MINIO')||'|'||count(*) filter (where approved and visibility='HIDDEN') from problem.testcases where problem_id='${problemId}'`,
    ),
  ).toBe("5|1|5");
});

test("reference run reports each case with verdict words only and never saves the typed code", async ({
  page,
  request,
}) => {
  test.skip(process.env.E2E_F3F4_GRADING !== "true", NEEDS_SANDBOX);
  test.setTimeout(150_000);
  const { id: authorId } = await instructorOn(page, request, "refrun");
  const problemId = draft(authorId, "E2E-RUN");
  // Distinctive hidden inputs, so a leak into the response is detectable (single digits would prove nothing).
  const secrets = ["7731 8842", "6612 9953", "5503 1164"];
  sql(`insert into problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,matching_strategy,spec_version)
       values(gen_random_uuid(),'${problemId}','two integers','their sum','EXACT',1);
       insert into problem.problem_reference_solutions(id,problem_id,language,source_code)
       values(gen_random_uuid(),'${problemId}','PYTHON','a, b = map(int, input().split())'||chr(10)||'print(a + b)'||chr(10));
       insert into problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline,origin)
       values ${secrets
         .map((value, i) => {
           const [a = 0, b = 0] = value.split(" ").map(Number);
           return `(gen_random_uuid(),'${problemId}','HIDDEN',${i + 1},true,'INLINE','${value}','INLINE','${a + b}','MANUAL')`;
         })
         .join(",")};`);

  await page.goto(`${APP_URL}/instructor/problems/${problemId}/edit`);
  await testcasesTab(page).click();
  const run = page.getByRole("button", { name: m.runSolution });

  const good = page.waitForResponse((r) =>
    r.url().includes("/reference-solution/run"),
  );
  await run.click();
  const goodBody = await (await good).text();
  await expect(page.getByText(/Đúng 3\/3 testcase đã duyệt/)).toBeVisible({
    timeout: 60_000,
  });
  const keys = new Set(
    (
      JSON.parse(goodBody) as { results: Record<string, unknown>[] }
    ).results.flatMap((row) => Object.keys(row)),
  );
  for (const forbidden of ["input", "expectedOutput", "stdout", "stderr"]) {
    expect([...keys].map((key) => key.toLowerCase())).not.toContain(
      forbidden.toLowerCase(),
    );
  }

  await page.getByRole("button", { name: m.tab.content }).click();
  await page.getByLabel(/Đáp án mẫu bằng/).fill("print(0)\n");
  await testcasesTab(page).click();
  const bad = page.waitForResponse((r) =>
    r.url().includes("/reference-solution/run"),
  );
  await run.click();
  const badBody = await (await bad).text();
  await expect(page.getByText(/Thất bại ở testcase #\d+/)).toBeVisible({
    timeout: 60_000,
  });
  for (const secret of secrets) {
    expect(badBody).not.toContain(secret);
  }
  expect(
    sql(
      `select source_code from problem.problem_reference_solutions where problem_id='${problemId}'`,
    ),
  ).not.toContain("print(0)");
});

test("a standard-I/O-only problem reads as spec-declared and publishes from the screen", async ({
  page,
  request,
}) => {
  test.skip(process.env.E2E_F3F4_GRADING !== "true", NEEDS_SANDBOX);
  test.setTimeout(150_000);
  const { id: authorId } = await instructorOn(page, request, "stdio");
  const problemId = draft(authorId, "E2E-STDIO");
  sql(`insert into problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,matching_strategy,spec_version)
       values(gen_random_uuid(),'${problemId}','two integers','their sum','EXACT',1);
       insert into problem.worked_examples(id,problem_id,position,input,output)
       values(gen_random_uuid(),'${problemId}',1,'1 2','3'),(gen_random_uuid(),'${problemId}',2,'2 3','5');
       insert into problem.problem_reference_solutions(id,problem_id,language,source_code)
       values(gen_random_uuid(),'${problemId}','PYTHON','a, b = map(int, input().split())'||chr(10)||'print(a + b)'||chr(10));
       insert into problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline,origin)
       select gen_random_uuid(),'${problemId}',case when i<=2 then 'SAMPLE' else 'HIDDEN' end,i,true,'INLINE',i||' '||(i+1),'INLINE',(i+i+1)::text,'MANUAL' from generate_series(1,8) i;`);

  await page.goto(`${APP_URL}/instructor/problems/${problemId}/edit`);
  // Before the fix this item stayed "Chưa" because the client demanded a function signature.
  const specItem = page.locator("li", { hasText: m.check.specDeclared });
  await expect(specItem.getByText(m.checkDone)).toBeVisible();

  await testcasesTab(page).click();
  await page.getByRole("button", { name: m.runSolution }).click();
  await expect(page.getByText(/Đúng 8\/8 testcase đã duyệt/)).toBeVisible({
    timeout: 60_000,
  });
  // The button looks locked while the client checklist is incomplete but a click always reaches the server.
  await page.getByRole("button", { name: m.publish }).click({ force: true });
  await expect(page.getByText(m.toast.published)).toBeVisible({
    timeout: 30_000,
  });
  expect(
    sql(`select status from problem.problems where id='${problemId}'`),
  ).toBe("PUBLISHED");
});

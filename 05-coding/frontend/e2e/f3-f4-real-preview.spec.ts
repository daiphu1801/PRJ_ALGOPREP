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

// AC-49 in a real browser: the signature lines on the "Đặc tả" tab come from the F3 preview
// endpoint, follow the draft, save nothing, and equal the starter code of a saved problem.
// Run with E2E_PG_DB set to the backend database.
requireRealStack();
test.setTimeout(120000);

const spec = vi.problemAuthoring.spec;
const PREVIEW = "/harness/function-signature/preview";

type Declarations = Record<"JAVA" | "CPP" | "PYTHON", string | null>;

const previewResponse = (page: Page) =>
  page.waitForResponse(
    (r) => r.url().includes(PREVIEW) && r.request().method() === "POST",
  );

async function openSpecTab(page: Page, email: string, password: string) {
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, email, password);
  await expect(page).toHaveURL(/instructor\/overview/);
  await page.goto(`${APP_URL}/instructor/problems/new`);
  await page
    .getByRole("button", { name: vi.problemAuthoring.tab.spec, exact: true })
    .click();
  await expect(page.getByLabel(spec.functionName)).toBeVisible();
}

function row(page: Page, language: "java" | "cpp" | "python") {
  return page.getByRole("listitem", {
    name: spec.language[language],
    exact: true,
  });
}

test("the signature lines come from the server, follow the draft and save nothing", async ({
  page,
  request,
}) => {
  const author = await createUser(request, {
    tag: "f3-preview-author",
    role: "INSTRUCTOR",
  });
  await openSpecTab(page, author.email, author.password);
  const problemsBefore = sql("select count(*) from problem.problems");
  const specsBefore = sql("select count(*) from problem.problem_specs");

  const first = previewResponse(page);
  await page.getByLabel(spec.functionName).fill("e2e_echo");
  await page.getByRole("button", { name: spec.addParameter }).click();
  await page
    .getByLabel(spec.parameterName.replace("{index}", "1"))
    .fill("value");
  // The author is still typing, so each call is debounced: wait for the one that holds the whole
  // signature (a parameter named "value").
  let response = await first;
  for (let i = 0; i < 5; i++) {
    if (JSON.stringify(response.request().postDataJSON()).includes('"value"')) {
      break;
    }
    response = await previewResponse(page);
  }
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.wrapperSupported).toBe(true);
  const intDeclarations = body.declarations as Declarations;
  for (const [key, language] of [
    ["JAVA", "java"],
    ["CPP", "cpp"],
    ["PYTHON", "python"],
  ] as const) {
    await page
      .getByRole("button", { name: spec.language[language], exact: true })
      .click();
    expect(intDeclarations[key]).toBeTruthy();
    await expect(row(page, language)).toContainText(
      intDeclarations[key]!.trim().split("\n")[0]!,
    );
  }

  // The name is derived per language: snake_case for Python, camelCase for Java and C++.
  expect(intDeclarations.PYTHON).toContain("e2e_echo");
  expect(intDeclarations.JAVA).toContain("e2eEcho");
  expect(intDeclarations.CPP).toContain("e2eEcho");

  // Change the return type: the lines change with it.
  const changed = previewResponse(page);
  await page.getByLabel(spec.returnType).selectOption({ label: "string" });
  const changedBody = await (await changed).json();
  await page
    .getByRole("button", { name: spec.language.java, exact: true })
    .click();
  expect(changedBody.declarations.JAVA).not.toBe(intDeclarations.JAVA);
  await expect(row(page, "java")).toContainText(
    changedBody.declarations.JAVA.trim().split("\n")[0],
  );

  // A wrong name is refused by the server (HRN-060) and the page keeps working.
  const refused = previewResponse(page);
  await page.getByLabel(spec.functionName).fill("2bad");
  let wrong = await refused;
  for (let i = 0; i < 5 && wrong.status() === 200; i++) {
    wrong = await previewResponse(page);
  }
  expect(wrong.status()).toBe(422);
  expect((await wrong.json()).errorCode).toBe("HRN-060");
  await expect(page.getByLabel(spec.functionName)).toBeVisible();

  // Previewing is not saving.
  expect(sql("select count(*) from problem.problems")).toBe(problemsBefore);
  expect(sql("select count(*) from problem.problem_specs")).toBe(specsBefore);
});

test("the previewed lines equal the starter code of a problem saved with the same signature", async ({
  page,
  request,
}) => {
  const author = await createUser(request, {
    tag: "f3-match-author",
    role: "INSTRUCTOR",
  });
  const reader = await createUser(request, { tag: "f3-match-reader" });
  await openSpecTab(page, author.email, author.password);
  await page.getByLabel(spec.functionName).fill("e2e_echo");
  await page.getByRole("button", { name: spec.addParameter }).click();
  await page
    .getByLabel(spec.parameterName.replace("{index}", "1"))
    .fill("value");
  let response = await previewResponse(page);
  for (let i = 0; i < 5; i++) {
    if (JSON.stringify(response.request().postDataJSON()).includes('"value"')) {
      break;
    }
    response = await previewResponse(page);
  }
  expect(response.status()).toBe(200);
  const declarations = (await response.json()).declarations as Declarations;

  const problemId = randomUUID();
  const authorId = sql(
    `select id from identity.users where email='${author.email}'`,
  );
  sql(`insert into problem.problems(id,code,title,statement_md,author_id,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported,status,published_at)
    values('${problemId}','FE-${problemId}','F3 preview match','x','${authorId}','20000000-0000-4000-8000-000000000001',2000,256,64,true,'PUBLISHED',clock_timestamp());
    insert into problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,matching_strategy,spec_version,function_name,return_type,parameters)
    values(gen_random_uuid(),'${problemId}','x','x','EXACT',1,'e2e_echo','{"kind":"INT"}'::jsonb,'[{"name":"value","type":{"kind":"INT"}}]'::jsonb);`);
  const login = await request.post(`${APP_URL}/api/v1/identity/auth/login`, {
    data: { email: reader.email, password: reader.password },
  });
  const { accessToken } = await login.json();
  for (const language of ["JAVA", "CPP", "PYTHON"] as const) {
    const starter = await request.get(
      `${APP_URL}/api/v1/harness/problems/${problemId}/starter-code?language=${language}&model=FUNCTION_WRAPPER`,
      { headers: { Authorization: `Bearer ${accessToken}` } },
    );
    expect(starter.status()).toBe(200);
    expect((await starter.json()).sourceCode).toContain(
      declarations[language]!,
    );
  }
});

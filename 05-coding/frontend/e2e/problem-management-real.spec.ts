// SHR0201 (problem management) against the REAL backend: list filters and the confirmed delete. Replaces the
// mock-mode `problem-management.spec.ts` (fixed titles and counts). The test seeds its own problems in the
// database (code and title prefix `E2E-PM-<ts>`) and derives every expected number from them. Needs the stack
// in e2e/README.md; skips itself otherwise.
import { expect, test } from "@playwright/test";
import { pickFilter } from "./filter-menu";
import { APP_URL, requireRealStack, sql, vi } from "./support/real-stack";
import { loginAsAdmin, userId } from "./support/real-session";

requireRealStack();

const m = vi.problemManagement;
const LEVEL = "20000000-0000-4000-8000-000000000001";
const run = `E2E-PM-${Date.now()}`;

function problem(
  authorId: string,
  n: number,
  status: "PUBLISHED" | "UNPUBLISHED",
): string {
  const title = `${run} title ${n}`;
  sql(
    `insert into problem.problems(id,code,title,statement_md,author_id,level_id,status,published_at,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
     values(gen_random_uuid(),'${run}-${n}','${title}','Read two integers, print the sum.','${authorId}','${LEVEL}','${status}',${status === "PUBLISHED" ? "now()" : "null"},1000,256,64,false)`,
  );
  return title;
}

const resultText = (shown: number, total: number) =>
  m.resultCount
    .replace("{shown}", String(shown))
    .replace("{total}", String(total));

test("search and status filter narrow the list and the count follows the database", async ({
  page,
  request,
}) => {
  const admin = await loginAsAdmin(page, request, "pmg-list");
  const author = userId(admin.email);
  problem(author, 1, "UNPUBLISHED");
  problem(author, 2, "UNPUBLISHED");
  problem(author, 3, "PUBLISHED");
  await page.goto(`${APP_URL}/admin/problems`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.title);

  const overall = Number(
    sql("select count(*) from problem.problems where not deleted"),
  );
  const rows = page
    .getByRole("table", { name: m.tableCaption })
    .locator("tbody tr");

  await page.getByLabel(m.searchLabel).fill(run);
  await expect(rows).toHaveCount(3, { timeout: 15_000 });
  await expect(page.getByText(resultText(3, overall))).toBeVisible();

  await pickFilter(page, m.statusFilterLabel, m.status.draft);
  await expect(rows).toHaveCount(2, { timeout: 10_000 });
  await expect(page.getByText(resultText(2, overall))).toBeVisible();
  await expect(rows.filter({ hasText: m.status.published })).toHaveCount(0);

  await pickFilter(page, m.statusFilterLabel, m.status.published);
  await expect(rows).toHaveCount(1, { timeout: 10_000 });
  await expect(rows.first()).toContainText(`${run} title 3`);
});

test("deleting a problem asks for confirmation first and only then hides it", async ({
  page,
  request,
}) => {
  const admin = await loginAsAdmin(page, request, "pmg-del");
  const title = problem(userId(admin.email), 9, "UNPUBLISHED");
  await page.goto(`${APP_URL}/admin/problems`);
  await page.getByLabel(m.searchLabel).fill(title);
  const row = page
    .getByRole("table", { name: m.tableCaption })
    .locator("tbody tr")
    .filter({ hasText: title });
  await expect(row).toHaveCount(1, { timeout: 15_000 });

  await page
    .getByRole("button", { name: m.deleteProblem.replace("{title}", title) })
    .click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toContainText(
    m.confirmDeleteTitle.replace("{title}", title),
  );
  // Nothing is deleted while the dialog is open, nor after cancelling.
  expect(
    sql(`select deleted from problem.problems where title='${title}'`),
  ).toBe("f");
  await dialog.getByRole("button", { name: m.cancel }).click();
  await expect(dialog).toHaveCount(0);
  expect(
    sql(`select deleted from problem.problems where title='${title}'`),
  ).toBe("f");
  await expect(row).toHaveCount(1);

  await page
    .getByRole("button", { name: m.deleteProblem.replace("{title}", title) })
    .click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.delete, exact: true })
    .click();
  await expect(row).toHaveCount(0, { timeout: 15_000 });
  expect(
    sql(`select deleted from problem.problems where title='${title}'`),
  ).toBe("t");
});

// ADM0202 (permission matrix) against the REAL backend: guards the fixed function list and the read-only
// STUDENT column. Replaces the mock-mode `admin-permission-matrix.spec.ts` (fixed row count of ten).
// Needs the stack in e2e/README.md; skips itself otherwise (and in mock mode).
import { expect, test } from "@playwright/test";
import { APP_URL, requireRealStack, sql, vi } from "./support/real-stack";
import { loginAsAdmin } from "./support/real-session";

requireRealStack();

const m = vi.adminPermissionMatrix;

function cell(
  page: Parameters<typeof loginAsAdmin>[0],
  action: keyof typeof m.action,
  fn: keyof typeof m.function,
  role: string,
) {
  return page.getByLabel(
    m.cellLabel
      .replace("{action}", m.action[action])
      .replace("{function}", m.function[fn])
      .replace("{role}", role),
  );
}

test("one row per function stored in the database, and no rejudge row", async ({
  page,
  request,
}) => {
  await loginAsAdmin(page, request, "pmx-rows");
  await page.goto(`${APP_URL}/admin/permissions`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.title);

  const stored = Number(sql("select count(*) from identity.functions"));
  expect(stored).toBeGreaterThan(0);
  const rows = page.getByRole("table").locator("tbody tr");
  await expect(rows).toHaveCount(stored, { timeout: 15_000 });

  // Every stored code is on screen as a row label, none of them is a rejudge function.
  const codes = sql("select code from identity.functions order by code").split(
    "\n",
  );
  expect(codes.some((code) => /REJUDGE/i.test(code))).toBe(false);
  for (const code of codes) {
    await expect(
      page.getByRole("table").getByText(code, { exact: true }),
    ).toBeVisible();
  }
  await expect(page.getByText(/REJUDGE/i)).toHaveCount(0);
});

test("INSTRUCTOR cells are editable and mirror the stored grant, STUDENT cells are read-only", async ({
  page,
  request,
}) => {
  await loginAsAdmin(page, request, "pmx-cells");
  await page.goto(`${APP_URL}/admin/permissions`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.title);

  const stored = sql(
    `select coalesce((select p.granted::text from identity.permissions p
       join identity.roles r on r.id = p.role_id
       join identity.functions f on f.id = p.function_id
       join identity.actions a on a.id = p.action_id
      where r.code = 'INSTRUCTOR' and f.code = 'USER_MANAGEMENT' and a.code = 'UPDATE'), 'false')`,
  );
  const instructorCell = cell(page, "update", "USER_MANAGEMENT", "Giảng viên");
  await expect(instructorCell).toBeEnabled({ timeout: 15_000 });
  if (stored === "true") await expect(instructorCell).toBeChecked();
  else await expect(instructorCell).not.toBeChecked();

  await page.getByRole("button", { name: "Học viên", exact: true }).click();
  await expect(
    page.getByText(m.studentNoticeTitle, { exact: true }),
  ).toBeVisible();
  const boxes = page.getByRole("table").getByRole("checkbox");
  const count = await boxes.count();
  expect(count).toBeGreaterThan(0);
  for (let i = 0; i < count; i++) await expect(boxes.nth(i)).toBeDisabled();
  // A read-only role offers no delete.
  await expect(page.getByText(m.systemRoleNote)).toBeVisible();
  await expect(page.getByRole("button", { name: m.deleteRole })).toHaveCount(0);
});

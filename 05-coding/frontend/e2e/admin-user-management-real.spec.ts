// ADM0201 (admin user management) against the REAL backend: combined filters and the result count.
// Replaces the mock-mode `admin-user-management.spec.ts` (fixed nine rows). Expected numbers come from the
// database or from accounts this test creates. Needs the stack in e2e/README.md; skips itself otherwise.
import { expect, test } from "@playwright/test";
import { pickFilter } from "./filter-menu";
import {
  APP_URL,
  createUser,
  requireRealStack,
  sql,
  vi,
} from "./support/real-stack";
import { loginAsAdmin } from "./support/real-session";

requireRealStack();

const m = vi.adminUserManagement;
const PAGE_SIZE = 20;
const count = (n: number) => n.toLocaleString("vi-VN");
const resultText = (shown: number, total: number) =>
  m.resultCount
    .replace("{shown}", String(shown))
    .replace("{total}", count(total));

test("role, status and search filters combine and the count equals the database", async ({
  page,
  request,
}) => {
  const run = `umg${Date.now().toString(36)}`;
  // One active instructor, one active student, one locked student, all carrying the run token.
  await createUser(request, { tag: `${run}-ins`, role: "INSTRUCTOR" });
  await createUser(request, { tag: `${run}-stu` });
  const locked = await createUser(request, { tag: `${run}-lock` });
  sql(
    `update identity.users set status='DEACTIVATED', deactivation_source='ADMIN', deactivated_at=now() where email='${locked.email}'`,
  );
  await loginAsAdmin(page, request, "umg-admin");
  await page.goto(`${APP_URL}/admin/users`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.title);

  const rows = page
    .getByRole("table", { name: m.tableCaption })
    .locator("tbody tr");
  const search = page.getByLabel(m.searchLabel);

  // Search alone: the three accounts (the admin created above has another tag).
  await search.fill(run);
  await expect(rows).toHaveCount(3, { timeout: 10_000 });
  await expect(page.getByText(resultText(3, 3))).toBeVisible();

  // Role filter narrows the same search to the instructor.
  await pickFilter(page, m.roleFilterLabel, m.role.instructor);
  await expect(rows).toHaveCount(1, { timeout: 10_000 });
  await expect(page.getByText(resultText(1, 1))).toBeVisible();

  // Student role plus status: active leaves one, locked leaves the other one.
  await pickFilter(page, m.roleFilterLabel, m.role.student);
  await pickFilter(page, m.statusFilterLabel, m.status.active);
  await expect(rows).toHaveCount(1, { timeout: 10_000 });
  await expect(rows.first()).toContainText(`${run}-stu`);
  await pickFilter(page, m.statusFilterLabel, m.status.locked);
  await expect(rows).toHaveCount(1, { timeout: 10_000 });
  await expect(rows.first()).toContainText(`${run}-lock`);

  // A combination that matches nothing says so instead of showing stale rows.
  await pickFilter(page, m.roleFilterLabel, m.role.instructor);
  // The empty message is itself one table row, so assert the message and the count, not zero rows.
  await expect(page.getByText(m.emptyFiltered)).toBeVisible({
    timeout: 10_000,
  });
  await expect(page.getByText(resultText(0, 0))).toBeVisible();
  await expect(rows.filter({ hasText: run })).toHaveCount(0);
});

test("without a search the role filter total equals the stored accounts of that base role", async ({
  page,
  request,
}) => {
  await loginAsAdmin(page, request, "umg-count");
  await page.goto(`${APP_URL}/admin/users`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.title);

  const stored = Number(
    sql(
      `select count(*) from identity.users u join identity.roles r on r.id = u.role_id where r.base_category = 'INSTRUCTOR'`,
    ),
  );
  expect(stored).toBeGreaterThan(0);
  await pickFilter(page, m.roleFilterLabel, m.role.instructor);
  await expect(
    page.getByText(resultText(Math.min(stored, PAGE_SIZE), stored)),
  ).toBeVisible({ timeout: 10_000 });
});

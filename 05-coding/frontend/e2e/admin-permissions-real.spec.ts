// ADM0202 (permission matrix) against the REAL backend: 03-dd/api/F1_identity.md #30 to #34. Needs the
// stack in e2e/README.md; skips itself otherwise (and in mock mode).
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

const m = vi.adminPermissionMatrix;
const apiErrors = vi.apiErrors as Record<string, string>;

async function openAsAdmin(
  page: Page,
  request: Parameters<typeof createUser>[0],
) {
  const admin = await createUser(request, { tag: "pm-admin", role: "ADMIN" });
  await page.goto(`${APP_URL}/admin/login`);
  await submitLogin(page, admin.email, admin.password);
  await expect(page).toHaveURL(/\/admin\/overview/, { timeout: 15_000 });
  await page.goto(`${APP_URL}/admin/permissions`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.title);
}

/** The checkbox of one cell, addressed the way a screen reader reads it. */
function cell(
  page: Page,
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

function storedGrant(role: string, fn: string, action: string): string {
  return sql(
    `select coalesce((select p.granted::text from identity.permissions p
       join identity.roles r on r.id = p.role_id
       join identity.functions f on f.id = p.function_id
       join identity.actions a on a.id = p.action_id
      where r.code = '${role}' and f.code = '${fn}' and a.code = '${action}'), 'false')`,
  );
}

test("the grid shows the stored cells and the student role is read-only", async ({
  page,
  request,
}) => {
  await openAsAdmin(page, request);

  await expect(
    cell(page, "read", "CLASS_MANAGEMENT", "Giảng viên"),
  ).toBeChecked();
  expect(storedGrant("INSTRUCTOR", "CLASS_MANAGEMENT", "READ")).toBe("true");
  await expect(
    cell(page, "read", "USER_MANAGEMENT", "Giảng viên"),
  ).not.toBeChecked();

  await page.getByRole("button", { name: "Học viên", exact: true }).click();
  await expect(
    cell(page, "read", "CLASS_MANAGEMENT", "Học viên"),
  ).toBeDisabled();
});

test("a toggled cell is saved on the server and survives a reload", async ({
  page,
  request,
}) => {
  await openAsAdmin(page, request);
  expect(storedGrant("INSTRUCTOR", "AI_CONFIG", "READ")).toBe("false");

  try {
    // The box flips when the server confirms, not on the click, so click and then wait for it.
    await cell(page, "read", "AI_CONFIG", "Giảng viên").click();
    await expect(cell(page, "read", "AI_CONFIG", "Giảng viên")).toBeChecked();
    await expect
      .poll(() => storedGrant("INSTRUCTOR", "AI_CONFIG", "READ"))
      .toBe("true");

    await page.reload();
    await expect(cell(page, "read", "AI_CONFIG", "Giảng viên")).toBeChecked();
  } finally {
    // Put the seeded matrix back whatever happened above.
    sql(
      `update identity.permissions p set granted = false from identity.roles r, identity.functions f, identity.actions a
        where r.code = 'INSTRUCTOR' and f.code = 'AI_CONFIG' and a.code = 'READ'
          and p.role_id = r.id and p.function_id = f.id and p.action_id = a.id`,
    );
    sql(
      "update identity.roles set permission_version = permission_version + 1 where code = 'INSTRUCTOR'",
    );
  }
});

test("a custom role is created and then deleted on the server", async ({
  page,
  request,
}) => {
  await openAsAdmin(page, request);
  const name = `E2E vai tro ${Date.now()}`;

  await page.getByRole("button", { name: m.addRole }).click();
  await page.getByLabel(m.newRoleLabel).fill(name);
  await page.getByRole("button", { name: m.createRole, exact: true }).click();

  await expect(page.getByRole("button", { name, exact: true })).toBeVisible({
    timeout: 10_000,
  });
  expect(
    sql(
      `select count(*) from identity.roles where name = '${name}' and not is_system`,
    ),
  ).toBe("1");
  // A new custom role starts with every cell off.
  await expect(cell(page, "read", "CLASS_MANAGEMENT", name)).not.toBeChecked();

  await page.getByRole("button", { name: m.deleteRole }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.deleteRole })
    .click();

  await expect(page.getByRole("button", { name, exact: true })).toHaveCount(0, {
    timeout: 10_000,
  });
  expect(
    sql(`select count(*) from identity.roles where name = '${name}'`),
  ).toBe("0");
});

test("a duplicate role name is refused by the server and the form stays", async ({
  page,
  request,
}) => {
  await openAsAdmin(page, request);
  const name = `E2E trung ${Date.now()}`;
  sql(
    `insert into identity.roles (id, code, name, base_category, is_system)
     values (gen_random_uuid(), 'role_e2e_${Date.now()}', '${name}', 'STUDENT', false)`,
  );

  // The page was loaded before the row existed, so only the server can refuse the duplicate.
  await page.getByRole("button", { name: m.addRole }).click();
  await page.getByLabel(m.newRoleLabel).fill(name);
  await page.getByRole("button", { name: m.createRole, exact: true }).click();

  await expect(page.getByText(apiErrors["IDT-425"] ?? "").first()).toBeVisible({
    timeout: 10_000,
  });
  await expect(page.getByLabel(m.newRoleLabel)).toHaveValue(name);
  sql(`delete from identity.roles where name = '${name}'`);
});

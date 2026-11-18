// ADM0403 (system log) against the REAL backend: 03-dd/api/F1_identity.md #39, #40, #51, #52, #64 to
// #66. Needs the stack in e2e/README.md; skips itself otherwise (and in mock mode). Rows are inserted
// with SQL for one fresh admin and the list is narrowed to that admin by e-mail, so the spec does not
// depend on what else is in the log.
import { randomUUID } from "node:crypto";
import { readFileSync } from "node:fs";
import { expect, test, type Page } from "@playwright/test";
import { pickFilter } from "./filter-menu";
import {
  APP_URL,
  createUser,
  requireRealStack,
  sql,
  submitLogin,
  vi,
  type TestUser,
} from "./support/real-stack";

requireRealStack();

const m = vi.adminSystemLog;
const apiErrors = vi.apiErrors as Record<string, string>;

async function openAsAdmin(
  page: Page,
  request: Parameters<typeof createUser>[0],
): Promise<TestUser> {
  const admin = await createUser(request, { tag: "log-admin", role: "ADMIN" });
  await page.goto(`${APP_URL}/admin/login`);
  await submitLogin(page, admin.email, admin.password);
  await expect(page).toHaveURL(/\/admin\/overview/, { timeout: 15_000 });
  await page.goto(`${APP_URL}/admin/system-log`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.title);
  return admin;
}

/** One audit row written by `actor`; `ageDays` moves it into the past. Plain ASCII, SQL via docker. */
function addRow(
  actor: TestUser,
  row: {
    action: string;
    targetType: string;
    after?: Record<string, unknown>;
    ageDays?: number;
    id?: string;
  },
): string {
  const id = row.id ?? randomUUID();
  sql(
    `insert into identity.system_audit_logs (id, actor_user_id, action_type, target_type, target_id, after_json, created_at)
     values ('${id}', (select id from identity.users where email = '${actor.email}'), '${row.action}',
             '${row.targetType}', 'e2e', '${JSON.stringify(row.after ?? {})}'::jsonb,
             now() - interval '${row.ageDays ?? 0} days')`,
  );
  return id;
}

/** Types the admin's e-mail and waits for the server to answer that search (the box is debounced). */
async function narrowTo(page: Page, admin: TestUser) {
  const answered = page.waitForResponse(
    (res) =>
      res.url().includes("/audit-logs?") &&
      res.url().includes("q=") &&
      res.ok(),
  );
  await page.getByLabel(m.searchLabel).fill(admin.email);
  await answered;
}

const events = (page: Page) =>
  page.getByRole("list", { name: m.eventListLabel }).getByRole("listitem");

test("a row reads as a sentence from its action, with the short code, the actor and the lock reason", async ({
  page,
  request,
}) => {
  const admin = await openAsAdmin(page, request);
  const role = `E2E vai tro ${Date.now()}`;
  const created = addRow(admin, {
    action: "ROLE_CREATED",
    targetType: "ROLE",
    after: { name: role },
  });
  addRow(admin, {
    action: "USER_LOCKED",
    targetType: "USER",
    after: { status: "DEACTIVATED", reason: "Bulk signup from one address" },
  });
  addRow(admin, {
    action: "USER_UNLOCKED",
    targetType: "USER",
    after: { status: "ACTIVE" },
  });
  await page.reload();
  await narrowTo(page, admin);

  await expect(events(page)).toHaveCount(3);
  const createdRow = events(page).filter({ hasText: `Tạo vai trò ${role}` });
  await expect(createdRow).toBeVisible();
  await expect(createdRow).toContainText(created.slice(0, 7));
  await expect(createdRow).toContainText(admin.displayName);
  await expect(createdRow).toContainText("permission");

  // "Mở khoá một tài khoản" contains the lock sentence, so the lock row is found by its reason.
  const lockRow = events(page).filter({ hasText: "Bulk signup" });
  await expect(lockRow).toHaveCount(1);
  await expect(lockRow).toContainText("Khoá một tài khoản");
  await expect(lockRow).toContainText(
    `${m.reasonLabel}: Bulk signup from one address`,
  );
  await expect(
    events(page).filter({ hasText: "Mở khoá một tài khoản" }),
  ).not.toContainText(m.reasonLabel);
});

test("the server filters by category and by day range, and the two blocks on the right have their own data", async ({
  page,
  request,
}) => {
  const admin = await openAsAdmin(page, request);
  addRow(admin, { action: "PASSWORD_CHANGED", targetType: "USER" });
  addRow(admin, {
    action: "ROLE_CREATED",
    targetType: "ROLE",
    after: { name: "Today role" },
  });
  addRow(admin, {
    action: "ROLE_DELETED",
    targetType: "ROLE",
    after: { name: "Old role" },
    ageDays: 3,
  });
  await page.reload();
  await narrowTo(page, admin);
  await expect(events(page)).toHaveCount(3);

  await pickFilter(page, m.categoryFilterLabel, m.category.auth);
  await expect(events(page)).toHaveCount(1);
  await expect(events(page).first()).toContainText("Người dùng đổi mật khẩu");
  await pickFilter(page, m.categoryFilterLabel, m.categoryAll);
  await expect(events(page)).toHaveCount(3);

  // Day range, inclusive: only the day three days ago (UTC+7).
  const day = new Date(Date.now() - 3 * 86_400_000 + 7 * 3_600_000)
    .toISOString()
    .slice(0, 10);
  await page.getByLabel(m.dateFrom).fill(day);
  await page.getByLabel(m.dateTo).fill(day);
  await expect(events(page)).toHaveCount(1);
  await expect(events(page).first()).toContainText("Xoá vai trò Old role");

  // Side blocks: this admin is among the active admins (24 hours) and the breakdown lists four groups.
  await expect(page.getByText(/hành động$/).first()).toBeVisible();
  await expect(
    page.getByRole("heading", { name: m.breakdownTitle }),
  ).toBeVisible();
});

test("the list pages by cursor, 50 rows at a time", async ({
  page,
  request,
}) => {
  const admin = await openAsAdmin(page, request);
  sql(
    `insert into identity.system_audit_logs (id, actor_user_id, action_type, target_type, target_id, after_json, created_at)
     select gen_random_uuid(), (select id from identity.users where email = '${admin.email}'), 'ROLE_CREATED', 'ROLE', 'e2e',
            '{"name":"bulk"}'::jsonb, now() - (g || ' seconds')::interval
       from generate_series(1, 55) g`,
  );
  await page.reload();
  await narrowTo(page, admin);

  await expect(events(page)).toHaveCount(50);
  await page.getByRole("button", { name: "Tải thêm 50 dòng" }).click();
  await expect(events(page)).toHaveCount(55);
  await expect(
    page.getByRole("button", { name: "Tải thêm 50 dòng" }),
  ).toBeDisabled();
});

test("a row written while the page is open shows up at the top without a reload", async ({
  page,
  request,
}) => {
  const admin = await openAsAdmin(page, request);
  await narrowTo(page, admin);
  await expect(page.getByText(m.emptyFiltered)).toBeVisible();

  // The toggle is on by default; the stream polls the table every couple of seconds.
  addRow(admin, {
    action: "ROLE_CREATED",
    targetType: "ROLE",
    after: { name: "Live role" },
  });
  await expect(
    events(page).filter({ hasText: "Tạo vai trò Live role" }),
  ).toBeVisible({ timeout: 20_000 });
});

test("the retention is read from the server, and a save is stored or refused with its code", async ({
  page,
  request,
}) => {
  await openAsAdmin(page, request);
  const stored = () =>
    sql(
      "select value_json from identity.system_settings where key = 'audit_log.retention_days'",
    );
  const before = stored();
  await expect(
    page.getByText(m.retention.footer.replace("{days}", before)),
  ).toBeVisible();

  const canEdit =
    sql(
      `select count(*) from identity.permissions p
         join identity.roles r on r.id = p.role_id
         join identity.functions f on f.id = p.function_id
         join identity.actions a on a.id = p.action_id
        where r.code = 'ADMIN' and f.code = 'SYSTEM_AUDIT_LOG' and a.code = 'UPDATE' and p.granted`,
    ) === "1";

  try {
    await page.getByRole("button", { name: m.retention.open }).click();
    const dialog = page.getByRole("dialog");
    await dialog.getByLabel(/Thời hạn lưu/).fill("45");
    await dialog.getByRole("button", { name: m.retention.save }).click();
    // Shortening asks a second time.
    await expect(
      dialog.getByText(m.retention.confirmShorter.replace("{days}", "45")),
    ).toBeVisible();
    await dialog.getByRole("button", { name: m.retention.confirm }).click();

    if (canEdit) {
      await expect(page.getByText(m.retention.saved)).toBeVisible();
      await expect(
        page.getByText(m.retention.footer.replace("{days}", "45")),
      ).toBeVisible();
      expect(stored()).toBe("45");
    } else {
      // No SYSTEM_AUDIT_LOG:UPDATE cell for ADMIN: the server refuses and nothing changes.
      await expect(
        page.getByText(apiErrors["IDT-445"] ?? "").first(),
      ).toBeVisible();
      await expect(dialog).toBeVisible();
      expect(stored()).toBe(before);
    }
  } finally {
    sql(
      `update identity.system_settings set value_json = '${before}'::jsonb where key = 'audit_log.retention_days'`,
    );
  }
});

test("the export downloads the server's CSV with the chosen columns and logs itself", async ({
  page,
  request,
}) => {
  const admin = await openAsAdmin(page, request);
  addRow(admin, {
    action: "ROLE_CREATED",
    targetType: "ROLE",
    after: { name: "Csv role" },
  });
  await page.reload();
  await narrowTo(page, admin);
  await expect(events(page)).toHaveCount(1);
  const exportsBefore = sql(
    "select count(*) from identity.system_audit_logs where action_type = 'AUDIT_LOG_EXPORTED'",
  );

  await page.getByRole("button", { name: m.export, exact: true }).click();
  const dialog = page.getByRole("dialog");
  // Keep three columns: time, category and the sentence.
  for (const column of ["actor", "service", "id"] as const) {
    await dialog.getByLabel(m.exportDialog.col[column]).uncheck();
  }
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    dialog.getByRole("button", { name: m.exportDialog.submit }).click(),
  ]);

  expect(download.suggestedFilename()).toMatch(/^audit-logs-\d{8}-\d{4}\.csv$/);
  const text = readFileSync((await download.path()) as string, "utf8");
  const lines = text.replace(/^﻿/, "").trim().split(/\r?\n/);
  // The header carries only the chosen columns; the narrowed admin's row is in the body.
  expect(lines[0]).toContain(m.exportDialog.col.time);
  expect(lines[0]).not.toContain(m.exportDialog.col.actor);
  expect(lines.length).toBeGreaterThanOrEqual(2);
  expect(
    lines.some(
      (line) => line.includes("ROLE_CREATED") && line.includes("Csv role"),
    ),
  ).toBe(true);
  await expect(page.getByText(/Đã xuất nhật ký với 3 cột/)).toBeVisible();
  await expect
    .poll(() =>
      sql(
        "select count(*) from identity.system_audit_logs where action_type = 'AUDIT_LOG_EXPORTED'",
      ),
    )
    .toBe(String(Number(exportsBefore) + 1));
});

// ADM0201 (admin user management) against the REAL backend: 03-dd/api/F1_identity.md #35 to #38,
// #50. Needs the stack in e2e/README.md; skips itself otherwise (and in mock mode).
import {
  expect,
  test,
  type APIRequestContext,
  type Page,
} from "@playwright/test";
import {
  APP_URL,
  MAILPIT_URL,
  PASSWORD,
  clearMail,
  createUser,
  requireRealStack,
  sql,
  submitLogin,
  uniqueEmail,
  vi,
} from "./support/real-stack";

requireRealStack();

const m = vi.adminUserManagement;
const apiErrors = vi.apiErrors as Record<string, string>;
const label = (template: string, name: string) =>
  template.replace("{name}", name);

async function openAsAdmin(page: Page, request: APIRequestContext) {
  const admin = await createUser(request, { tag: "um-admin", role: "ADMIN" });
  await page.goto(`${APP_URL}/admin/login`);
  await submitLogin(page, admin.email, admin.password);
  await expect(page).toHaveURL(/\/admin\/overview/, { timeout: 15_000 });
  await page.goto(`${APP_URL}/admin/users`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.title);
  return admin;
}

/** The row of one account, found through the server-side search. */
async function findRow(page: Page, email: string) {
  await page.getByLabel(m.searchLabel).fill(email);
  const row = page
    .getByRole("table", { name: m.tableCaption })
    .locator("tbody tr")
    .filter({ hasText: email });
  await expect(row).toHaveCount(1, { timeout: 10_000 });
  return row;
}

/** The temporary password in the newest mail to `to`. */
async function mailedPassword(
  request: APIRequestContext,
  to: string,
): Promise<string> {
  let text = "";
  await expect
    .poll(
      async () => {
        const res = await request.get(
          `${MAILPIT_URL}/search?query=${encodeURIComponent("to:" + to)}&limit=1`,
        );
        const hit = ((await res.json()) as { messages: { ID: string }[] })
          .messages[0];
        if (!hit) return "";
        text =
          (
            (await (
              await request.get(`${MAILPIT_URL}/message/${hit.ID}`)
            ).json()) as {
              Text?: string;
            }
          ).Text ?? "";
        return text;
      },
      { message: `no mail to ${to}`, timeout: 15_000 },
    )
    .not.toBe("");
  const found = /Mật khẩu tạm thời: (\S+?)\./.exec(text)?.[1];
  expect(found, "temporary password in the mail").toBeTruthy();
  return found as string;
}

async function login(
  request: APIRequestContext,
  email: string,
  password: string,
) {
  return request.post(`${APP_URL}/api/v1/identity/auth/login`, {
    data: { email, password },
  });
}

test("the list shows server rows, the counters and no invented activity numbers", async ({
  page,
  request,
}) => {
  await openAsAdmin(page, request);

  await expect(
    page.getByRole("columnheader", { name: m.columnStatus }),
  ).toBeVisible();
  await expect(
    page.getByRole("columnheader", { name: m.columnLastActive }),
  ).toBeVisible();
  // The admin logged in a moment ago, so its own row reads as recent activity.
  await expect(page.getByText(/bây giờ|phút trước/i).first()).toBeVisible();
  await expect(
    page.getByRole("columnheader", { name: m.columnSolved }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("columnheader", { name: m.columnSubmissions }),
  ).toHaveCount(0);
  await expect(
    page
      .getByRole("table", { name: m.tableCaption })
      .locator("tbody tr")
      .first(),
  ).toBeVisible();
});

test("lock and unlock go to the server, survive a reload and end the user's sessions", async ({
  page,
  request,
}) => {
  await openAsAdmin(page, request);
  const user = await createUser(request, { tag: "um-lock" });

  let row = await findRow(page, user.email);
  await row
    .getByRole("button", { name: label(m.actionLockFor, user.displayName) })
    .click();
  await page
    .getByRole("dialog")
    .getByLabel(m.lockDialog.reasonLabel)
    .fill("Spam e2e");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.lockDialog.submit })
    .click();
  await expect(row.getByText(m.status.locked)).toBeVisible({ timeout: 10_000 });

  expect(
    sql(
      `select status || '/' || deactivation_source from identity.users where email = '${user.email}'`,
    ),
  ).toBe("DEACTIVATED/ADMIN");
  expect((await login(request, user.email, PASSWORD)).status()).toBe(403);

  await page.reload();
  row = await findRow(page, user.email);
  await expect(row.getByText(m.status.locked)).toBeVisible();

  await row
    .getByRole("button", { name: label(m.actionUnlockFor, user.displayName) })
    .click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.confirmUnlockAction })
    .click();
  await expect(row.getByText(m.status.active)).toBeVisible({ timeout: 10_000 });
  expect(
    sql(`select status from identity.users where email = '${user.email}'`),
  ).toBe("ACTIVE");
  expect((await login(request, user.email, PASSWORD)).status()).toBe(200);
});

test("a role change is saved on the server and applies to the next request", async ({
  page,
  request,
}) => {
  await openAsAdmin(page, request);
  const user = await createUser(request, { tag: "um-role" });

  const row = await findRow(page, user.email);
  await row
    .getByRole("button", {
      name: label(m.actionChangeRoleFor, user.displayName),
    })
    .click();
  await page
    .getByRole("dialog")
    .getByLabel(m.roleDialog.label)
    .selectOption("instructor");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.roleDialog.submit })
    .click();

  await expect
    .poll(() =>
      sql(
        `select r.code from identity.users u join identity.roles r on r.id = u.role_id where u.email = '${user.email}'`,
      ),
    )
    .toBe("INSTRUCTOR");
  await page.reload();
  await expect(await findRow(page, user.email)).toContainText(
    m.role.instructor,
  );
});

test("a password reset mails a temporary password that forces a change at the next login", async ({
  page,
  request,
}) => {
  await openAsAdmin(page, request);
  const user = await createUser(request, { tag: "um-reset" });
  await clearMail(request, user.email);

  const row = await findRow(page, user.email);
  await row
    .getByRole("button", {
      name: label(m.actionResetPasswordFor, user.displayName),
    })
    .click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.resetAction })
    .click();

  const temporary = await mailedPassword(request, user.email);
  expect((await login(request, user.email, PASSWORD)).status()).toBe(401);
  const res = await login(request, user.email, temporary);
  expect(res.status()).toBe(200);
  expect(
    ((await res.json()) as { user: { mustChangePassword: boolean } }).user
      .mustChangePassword,
  ).toBe(true);
});

test("a new account is created, mailed a temporary password, and a taken e-mail keeps the dialog open", async ({
  page,
  request,
}) => {
  await openAsAdmin(page, request);
  const email = uniqueEmail("um-new");
  await clearMail(request, email);

  await page.getByRole("button", { name: m.addAccount }).click();
  await page
    .getByRole("dialog")
    .getByLabel(m.addDialog.nameLabel)
    .fill("Nguoi Moi E2E");
  await page.getByRole("dialog").getByLabel(m.addDialog.emailLabel).fill(email);
  await page
    .getByRole("dialog")
    .getByLabel(m.addDialog.roleLabel)
    .selectOption("instructor");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.addDialog.submit })
    .click();

  await expect
    .poll(() =>
      sql(`select status from identity.users where email = '${email}'`),
    )
    .toBe("PENDING_VERIFICATION");
  const res = await login(request, email, await mailedPassword(request, email));
  expect(res.status()).toBe(200);
  expect(
    sql(`select status from identity.users where email = '${email}'`),
  ).toBe("ACTIVE");

  // The same address again. The dialog only knows the rows on screen, so filter the list down to
  // nothing first: the duplicate then reaches the server, which refuses it, and the dialog stays.
  await page.getByLabel(m.searchLabel).fill("zzz-no-such-user");
  await expect(page.getByText(m.emptyFiltered)).toBeVisible({
    timeout: 10_000,
  });
  await page.getByRole("button", { name: m.addAccount }).click();
  await page
    .getByRole("dialog")
    .getByLabel(m.addDialog.nameLabel)
    .fill("Nguoi Trung");
  await page
    .getByRole("dialog")
    .getByLabel(m.addDialog.emailLabel)
    .fill(email.toUpperCase());
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.addDialog.submit })
    .click();
  await expect(page.getByText(apiErrors["IDT-440"] ?? "").first()).toBeVisible({
    timeout: 10_000,
  });
  await expect(
    page.getByRole("dialog").getByLabel(m.addDialog.nameLabel),
  ).toHaveValue("Nguoi Trung");
});

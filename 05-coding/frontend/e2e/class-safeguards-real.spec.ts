// Class safeguards of 2026-11-26 against the REAL backend (F1 #20, #21, #25, #26, #78 and the delete notice of
// INS0201), plus the SHR0201 changes of the same week (Excel export, side blocks removed, import hint).
// Needs the stack in e2e/README.md; skips itself otherwise. Problems are seeded by SQL and flagged deleted
// afterwards; accounts and classes stay, so use a demo database.
import { readFile } from "node:fs/promises";
import {
  expect,
  test,
  type APIRequestContext,
  type Browser,
  type Locator,
  type Page,
} from "@playwright/test";
import { loginAsAdmin, userId } from "./support/real-session";
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

const m = vi.classManagement;
const d = vi.classDetail;
const mine = vi.myClasses;
const um = vi.adminUserManagement;
const pm = vi.problemManagement;
const pi = vi.problemImport;
const apiErrors = vi.apiErrors as Record<string, string>;
const API = `${APP_URL}/api/v1`;
const LEVEL = "20000000-0000-4000-8000-000000000001";
const fill = (text: string, values: Record<string, string | number>) =>
  text.replace(/\{(\w+)\}/g, (_, key: string) => String(values[key]));

const seeded: string[] = [];
test.afterAll(() => {
  for (const code of seeded)
    sql(
      `update problem.problems set status='UNPUBLISHED', deleted=true where code='${code}'`,
    );
});

/** Screenshot with a red outline on the element the step is about; the outline is removed afterwards. */
async function shot(page: Page, name: string, mark?: Locator) {
  await mark?.evaluate((el: HTMLElement) => {
    el.style.outline = "3px solid #e11d48";
    el.style.outlineOffset = "2px";
  });
  const path = test.info().outputPath(`${name}.png`);
  await page.screenshot({ path });
  await test.info().attach(name, { path, contentType: "image/png" });
  await mark
    ?.evaluate((el: HTMLElement) => {
      el.style.outline = "";
    })
    .catch(() => {});
}

function seedProblem(authorId: string, code: string) {
  sql(
    `insert into problem.problems(id,code,title,statement_md,author_id,level_id,status,published_at,time_limit_ms,memory_limit_mb,max_output_size_kb,function_wrapper_supported)
     values(gen_random_uuid(),'${code}','${code}','Read two integers, print the sum.','${authorId}','${LEVEL}','PUBLISHED',now(),1000,256,64,false)`,
  );
  seeded.push(code);
  return sql(`select id from problem.problems where code='${code}'`);
}

async function signInInstructor(page: Page, request: APIRequestContext) {
  const user = await createUser(request, {
    tag: "safe-inst",
    role: "INSTRUCTOR",
  });
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/\/instructor\/overview/, { timeout: 30_000 });
  return user;
}

async function createClassViaUi(page: Page, name: string) {
  await page.goto(`${APP_URL}/instructor/classes`);
  await page.getByRole("button", { name: m.header.btnCreateClass }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel(new RegExp(m.popup.classNameLabel)).fill(name);
  await dialog.getByRole("button", { name: m.popup.saveClass }).click();
  await expect(page).toHaveURL(/\/instructor\/classes\/[0-9a-f-]{36}$/, {
    timeout: 15_000,
  });
  const id = /\/classes\/([0-9a-f-]{36})$/.exec(page.url())![1]!;
  await expect(page.locator("li .font-mono")).toHaveCount(1);
  return id;
}

const codeOnPage = async (page: Page) =>
  (await page.locator("li .font-mono").first().innerText()).trim();

/** A new learner signed in in their own browser context, on "Lớp của tôi". */
async function learnerOnClasses(browser: Browser, request: APIRequestContext) {
  const learner: TestUser = await createUser(request, { tag: "safe-learner" });
  const context = await browser.newContext();
  const page = await context.newPage();
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, learner.email, learner.password);
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 });
  await page.goto(`${APP_URL}/classes`);
  return { learner, page, context };
}

async function join(page: Page, code: string) {
  await page.getByLabel(mine.join.label).fill(code);
  await page.getByRole("button", { name: mine.join.submit }).click();
}

/** The class on "Lớp của tôi": one table row whose name links to the class page. */
const joined = (page: Page, name: string) =>
  page.getByRole("link", {
    name: fill(mine.card.viewDetailOf, { name }),
  });

const members = (classId: string) =>
  sql(
    `select count(*) from identity.class_enrollments where class_id='${classId}' and left_at is null`,
  );

/** The admin locks or unlocks one account on ADM0201. */
async function setLocked(admin: Page, user: TestUser, locked: boolean) {
  await admin.goto(`${APP_URL}/admin/users`);
  await admin.getByLabel(um.searchLabel).fill(user.email);
  const row = admin
    .getByRole("table", { name: um.tableCaption })
    .locator("tbody tr")
    .filter({ hasText: user.email });
  await expect(row).toHaveCount(1, { timeout: 10_000 });
  const action = locked ? um.actionLockFor : um.actionUnlockFor;
  await row
    .getByRole("button", { name: fill(action, { name: user.displayName }) })
    .click();
  const dialog = admin.getByRole("dialog");
  if (locked)
    await dialog.getByLabel(um.lockDialog.reasonLabel).fill("e2e khoa tam");
  await dialog
    .getByRole("button", {
      name: locked ? um.lockDialog.submit : um.confirmUnlockAction,
    })
    .click();
  await expect(
    row.getByText(locked ? um.status.locked : um.status.active),
  ).toBeVisible({ timeout: 10_000 });
}

test("buoc 1-4 va 6: ten lop trung, thu hoi ma moi, hop thoai xoa lop va xoa that", async ({
  browser,
  page,
  request,
}) => {
  test.setTimeout(3 * 60_000);
  const by = await signInInstructor(page, request);
  const name = `E2E Lop test ${Date.now()}`;
  const id = await createClassViaUi(page, name);
  await shot(page, "01-lop-moi", page.locator("li .font-mono").first());

  // 2: same name with other case and outer spaces.
  await page.goto(`${APP_URL}/instructor/classes`);
  await page.getByRole("button", { name: m.header.btnCreateClass }).click();
  const dialog = page.getByRole("dialog");
  await dialog
    .getByLabel(new RegExp(m.popup.classNameLabel))
    .fill(`  ${name.toUpperCase()} `);
  await dialog.getByRole("button", { name: m.popup.saveClass }).click();
  const clash = page.getByText(apiErrors["IDT-417"] ?? "missing").first();
  await expect(clash).toBeVisible();
  await expect(dialog).toBeVisible();
  await shot(page, "02-ten-trung", clash);
  expect(
    sql(
      `select count(*) from identity.classes where lower(btrim(name))=lower('${name}')`,
    ),
  ).toBe("1");

  // 3: a second code, then the first one is revoked (cancel first).
  await page.goto(`${APP_URL}/instructor/classes/${id}`);
  await expect(page.locator("li .font-mono")).toHaveCount(1);
  const leaked = await codeOnPage(page);
  await page.getByRole("button", { name: d.invite.create }).click();
  await expect(page.locator("li .font-mono")).toHaveCount(2);
  const kept = sql(
    `select code from identity.class_invite_codes where class_id='${id}' and code<>'${leaked}'`,
  );
  const revoke = page.getByRole("button", {
    name: fill(d.invite.revokeCode, { code: leaked }),
  });
  await revoke.click();
  await shot(page, "03a-xac-nhan-thu-hoi", page.getByRole("dialog"));
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.cancel })
    .click();
  await expect(revoke).toBeVisible();
  await revoke.click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: d.invite.revokeConfirm })
    .click();
  await expect(revoke).toHaveCount(0);
  await expect(page.locator("li .font-mono")).toHaveCount(1);
  await shot(page, "03b-sau-thu-hoi", page.locator("li .font-mono").first());
  expect(
    sql(
      `select string_agg(code, ',') from identity.class_invite_codes where class_id='${id}'`,
    ),
  ).toBe(kept);

  // 4: the learner is refused with the revoked code and joins with the other.
  const one = await learnerOnClasses(browser, request);
  try {
    await join(one.page, leaked);
    const refused = one.page.getByText(apiErrors["IDT-410"] ?? "").first();
    await expect(refused).toBeVisible();
    await shot(one.page, "04a-ma-da-thu-hoi", refused);
    expect(members(id)).toBe("0");
    await join(one.page, kept);
    await expect(joined(one.page, name)).toBeVisible({ timeout: 10_000 });
    await shot(one.page, "04b-vao-duoc-lop", joined(one.page, name));
    expect(members(id)).toBe("1");

    // 6: two assigned problems, the delete dialog names both figures, then the class is really deleted.
    const author = userId(by.email);
    const stamp = Date.now();
    const problems = [1, 2].map((n) =>
      seedProblem(author, `E2E-SAFE-${stamp}-${n}`),
    );
    const login = await request.post(`${API}/identity/auth/login`, {
      data: { email: by.email, password: by.password },
    });
    const assigned = await request.post(
      `${API}/problem-bank/instructor/classes/${id}/assignments`,
      {
        headers: {
          Authorization: `Bearer ${(await login.json()).accessToken}`,
        },
        data: { problemIds: problems },
      },
    );
    expect(assigned.ok(), "assign").toBe(true);
    await page.goto(`${APP_URL}/instructor/classes`);
    await page.getByRole("button", { name: `Xoá lớp ${name}` }).click();
    const impact = page
      .getByRole("dialog")
      .getByText(fill(m.popup.deleteClassImpact, { students: 1, problems: 2 }));
    await expect(impact).toBeVisible({ timeout: 15_000 });
    await shot(page, "06a-hop-thoai-xoa", impact);
    await page
      .getByRole("dialog")
      .getByRole("button", { name: m.popup.deleteClassConfirm })
      .click();
    await expect(page.getByRole("cell", { name })).toHaveCount(0);
    expect(sql(`select count(*) from identity.classes where id='${id}'`)).toBe(
      "0",
    );
    // F2 withdraws the assignments from the ClassDeleted event.
    await expect
      .poll(
        () =>
          sql(
            `select count(*) from problem.class_assignments where class_id='${id}' and removed_at is null`,
          ),
        { timeout: 30_000 },
      )
      .toBe("0");
    await one.page.goto(`${APP_URL}/classes`);
    await expect(one.page.getByText(mine.empty)).toBeVisible();
    await shot(one.page, "06b-hoc-vien-khong-con-lop");
  } finally {
    await one.context.close();
  }
});

test("buoc 7: giang vien bi khoa thi ma moi khong dung duoc, mo khoa thi dung lai", async ({
  browser,
  page,
  request,
}) => {
  test.setTimeout(3 * 60_000);
  const by = await signInInstructor(page, request);
  const name = `E2E Lop khoa ${Date.now()}`;
  const id = await createClassViaUi(page, name);
  const code = await codeOnPage(page);

  const early = await learnerOnClasses(browser, request);
  const late = await learnerOnClasses(browser, request);
  const adminContext = await browser.newContext();
  const admin = await adminContext.newPage();
  try {
    await join(early.page, code);
    await expect(joined(early.page, name)).toBeVisible();
    await loginAsAdmin(admin, request, "safe-admin");

    await setLocked(admin, by, true);
    await shot(admin, "07a-admin-khoa-giang-vien");
    await join(late.page, code);
    const refused = late.page.getByText(apiErrors["IDT-410"] ?? "").first();
    await expect(refused).toBeVisible();
    await shot(late.page, "07b-ma-bi-tu-choi", refused);
    expect(members(id), "the early member stays, nobody new").toBe("1");

    await setLocked(admin, by, false);
    await join(late.page, code);
    await expect(joined(late.page, name)).toBeVisible({
      timeout: 10_000,
    });
    await shot(late.page, "07c-mo-khoa-vao-duoc");
    expect(members(id)).toBe("2");
  } finally {
    await early.context.close();
    await late.context.close();
    await adminContext.close();
  }
});

test("buoc 8: hoc vien bi khoa co nhan trong danh sach lop, si so giu nguyen", async ({
  browser,
  page,
  request,
}) => {
  test.setTimeout(3 * 60_000);
  await signInInstructor(page, request);
  const id = await createClassViaUi(page, `E2E Lop nhan ${Date.now()}`);
  const code = await codeOnPage(page);
  const gone = await learnerOnClasses(browser, request);
  const stays = await learnerOnClasses(browser, request);
  const adminContext = await browser.newContext();
  const admin = await adminContext.newPage();
  try {
    for (const one of [gone, stays]) {
      await join(one.page, code);
      await expect(one.page.getByText(mine.empty)).toHaveCount(0);
      await expect.poll(() => members(id)).not.toBe("0");
    }
    await expect.poll(() => members(id)).toBe("2");
    await loginAsAdmin(admin, request, "safe-admin");
    await setLocked(admin, gone.learner, true);

    await page.goto(`${APP_URL}/instructor/classes/${id}`);
    const table = page.getByRole("table", { name: d.students.title });
    const lockedRow = table
      .locator("tbody tr")
      .filter({ hasText: gone.learner.email });
    const badge = lockedRow.getByText(d.students.inactive);
    await expect(badge).toBeVisible();
    await expect(
      table
        .locator("tbody tr")
        .filter({ hasText: stays.learner.email })
        .getByText(d.students.inactive),
    ).toHaveCount(0);
    await expect(
      page.getByText(fill(d.students.subtitle, { count: 2 })),
    ).toBeVisible();
    await badge.scrollIntoViewIfNeeded();
    await shot(page, "08a-nhan-tai-khoan", badge);

    await setLocked(admin, gone.learner, false);
    await page.reload();
    await expect(table.getByText(gone.learner.email)).toBeVisible();
    await expect(table.getByText(d.students.inactive)).toHaveCount(0);
  } finally {
    await gone.context.close();
    await stays.context.close();
    await adminContext.close();
  }
});

test("buoc 9: xuat Excel, hai khoi da go, tab Loi giai mau co chu thich", async ({
  page,
  request,
}) => {
  test.setTimeout(2 * 60_000);
  const by = await signInInstructor(page, request);
  seedProblem(userId(by.email), `E2E-XLSX-${Date.now()}`);
  await page.goto(`${APP_URL}/instructor/problems`);
  const table = page.getByRole("table", { name: pm.tableCaption });
  await expect(table.locator("tbody tr")).toHaveCount(1, { timeout: 15_000 });
  await expect(page.getByText("Phân bố theo chủ đề")).toHaveCount(0);
  await expect(page.getByText("Bài cần chú ý")).toHaveCount(0);
  await shot(page, "09a-quan-ly-bai-tap");

  const downloading = page.waitForEvent("download");
  await page.getByRole("button", { name: pm.exportFiltered }).click();
  const file = await downloading;
  expect(file.suggestedFilename()).toMatch(/^problems-\d{8}\.xlsx$/);
  const bytes = await readFile((await file.path())!);
  expect(bytes.subarray(0, 2).toString(), "a zip container").toBe("PK");
  await file.saveAs(test.info().outputPath("problems.xlsx"));

  await page.getByRole("link", { name: pm.importProblems }).click();
  await expect(page).toHaveURL(/\/instructor\/problems\/import$/);
  await page.getByRole("button", { name: pi.addRow }).first().click();
  const tab = page.getByRole("button", {
    name: pi.editor.solution,
    exact: true,
  });
  if (!(await tab.isVisible()))
    await page
      .locator("table tbody tr")
      .first()
      .locator("td")
      .nth(1)
      .getByRole("button")
      .click();
  await tab.click();
  const hint = page.getByText(pi.editor.solutionHint);
  await expect(hint).toBeVisible();
  await page
    .getByRole("button", { name: new RegExp(`^${pi.field.language}`) })
    .click();
  const options = page.getByRole("listbox").getByRole("option");
  await expect(options.filter({ hasText: /python/i })).toHaveCount(1);
  await shot(page, "09b-loi-giai-mau", hint);
});

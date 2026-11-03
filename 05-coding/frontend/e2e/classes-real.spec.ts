// The class cluster against the REAL backend: INS0201 and INS0205 (instructor, 03-dd/api/F1_identity.md
// #19 to #24, #26, #27) and USR0701 and USR0702 (learner, #25, #25A, #25B). Needs the stack in
// e2e/README.md; skips itself otherwise (and in mock mode).
import { expect, test, type Page } from "@playwright/test";
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
const apiErrors = vi.apiErrors as Record<string, string>;

async function instructorOn(
  page: Page,
  request: Parameters<typeof createUser>[0],
) {
  const user = await createUser(request, {
    tag: "cls-inst",
    role: "INSTRUCTOR",
  });
  await page.goto(`${APP_URL}/instructor/login`);
  await submitLogin(page, user.email, user.password);
  await expect(page).toHaveURL(/\/instructor\/overview/, { timeout: 15_000 });
  return user;
}

async function createClassViaUi(page: Page, name: string) {
  await page.goto(`${APP_URL}/instructor/classes`);
  await page.getByRole("button", { name: m.header.btnCreateClass }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel(new RegExp(m.popup.classNameLabel)).fill(name);
  await dialog
    .getByLabel(m.popup.descriptionLabel)
    .fill("Lop dung cho kiem thu");
  await dialog
    .getByRole("checkbox", { name: m.schedule.dayShort["1"] })
    .click();
  await dialog.getByLabel(m.schedule.startTime).fill("19:00");
  await dialog.getByLabel(m.schedule.endTime).fill("21:00");
  await dialog.getByRole("button", { name: m.popup.saveClass }).click();
  await expect(page).toHaveURL(/\/instructor\/classes\/[0-9a-f-]{36}$/, {
    timeout: 15_000,
  });
  return /\/classes\/([0-9a-f-]{36})$/.exec(page.url())![1]!;
}

const stored = (id: string, column: string) =>
  sql(`select ${column} from identity.classes where id = '${id}'`);

test("an instructor creates, edits and deletes a class on the server", async ({
  page,
  request,
}) => {
  await instructorOn(page, request);
  const name = `E2E lop ${Date.now()}`;

  const id = await createClassViaUi(page, name);
  await expect(page.getByRole("heading", { name })).toBeVisible();
  expect(stored(id, "name")).toBe(name);
  expect(stored(id, "schedule->>'startTime'")).toBe("19:00");
  // The first invite code was created with the class and is on the page.
  const codes = page.locator("li .font-mono");
  await expect(codes).toHaveCount(1);
  expect(
    sql(
      `select count(*) from identity.class_invite_codes where class_id = '${id}'`,
    ),
  ).toBe("1");

  // Another code is added next to the first one.
  await page.getByRole("button", { name: d.invite.create }).click();
  await expect(codes).toHaveCount(2);

  // Edit: the name changes on the server and the page.
  await page.getByRole("button", { name: d.edit }).click();
  const dialog = page.getByRole("dialog");
  await dialog
    .getByLabel(new RegExp(m.popup.classNameLabel))
    .fill(`${name} sua`);
  await dialog.getByRole("button", { name: m.popup.saveClass }).click();
  await expect(
    page.getByRole("heading", { name: `${name} sua` }),
  ).toBeVisible();
  expect(stored(id, "name")).toBe(`${name} sua`);

  // The list no longer shows the F4 columns, only what the server has.
  await page.goto(`${APP_URL}/instructor/classes`);
  const table = page.getByRole("table", { name: m.header.title });
  await expect(table.getByText(`${name} sua`)).toBeVisible();
  await expect(table.getByText(m.classList.col.completionBar)).toHaveCount(0);

  // Delete.
  await page.getByRole("button", { name: `Xoá lớp ${name} sua` }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.popup.deleteClassConfirm })
    .click();
  await expect(table.getByText(`${name} sua`)).toHaveCount(0);
  expect(sql(`select count(*) from identity.classes where id = '${id}'`)).toBe(
    "0",
  );
});

test("a learner joins with the code, the instructor sees and removes them, and the learner's list follows", async ({
  browser,
  page,
  request,
}) => {
  await instructorOn(page, request);
  const name = `E2E lop tham gia ${Date.now()}`;
  const id = await createClassViaUi(page, name);
  const code = (await page.locator("li .font-mono").first().innerText()).trim();

  const learnerContext = await browser.newContext();
  const learnerPage = await learnerContext.newPage();
  const learner: TestUser = await createUser(request, { tag: "cls-learner" });
  try {
    await learnerPage.goto(`${APP_URL}/login`);
    await submitLogin(learnerPage, learner.email, learner.password);
    await expect(learnerPage).not.toHaveURL(/\/login/, { timeout: 15_000 });
    await learnerPage.goto(`${APP_URL}/classes`);
    await expect(learnerPage.getByText(mine.empty)).toBeVisible();

    // A wrong code reads by its own error text; the right one joins.
    await learnerPage.getByLabel(mine.join.label).fill("WRONG234");
    await learnerPage.getByRole("button", { name: mine.join.submit }).click();
    await expect(
      learnerPage.getByText(apiErrors["IDT-410"] ?? "").first(),
    ).toBeVisible();

    await learnerPage.getByLabel(mine.join.label).fill(code.toLowerCase());
    await learnerPage.getByRole("button", { name: mine.join.submit }).click();
    await expect(learnerPage.getByRole("heading", { name })).toBeVisible({
      timeout: 10_000,
    });
    expect(
      sql(
        `select count(*) from identity.class_enrollments e join identity.users u on u.id = e.student_id
          where e.class_id = '${id}' and u.email = '${learner.email}' and e.left_at is null`,
      ),
    ).toBe("1");

    // The learner's detail page: from its own call, with no assignment block.
    await learnerPage.goto(`${APP_URL}/classes/${id}`);
    await expect(learnerPage.getByRole("heading", { name })).toBeVisible();
    await expect(learnerPage.getByText(mine.card.assignments)).toHaveCount(0);

    // The instructor sees the learner in the roster, with contact details and no F4 columns.
    await page.reload();
    const roster = page.getByRole("table", { name: d.students.title });
    await expect(roster.getByText(learner.email)).toBeVisible();
    await expect(roster.getByText(d.students.col.avgScore)).toHaveCount(0);

    // Removing the learner keeps the history (left_at is set) and ends the learner's membership.
    await page
      .getByRole("button", {
        name: `${d.students.remove} ${learner.displayName}`,
      })
      .click();
    await page
      .getByRole("dialog")
      .getByRole("button", { name: m.popup.removeStudentConfirm })
      .click();
    await expect(roster.getByText(learner.email)).toHaveCount(0);
    expect(
      sql(
        `select count(*) from identity.class_enrollments e join identity.users u on u.id = e.student_id
          where e.class_id = '${id}' and u.email = '${learner.email}' and e.left_at is not null`,
      ),
    ).toBe("1");

    await learnerPage.goto(`${APP_URL}/classes`);
    await expect(learnerPage.getByText(mine.empty)).toBeVisible();
    // The class they left reads as not found on their own page.
    await learnerPage.goto(`${APP_URL}/classes/${id}`);
    await expect(learnerPage.getByText(mine.detail.notFound)).toBeVisible();
  } finally {
    await learnerContext.close();
  }
});

test("too many wrong codes are refused with their own message", async ({
  page,
  request,
}) => {
  const learner = await createUser(request, { tag: "cls-rate" });
  await page.goto(`${APP_URL}/login`);
  await submitLogin(page, learner.email, learner.password);
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 });
  await page.goto(`${APP_URL}/classes`);

  for (let attempt = 1; attempt <= 6; attempt += 1) {
    await page.getByLabel(mine.join.label).fill(`BAD${attempt}2345`);
    await page.getByRole("button", { name: mine.join.submit }).click();
    await expect(
      page
        .getByText(
          attempt <= 5
            ? (apiErrors["IDT-410"] ?? "")
            : (apiErrors["IDT-412"] ?? ""),
        )
        .first(),
    ).toBeVisible();
  }
});

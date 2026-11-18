// SHR0301 (interview question management) against the REAL backend: F6 #17 to #23. Replaces the mock-mode
// `interview-question-management.spec.ts` (fixed row counts). The test seeds its own questions in the
// database (title token `E2EIQMGT<ts>`, topic and level known) and derives every expected number from
// them. Needs the stack in e2e/README.md; skips itself otherwise.
import { expect, test } from "@playwright/test";
import { pickFilter } from "./filter-menu";
import { APP_URL, requireRealStack, sql, vi } from "./support/real-stack";
import { loginAsAdmin, userId } from "./support/real-session";

requireRealStack();

const m = vi.interviewQuestionManagement;
const token = `E2EIQMGT${Date.now()}`;
const TOPIC_A = "DATABASE";
const TOPIC_B = "CS_THEORY";
const DB_COUNT = 13;
const CS_COUNT = 8;
const TOTAL = DB_COUNT + CS_COUNT;

const resultText = (shown: number, total: number) =>
  m.resultCount
    .replace("{shown}", String(shown))
    .replace("{total}", String(total));

function seed(adminId: string) {
  const insert = (topic: string, level: string, n: number, label: string) =>
    sql(
      `insert into interview_bank.interview_questions(id,topic_id,level_id,title,status,created_by)
       select gen_random_uuid(), t.id, l.id, '${token} ${label} ${n}', 'ACTIVE', '${adminId}'
         from interview_bank.question_topics t, interview_bank.question_levels l
        where t.code='${topic}' and l.code='${level}'`,
    );
  for (let n = 1; n <= DB_COUNT; n++) insert(TOPIC_A, "EASY", n, "alpha");
  for (let n = 1; n <= CS_COUNT; n++) insert(TOPIC_B, "HARD", n, "beta");
}

test.afterAll(() => {
  sql(
    `delete from interview_bank.answer_rubrics where question_id in (select id from interview_bank.interview_questions where title like '${token}%')`,
  );
  sql(
    `delete from interview_bank.interview_questions where title like '${token}%'`,
  );
});

test("search, topic and level filters and paging match the seeded rows", async ({
  page,
  request,
}) => {
  const admin = await loginAsAdmin(page, request, "iqm-list");
  seed(userId(admin.email));
  await page.goto(`${APP_URL}/admin/interview-questions`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.title);

  const overall = Number(
    sql(
      "select count(*) from interview_bank.interview_questions where status='ACTIVE'",
    ),
  );
  const rows = page
    .getByRole("table", { name: m.tableCaption })
    .locator("tbody tr");

  await page.getByLabel(m.searchLabel).fill(token);
  await expect(page.getByText(resultText(TOTAL, overall))).toBeVisible({
    timeout: 15_000,
  });
  // Default page size is 8: 21 rows are three pages of 8, 8 and 5.
  await expect(rows).toHaveCount(8);
  await expect(
    page.getByText(
      m.pageLabel
        .replace("{page}", "1")
        .replace("{totalPages}", "3")
        .replace("{shown}", "8"),
    ),
  ).toBeVisible();
  await page
    .getByRole("button", { name: m.goToPage.replace("{page}", "3") })
    .click();
  await expect(rows).toHaveCount(TOTAL - 16, { timeout: 10_000 });

  // Topic filter narrows the same search; level filter narrows it further; the pair has no overlap.
  await pickFilter(page, m.topicFilterLabel, "Database");
  await expect(page.getByText(resultText(DB_COUNT, overall))).toBeVisible({
    timeout: 10_000,
  });
  await pickFilter(page, m.levelFilterLabel, "Khó");
  // The empty message is itself one table row, so assert the message and the count, not zero rows.
  await expect(page.getByText(m.emptyFiltered)).toBeVisible({
    timeout: 10_000,
  });
  await expect(page.getByText(resultText(0, overall))).toBeVisible();
  await pickFilter(page, m.topicFilterLabel, "Lý thuyết CS");
  await expect(page.getByText(resultText(CS_COUNT, overall))).toBeVisible({
    timeout: 10_000,
  });
  await expect(rows).toHaveCount(CS_COUNT);
});

test("deleting a question asks first, then retires it in the database", async ({
  page,
  request,
}) => {
  const admin = await loginAsAdmin(page, request, "iqm-del");
  const title = `${token} gamma delete`;
  sql(
    `insert into interview_bank.interview_questions(id,topic_id,level_id,title,status,created_by)
     select gen_random_uuid(), t.id, l.id, '${title}', 'ACTIVE', '${userId(admin.email)}'
       from interview_bank.question_topics t, interview_bank.question_levels l
      where t.code='${TOPIC_A}' and l.code='EASY'`,
  );
  await page.goto(`${APP_URL}/admin/interview-questions`);
  await page.getByLabel(m.searchLabel).fill(`${token} gamma`);
  const row = page
    .getByRole("table", { name: m.tableCaption })
    .locator("tbody tr")
    .filter({ hasText: title });
  await expect(row).toHaveCount(1, { timeout: 15_000 });

  await row.getByRole("button", { name: /^Xoá câu hỏi/ }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toContainText(/Xoá câu hỏi IQ-\d+\?/);
  // Cancelling changes nothing.
  await dialog.getByRole("button", { name: m.cancel }).click();
  await expect(dialog).toHaveCount(0);
  expect(
    sql(
      `select status from interview_bank.interview_questions where title='${title}'`,
    ),
  ).toBe("ACTIVE");

  await row.getByRole("button", { name: /^Xoá câu hỏi/ }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: m.delete, exact: true })
    .click();
  await expect(row).toHaveCount(0, { timeout: 15_000 });
  expect(
    sql(
      `select status from interview_bank.interview_questions where title='${title}'`,
    ),
  ).toBe("RETIRED");
});

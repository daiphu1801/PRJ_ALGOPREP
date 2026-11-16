// SHR0302 (interview question authoring) against the REAL backend: F6 #20 to #23. Replaces the mock-mode
// `interview-question-authoring.spec.ts`. Saved rows are checked in the database. Questions use the
// title prefix `E2E-IQ` and are removed afterwards. Needs the stack in e2e/README.md; skips itself otherwise.
import { expect, test } from "@playwright/test";
import { APP_URL, requireRealStack, sql, vi } from "./support/real-stack";
import { loginAsAdmin } from "./support/real-session";

requireRealStack();

const m = vi.interviewQuestionAuthoring;
const run = `E2E-IQ-AUTH-${Date.now()}`;

test.afterAll(() => {
  sql(
    `delete from interview_bank.answer_rubrics where question_id in (select id from interview_bank.interview_questions where title like '${run}%')`,
  );
  sql(
    `delete from interview_bank.interview_questions where title like '${run}%'`,
  );
});

const saveButton = (page: Parameters<typeof loginAsAdmin>[0]) =>
  page.getByRole("button", { name: m.save, exact: true });

test("saving is one action with no publish step, and the classification lists come from the server", async ({
  page,
  request,
}) => {
  await loginAsAdmin(page, request, "iqa-form");
  await page.goto(`${APP_URL}/admin/interview-questions/new`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(m.titleNew);

  await expect(saveButton(page)).toBeVisible();
  await expect(page.getByRole("button", { name: /xuất bản/i })).toHaveCount(0);

  const topics = Number(
    sql("select count(*) from interview_bank.question_topics"),
  );
  const levels = Number(
    sql("select count(*) from interview_bank.question_levels"),
  );
  await expect(page.getByLabel(m.topicLabel).getByRole("option")).toHaveCount(
    topics,
    { timeout: 15_000 },
  );
  await expect(page.getByLabel(m.levelLabel).getByRole("option")).toHaveCount(
    levels,
  );

  // The follow-up list never reaches zero inputs; adding a row enables removing.
  const remove = page.getByRole("button", {
    name: m.removeFollowUp.replace("{index}", "1"),
  });
  await expect(remove).toBeDisabled();
  await page.getByRole("button", { name: m.addFollowUp }).click();
  await expect(
    page.getByRole("button", { name: /^Xoá câu đào sâu/ }),
  ).toHaveCount(2);
  await expect(remove).toBeEnabled();
});

test("an empty rubric saves and the row is stored without criteria", async ({
  page,
  request,
}) => {
  await loginAsAdmin(page, request, "iqa-empty");
  await page.goto(`${APP_URL}/admin/interview-questions/new`);
  const title = `${run} empty rubric`;

  await page.getByLabel(m.questionLabel).fill(title);
  await expect(page.getByText(m.rubricEmptyTitle)).toBeVisible();
  await expect(saveButton(page)).not.toHaveAttribute("aria-disabled", "true");
  await saveButton(page).click();

  await expect(page.getByText(m.toast.saved).first()).toBeVisible({
    timeout: 15_000,
  });
  await expect(page).toHaveURL(
    /\/admin\/interview-questions\/[0-9a-f-]+\/edit/,
  );
  expect(
    sql(
      `select count(*) from interview_bank.interview_questions where title='${title}' and status='ACTIVE'`,
    ),
  ).toBe("1");
  expect(
    sql(
      `select count(*) from interview_bank.answer_rubrics r join interview_bank.interview_questions q on q.id = r.question_id where q.title='${title}'`,
    ),
  ).toBe("0");
});

test("a rubric that misses 100 percent is refused with the reason, and saves once it reaches 100", async ({
  page,
  request,
}) => {
  await loginAsAdmin(page, request, "iqa-rubric");
  await page.goto(`${APP_URL}/admin/interview-questions/new`);
  const title = `${run} rubric`;

  await page.getByLabel(m.questionLabel).fill(title);
  await page.getByRole("button", { name: m.addCriterion }).click();
  await page
    .getByLabel(m.criterionLabel.replace("{index}", "1"))
    .fill("So sanh hai co che");

  // One criterion at 0 percent: the button reads as disabled and a click names the reason.
  await expect(saveButton(page)).toHaveAttribute("aria-disabled", "true");
  // aria-disabled only: the click stays possible so it can explain itself (Playwright needs force).
  await saveButton(page).click({ force: true });
  await expect(page.getByText(m.rubricBlockTitle).first()).toBeVisible();
  expect(
    sql(
      `select count(*) from interview_bank.interview_questions where title='${title}'`,
    ),
  ).toBe("0");

  // Twenty clicks of +5 reach 100.
  const increment = page.getByRole("button", {
    name: m.criterionWeightIncrement.replace("{index}", "1"),
  });
  for (let i = 0; i < 20; i += 1) await increment.click();
  await expect(
    page.getByText(m.rubricTotal.replace("{total}", "100")),
  ).toBeVisible();
  await expect(saveButton(page)).not.toHaveAttribute("aria-disabled", "true");

  await saveButton(page).click();
  await expect(page.getByText(m.toast.saved).first()).toBeVisible({
    timeout: 15_000,
  });
  expect(
    sql(
      `select count(*)||'|'||coalesce(sum(r.weight_percent),0) from interview_bank.answer_rubrics r join interview_bank.interview_questions q on q.id = r.question_id where q.title='${title}'`,
    ),
  ).toMatch(/^1\|100(\.0+)?$/);
});

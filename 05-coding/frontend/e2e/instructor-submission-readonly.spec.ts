import { expect, test } from "@playwright/test";
import { pickFilter } from "./filter-menu";

// DEC-2026-1013-instructor-avatar-menu-and-read-source: A2 reads a learner's submission, read-only;
// outside the instructor's classes it is "not found".
test("a submission outside the instructor's classes is not found", async ({
  page,
}) => {
  await page.goto("/instructor/submissions/SB-99999");
  await expect(page.getByText("Không tìm thấy bài nộp.")).toBeVisible();
});

test("a student's history filters by result and opens the submission with who submitted it", async ({
  page,
}) => {
  await page.goto("/instructor/classes/c1/students/s1");
  const history = page.getByRole("table", { name: "Lịch sử nộp bài" });
  await expect(history.getByRole("row")).toHaveCount(5); // header + 4 submissions (one still judging)

  await pickFilter(page, "Lọc theo kết quả", "Đã chấp nhận");
  await expect(history.getByRole("row")).toHaveCount(2); // header + 1 accepted
  await expect(page.getByText("1 lượt nộp")).toBeVisible();
  // "Chưa đạt" is judged-and-failed only: the submission still being judged is not in it.
  await pickFilter(page, "Lọc theo kết quả", "Chưa đạt");
  await expect(history.getByRole("row")).toHaveCount(3); // header + wrong answer + compile error
  await pickFilter(page, "Lọc theo kết quả", "Đã chấp nhận");

  await history.getByRole("link", { name: "Xem kết quả bài Two Sum" }).click();
  await expect(page).toHaveURL(
    /\/instructor\/submissions\/SB-\d+\?studentId=s1&classId=c1&from=student$/,
  );

  // The sidebar keeps the item of the calling screen lit (from=student): "Danh sách học viên".
  const navStudents = page.getByRole("navigation", {
    name: "Điều hướng khu giảng viên",
  });
  await expect(
    navStudents.getByRole("link", { name: "Danh sách học viên" }),
  ).toHaveAttribute("aria-current", "page");

  // Context card: who submitted and in which class.
  const card = page.getByRole("region", { name: "Bài nộp của học viên" });
  await expect(card.getByRole("link", { name: "Nguyễn Văn An" })).toBeVisible();
  await expect(
    card.getByRole("link", { name: "Lập trình Java K21" }),
  ).toBeVisible();
  // Breadcrumb walks back through class and student, not through a queue.
  const crumbs = page.getByRole("navigation", { name: "Đường dẫn phân cấp" });
  await expect(crumbs.getByRole("link", { name: "Lớp của tôi" })).toBeVisible();
  await expect(
    crumbs.getByRole("link", { name: "Nguyễn Văn An" }),
  ).toBeVisible();
  // No stat-card strip for the instructor (owner rule): the figures are one line in the banner.
  await expect(page.getByText("RUNTIME", { exact: true })).toHaveCount(0);
});

test("the sidebar lists every submission of the instructor's classes, filterable, and a row opens its result", async ({
  page,
}) => {
  await page.goto("/instructor/overview");
  const nav = page.getByRole("navigation", {
    name: "Điều hướng khu giảng viên",
  });
  await nav.getByRole("link", { name: "Bài nộp" }).click();
  await expect(page).toHaveURL(/\/instructor\/submissions$/);
  await expect(
    page.getByRole("heading", { level: 1, name: "Bài nộp" }),
  ).toBeVisible();

  const table = page.getByRole("table", { name: "Bài nộp" });
  await expect(table.getByRole("row")).toHaveCount(21); // header + one page of 20 (fixed page size)
  await expect(nav.getByRole("link", { name: "Bài nộp" })).toHaveAttribute(
    "aria-current",
    "page",
  );

  // Filters live in the URL, so a reload keeps them.
  await pickFilter(page, "Lọc theo kết quả", "Đã chấp nhận");
  await expect(page).toHaveURL(/result=accepted/);
  const accepted = await table.getByRole("row").count();
  await page.reload();
  await expect(table.getByRole("row")).toHaveCount(accepted);
  await expect(table.getByText("Đã chấp nhận").first()).toBeVisible();
  await expect(table.getByText("Sai kết quả")).toHaveCount(0);

  // A row opens the read-only result with its learner and class.
  await table
    .getByRole("link", { name: /^Xem bài nộp của / })
    .first()
    .click();
  await expect(page).toHaveURL(
    /\/instructor\/submissions\/SB-\d+\?studentId=.+&classId=.+$/,
  );
  await expect(
    page.getByRole("region", { name: "Bài nộp của học viên" }),
  ).toBeVisible();

  // Opened from the list: the breadcrumb goes back in history, so the filter is still there.
  await page
    .getByRole("navigation", { name: "Đường dẫn phân cấp" })
    .getByRole("link", { name: "Bài nộp" })
    .click();
  await expect(page).toHaveURL(/\/instructor\/submissions\?.*result=accepted/);
});

import { expect, test } from "@playwright/test";

/**
 * Author scope of the instructor (A2): lists and edits only their own problems, reads published
 * problems of others, never edits them (BD SHR0201 Q1, SHR0203 Q8 as relaxed by
 * DEC-2026-1011-a2-read-published-problems-of-others). The mock decides the viewer from the area
 * the page is mounted under.
 */
test("the list holds only the instructor's own problems and no admin-only buttons", async ({
  page,
}) => {
  await page.goto("/instructor/problems");
  await expect(page.getByText("8 / 8 bài")).toBeVisible();
  await expect(page.getByText("#207")).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Quản lý chủ đề" }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Quản lý độ khó" }),
  ).toHaveCount(0);
});

test("another author's published problem opens read-only, but cannot be edited", async ({
  page,
}) => {
  await page.goto("/instructor/problems/207");
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await expect(page.getByText("Không tìm thấy bài tập.")).toHaveCount(0);

  await page.goto("/instructor/problems/207/edit");
  await expect(page.getByText("Không tìm thấy bài tập.")).toBeVisible();
});

test("'Chi tiết' on the class assignment list stays inside the instructor area", async ({
  page,
}) => {
  await page.goto("/instructor/assignments");
  await page.getByRole("link", { name: "Chi tiết" }).first().click();
  await expect(page).toHaveURL(/\/instructor\/problems\/\d+$/);
  await expect(page.locator(".instructor-shell")).toHaveCount(1);
});

test("the read-only view of another author's problem has no edit button", async ({
  page,
}) => {
  await page.goto("/instructor/problems/207");
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await expect(page.getByRole("link", { name: "Sửa bài" })).toHaveCount(0);

  await page.goto("/instructor/problems/139");
  await expect(page.getByRole("link", { name: "Sửa bài" })).toBeVisible();
});

test("an instructor creates a new problem like an admin, and it is theirs", async ({
  page,
}) => {
  await page.goto("/instructor/problems");
  await page.getByRole("link", { name: "Bài tập mới" }).click();
  await expect(page).toHaveURL(/\/instructor\/problems\/new$/);

  await page.getByLabel("Tiêu đề").fill("Bài giảng viên vừa tạo");
  await page.getByRole("button", { name: "Lưu", exact: true }).click();
  await expect(page.getByText(/đã lưu/i).first()).toBeVisible({
    timeout: 15_000,
  });

  // The new problem belongs to the instructor who created it: it shows in their own list. The mock
  // store lives in memory, so go back by client navigation (a reload would drop it).
  await page.getByRole("link", { name: "Quay lại" }).click();
  await expect(page.getByText("Bài giảng viên vừa tạo")).toBeVisible();
});

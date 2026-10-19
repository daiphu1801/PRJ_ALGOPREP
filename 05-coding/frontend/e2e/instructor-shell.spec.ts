import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for the Instructor shell. All 5 Instructor prototypes repeat the same
 * sidebar and the same blue palette (09-layoutBase/Giáo viên - Tổng quan.dc.html:18-46, :63-121),
 * but the app rendered them with no shell at all and on the generic placeholder tokens — reported
 * by the owner 2026-09-27 as "chưa theo layoutbase ở css". This spec pins down what was missing:
 * the sidebar exists on every Instructor route, marks the current one, and the palette scope is
 * actually applied (a screen on the generic tokens has a white background, not the mockup's
 * #F5F8FC).
 */
test("the sidebar is grouped like the admin one and marks the active route", async ({
  page,
}) => {
  await page.goto("/instructor/overview");

  const nav = page.getByRole("navigation", {
    name: "Điều hướng khu giảng viên",
  });
  await expect(nav.getByRole("link", { name: "Tổng quan" })).toHaveAttribute(
    "aria-current",
    "page",
  );
  // Groups start open for the instructor (owner 2026-10-13, Q-K): overview + 2 + 2 + 2 destinations.
  await expect(nav.getByRole("link")).toHaveCount(7);

  // A group header still toggles its own group only.
  await nav.getByRole("button", { name: "Nội dung" }).click();
  await expect(nav.getByRole("link")).toHaveCount(5);
  await nav.getByRole("button", { name: "Nội dung" }).click();
  await expect(nav.getByRole("link")).toHaveCount(7);

  await nav.getByRole("link", { name: "Lớp của tôi" }).click();
  await expect(page).toHaveURL(/\/instructor\/classes$/);
  await expect(nav.getByRole("link", { name: "Lớp của tôi" })).toHaveAttribute(
    "aria-current",
    "page",
  );
});

test("instructor screens render in the mockup palette, not the generic tokens", async ({
  page,
}) => {
  await page.goto("/instructor/overview");

  const shell = page.locator(".instructor-shell");
  await expect(shell).toHaveCount(1);
  // dc.html:20 --bg light. The generic :root value is #ffffff, so this is what separates
  // "the scope is applied" from "the scope silently did not match".
  await expect(shell).toHaveCSS("background-color", "rgb(245, 248, 252)");

  await page.screenshot({
    path: "e2e/__screenshots__/instructor-overview.png",
    fullPage: true,
  });
});

test("a nested route lights up exactly one nav item", async ({ page }) => {
  // A student detail page sits under /instructor/classes/<id>/students/<id> — its parent
  // "Lớp của tôi" must be the ONLY item marked.
  await page.goto("/instructor/classes/c1/students/s1");

  const nav = page.getByRole("navigation", {
    name: "Điều hướng khu giảng viên",
  });
  await expect(nav.getByRole("link", { name: "Lớp của tôi" })).toHaveAttribute(
    "aria-current",
    "page",
  );
  await expect(nav.locator('[aria-current="page"]')).toHaveCount(1);
});

test("class management holds no student table; the student list does", async ({
  page,
}) => {
  // The two near-identical student tables were merged 2026-09-27: classes manage classes, the
  // student list manages students.
  await page.goto("/instructor/classes");
  // One list of classes, no student table.
  await expect(page.getByRole("table")).toHaveCount(1);
  await expect(page.getByRole("table", { name: "Lớp của tôi" })).toBeVisible();

  // The class detail page holds that class's students; "Mở trong danh sách học viên" carries the
  // class through as a preselected filter.
  await page
    .getByRole("link", { name: /^Xem chi tiết lớp/ })
    .first()
    .click();
  await expect(page).toHaveURL(/\/instructor\/classes\/c\d+$/);
  await expect(
    page.getByRole("table", { name: "Học viên của lớp" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Mở trong danh sách học viên" }).click();
  await expect(page).toHaveURL(/\/instructor\/students\?classId=/);
  await expect(
    page.getByRole("table", { name: "Danh sách học viên" }),
  ).toBeVisible();
});

test("the sidebar stays put while a long table scrolls", async ({ page }) => {
  // Before dc.html:63's `position: sticky; top: 0; height: 100vh` was ported, the sidebar was an
  // ordinary flex child: the student table stretched the page and scrolled the whole nav away.
  await page.setViewportSize({ width: 1440, height: 800 });
  await page.goto("/instructor/students");

  const nav = page.getByRole("navigation", {
    name: "Điều hướng khu giảng viên",
  });
  await expect(nav).toBeVisible();
  await page.mouse.wheel(0, 2000);

  await expect.poll(async () => (await nav.boundingBox())?.y).toBe(0);
  await expect.poll(async () => (await nav.boundingBox())?.height).toBe(800);
});

test("the student list shows up to 20 rows a page, with no size picker", async ({
  page,
}) => {
  await page.goto("/instructor/students");

  const table = page.getByRole("table", { name: "Danh sách học viên" });
  await expect(table).toBeVisible();
  // 20 rows a page, fixed (DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric): the 12 learners of
  // the mock fit one page, and the mock learner who left is not listed.
  await expect(table.locator("tbody tr")).toHaveCount(12);
  await expect(page.getByText("Trang 1 / 1")).toBeVisible();
  await expect(
    page.getByRole("combobox", { name: "Số dòng mỗi trang" }),
  ).toHaveCount(0);
});

test("the content group opens problem management with its own nav item lit", async ({
  page,
}) => {
  await page.goto("/instructor/classes");
  const nav = page.getByRole("navigation", {
    name: "Điều hướng khu giảng viên",
  });
  // The "Nội dung" group is already open (Q-K), so the item is reachable without a toggle.
  await nav.getByRole("link", { name: "Quản lý bài tập" }).click();
  await expect(page).toHaveURL(/\/instructor\/problems$/);
  await expect(nav.locator('[aria-current="page"]')).toHaveCount(1);
  // The route owns the group, so it stays open after the navigation.
  await expect(
    nav.getByRole("link", { name: "Câu hỏi phỏng vấn" }),
  ).toBeVisible();
  // A2 never gets the ADMIN-only topic and level management buttons.
  await expect(
    page.getByRole("button", { name: "Quản lý chủ đề" }),
  ).toHaveCount(0);
});

test("profile and settings, opened from the toolbar menu, stay inside the instructor shell", async ({
  page,
}) => {
  await page.goto("/instructor/classes");
  // The menu button can be clicked before hydration attaches its handler on a cold dev server, so
  // retry opening the menu until the item shows.
  await expect(async () => {
    await page.locator('button[aria-haspopup="menu"]').click();
    await page
      .getByRole("menuitem", { name: "Trang cá nhân" })
      .click({ timeout: 2_000 });
  }).toPass({ timeout: 15_000 });
  // The first visit compiles the route on the dev server, so give it more than the default 5 s.
  await expect(page).toHaveURL(/\/instructor\/profile$/, { timeout: 15_000 });
  await expect(page.locator(".instructor-shell")).toHaveCount(1);
  await expect(page.locator("main dd", { hasText: "Giảng viên" })).toBeVisible({
    timeout: 15_000,
  });
});

test("the assign popup gives a bank problem to a class without leaving the shell", async ({
  page,
}) => {
  await page.goto("/instructor/assignments");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Bài giao cho lớp",
  );
  await page
    .getByRole("button", { name: "+ Gán từ ngân hàng bài toán" })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel(/Binary Search/).check();
  await dialog.getByRole("button", { name: "Giao 1 bài" }).click();

  await expect(page).toHaveURL(/\/instructor\/assignments$/);
  await expect(page.getByRole("cell", { name: "Binary Search" })).toBeVisible();
});

test("the toolbar user menu shows who is signed in with a sign-out link, and the area has a footer", async ({
  page,
}) => {
  await page.goto("/instructor/classes");
  const trigger = page.locator('button[aria-haspopup="menu"]');
  await expect(trigger).toContainText("Giảng viên");
  await trigger.click();
  await expect(page.getByRole("menuitem", { name: "Cài đặt" })).toBeVisible();
  await expect(page.getByRole("menuitem", { name: "Thoát" })).toHaveAttribute(
    "href",
    "/login",
  );
  await expect(page.getByText("AlgoPrep Giảng viên · v2.4.1")).toBeVisible();
});

test("on a short screen the footer sits at the bottom of the viewport", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/instructor/overview");
  const footer = page.getByText("AlgoPrep Giảng viên · v2.4.1");
  await expect(footer).toBeVisible();
  const box = await footer.boundingBox();
  // Within one footer height of the bottom edge, not hugging the content above it.
  expect(box!.y + box!.height).toBeGreaterThan(900 - 80);
});

test("editing a class opens the form filled in, and the description is saved", async ({
  page,
}) => {
  await page.goto("/instructor/classes");
  await page
    .getByRole("button", { name: "Sửa thông tin lớp Lập trình Java K21" })
    .click();
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByRole("textbox", { name: /Tên lớp/ })).toHaveValue(
    "Lập trình Java K21",
  );
  await dialog.getByRole("textbox", { name: "Mô tả" }).fill("Mô tả đã sửa");
  await dialog.getByRole("button", { name: "Lưu" }).click();
  await expect(page.getByText(/Đã lưu thay đổi lớp/).last()).toBeVisible();

  await page
    .getByRole("button", { name: "Sửa thông tin lớp Lập trình Java K21" })
    .click();
  await expect(
    page.getByRole("dialog").getByRole("textbox", { name: "Mô tả" }),
  ).toHaveValue("Mô tả đã sửa");
});

test("a class schedule is picked by weekday, time range and room, and shows as one line", async ({
  page,
}) => {
  await page.goto("/instructor/classes");
  await page
    .getByRole("button", { name: "Sửa thông tin lớp Lập trình Java K21" })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("checkbox", { name: "T3" }).click();
  await dialog.getByLabel("Giờ kết thúc").fill("21:30");
  await dialog.getByRole("textbox", { name: "Phòng / địa điểm" }).fill("A101");
  await dialog.getByRole("button", { name: "Lưu" }).click();

  await expect(
    page.getByText("T2 · T3 · T4 · T6 — 19:00–21:30 · A101 · 17/08–20/12"),
  ).toBeVisible();
});

test("the class detail page shows the schedule, the invite codes and the students, and a new code can be made", async ({
  page,
}) => {
  await page.goto("/instructor/classes/c1");
  await expect(
    page.getByRole("heading", { level: 1, name: "Lập trình Java K21" }),
  ).toBeVisible();
  await expect(page.getByText("Phòng B203", { exact: true })).toBeVisible();
  await expect(page.getByText("19:00 – 21:00")).toBeVisible();
  await expect(
    page.getByRole("table", { name: "Học viên của lớp" }),
  ).toBeVisible();

  await page.getByRole("button", { name: "Tạo mã mới" }).click();
  await expect(page.getByText(/^NEW-/)).toBeVisible();

  // Opened from the class list, still inside the "Lớp của tôi" group of the sidebar.
  const nav = page.getByRole("navigation", {
    name: "Điều hướng khu giảng viên",
  });
  await expect(nav.getByRole("link", { name: "Lớp của tôi" })).toHaveAttribute(
    "aria-current",
    "page",
  );
});

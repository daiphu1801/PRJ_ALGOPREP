import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for admin_language_config after the 2026-10-10 redesign: two tabs, a
 * four-column table, one sticky save bar, and the save rules from BD ADM0501 Sheet 9. Still exactly
 * three locked languages, and rejudge stays out of scope (DEC-2026-0828-remove-rejudge-scope).
 */
test("language table matches the locked scope", async ({ page }) => {
  await page.goto("/admin/language-config");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Ngôn ngữ và giới hạn chấm",
  );

  const table = page.getByRole("table", { name: "Ngôn ngữ được hỗ trợ" });
  await expect(table).toBeVisible();

  // Exactly three languages, no "add language" affordance (README section 6, PROTOTYPE_DEBT 1.1).
  await expect(table.getByRole("row")).toHaveCount(4); // header + 3
  await expect(table.getByRole("columnheader")).toHaveCount(4);
  for (const name of ["Python 3", "C++ 17", "Java 21"]) {
    await expect(table.getByText(name, { exact: true })).toBeVisible();
  }
  await expect(
    page.getByRole("button", { name: /thêm ngôn ngữ/i }),
  ).toHaveCount(0);
  await expect(page.getByRole("link", { name: /chấm lại/i })).toHaveCount(0);
  // The mockup's decorative environment id is gone.
  await expect(page.getByText(/go-judge #/)).toHaveCount(0);

  // Effective limits in one cell: 1000 ms x3 for Python, 256 MB x2 for Java.
  await expect(table.getByText("3.000 ms · 256 MB")).toBeVisible();
  await expect(table.getByText("2.000 ms · 512 MB")).toBeVisible();
});

test("the save bar appears with the change count, and discard restores the page", async ({
  page,
}) => {
  await page.goto("/admin/language-config");
  const table = page.getByRole("table", { name: "Ngôn ngữ được hỗ trợ" });
  await expect(
    page.getByRole("button", { name: "Lưu", exact: true }),
  ).toHaveCount(0);

  await page.getByLabel("Hệ số thời gian cho Python 3").fill("4");
  await expect(table.getByText("4.000 ms · 256 MB")).toBeVisible();
  await expect(page.getByText("1 thay đổi chưa lưu")).toBeVisible();

  await page.getByRole("button", { name: "Hoàn tác" }).click();
  await expect(table.getByText("3.000 ms · 256 MB")).toBeVisible();
  await expect(page.getByText(/thay đổi chưa lưu/)).toHaveCount(0);
});

test("turning every language off is refused, and one off asks before saving", async ({
  page,
}) => {
  await page.goto("/admin/language-config");
  const save = () => page.getByRole("button", { name: "Lưu", exact: true });

  for (const name of ["Python 3", "C++ 17", "Java 21"]) {
    await page.getByRole("switch", { name: `Bật ngôn ngữ ${name}` }).click();
  }
  await save().click();
  await expect(
    page.getByText("Phải giữ ít nhất một ngôn ngữ đang bật."),
  ).toBeVisible();
  await expect(page.getByRole("dialog")).toHaveCount(0);

  // Back to a single disabled language: now the one confirmation shows.
  await page.getByRole("switch", { name: "Bật ngôn ngữ Python 3" }).click();
  await page.getByRole("switch", { name: "Bật ngôn ngữ C++ 17" }).click();
  await save().click();
  await expect(page.getByRole("dialog")).toContainText("Java 21");
  await page.getByRole("button", { name: "Tắt và lưu" }).click();
  await expect(
    page.getByText("Đã lưu cấu hình ngôn ngữ và giới hạn"),
  ).toBeVisible();
});

test("a multiplier outside 0.10 to 9.99 is refused", async ({ page }) => {
  await page.goto("/admin/language-config");
  await page.getByLabel("Hệ số thời gian cho Python 3").fill("50");
  await page.getByRole("button", { name: "Lưu", exact: true }).click();
  await expect(
    page.getByText("Hệ số phải nằm trong khoảng 0,10 đến 9,99", {
      exact: false,
    }),
  ).toBeVisible();
});

test("sandbox and generator limits sit on the second tab", async ({ page }) => {
  await page.goto("/admin/language-config");
  await expect(page.getByText("Cho phép truy cập mạng")).toHaveCount(0);

  await page.getByRole("button", { name: "Sandbox và sinh testcase" }).click();
  await expect(page.getByText("Cho phép truy cập mạng")).toBeVisible();
  await expect(page.getByLabel("Thời gian chạy tối đa")).toHaveValue("30");
});

// Regenerates the images used to review this screen against the mockup. Not an assertion — the
// checks above are; this only keeps the evidence reproducible instead of hand-captured.
for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 940 });
    await page.addInitScript(
      (value) => localStorage.setItem("theme", value),
      theme,
    );
    await page.goto("/admin/language-config");
    await expect(page.getByRole("table")).toBeVisible();
    // caret: "initial" — Playwright's default caret hiding injects an inline
    // `caret-color: transparent` style, which React then reports as a hydration mismatch. The
    // warning is a test-harness artifact, not an app defect; this keeps it out of the logs.
    await page.screenshot({
      path: `e2e/__screenshots__/lang-config-${theme}.png`,
      caret: "initial",
    });
  });
}

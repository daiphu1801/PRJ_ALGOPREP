import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for admin_language_config. Asserts the parts the documents pin down, not
 * the pixels: exactly three locked languages, the rejudge link that
 * DEC-2026-0828-remove-rejudge-scope removed, and the editable multiplier that F2-10 requires.
 */
test("language config screen matches the locked scope", async ({ page }) => {
  await page.goto("/admin/language-config");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Ngôn ngữ và giới hạn chấm");

  const table = page.getByRole("table", { name: "Ngôn ngữ được hỗ trợ" });
  await expect(table).toBeVisible();

  // Exactly three languages, no "add language" affordance (README section 6, PROTOTYPE_DEBT 1.1).
  await expect(table.getByRole("row")).toHaveCount(4); // header + 3
  for (const name of ["Python 3", "C++ 17", "Java 21"]) {
    await expect(table.getByText(name, { exact: true })).toBeVisible();
  }
  await expect(page.getByRole("button", { name: /thêm ngôn ngữ/i })).toHaveCount(0);

  // Rejudge is out of scope — the mockup's link to it must not have been carried over.
  await expect(page.getByRole("link", { name: /chấm lại/i })).toHaveCount(0);

  // Derived limits: 1000 ms default x3 for Python, and 256 MB x2 for Java (dc.html:402-404, :418).
  await expect(table.getByText("3.000 ms")).toBeVisible();
  await expect(table.getByText("512 MB")).toBeVisible();

  // Save is inert until something actually changes, then the multiplier drives the derived column.
  const save = page.getByRole("button", { name: "Lưu thay đổi" });
  await expect(save).toBeDisabled();

  await page.getByLabel("Hệ số thời gian cho Python 3").fill("4");
  await expect(table.getByText("4.000 ms")).toBeVisible();
  await expect(save).toBeEnabled();
});

// Regenerates the images used to review this screen against the mockup. Not an assertion — the
// checks above are; this only keeps the evidence reproducible instead of hand-captured.
for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 940 });
    await page.addInitScript((value) => localStorage.setItem("theme", value), theme);
    await page.goto("/admin/language-config");
    await expect(page.getByRole("table")).toBeVisible();
    // caret: "initial" — Playwright's default caret hiding injects an inline
    // `caret-color: transparent` style, which React then reports as a hydration mismatch. The
    // warning is a test-harness artifact, not an app defect; this keeps it out of the logs.
    await page.screenshot({ path: `e2e/__screenshots__/lang-config-${theme}.png`, caret: "initial" });
  });
}

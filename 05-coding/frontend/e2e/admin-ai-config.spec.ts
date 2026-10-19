import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for admin_ai_config (BD ADM0301 V1.0, one tab per AI feature). The scope
 * guard here is the "Chạy đối chiếu" button: PROTOTYPE_DEBT 2.8 keeps the UI but defers the backend,
 * so it must be visibly inactive rather than wired to something that does not exist.
 */
test("the comparison run is present but inert, per the deferred-backend decision", async ({
  page,
}) => {
  await page.goto("/admin/ai-config");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Prompt và chất lượng AI",
  );

  const run = page.getByRole("button", { name: "Chạy đối chiếu" });
  await expect(run).toBeVisible();
  await expect(run).toBeDisabled();
});

test("each tab shows only its own feature: the comparison run belongs to the review tab", async ({
  page,
}) => {
  await page.goto("/admin/ai-config");

  await expect(
    page.getByRole("button", { name: "Chạy đối chiếu" }),
  ).toBeVisible();

  await page.getByRole("button", { name: "Phỏng vấn giả lập" }).click();
  await expect(
    page.getByRole("button", { name: "Chạy đối chiếu" }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("heading", { name: "Nguyên tắc trả lời" }),
  ).toBeVisible();

  await page.getByRole("button", { name: "Sinh testcase" }).click();
  await expect(
    page.getByRole("heading", { name: "Nguyên tắc trả lời" }),
  ).toHaveCount(0);
});

test("pending changes are counted, can be discarded and can be published", async ({
  page,
}) => {
  await page.goto("/admin/ai-config");
  const publish = page.getByRole("button", { name: "Lưu và phát hành" });

  // Nothing changed yet: nothing to publish.
  await expect(publish).toHaveAttribute("aria-disabled", "true");

  await page
    .getByRole("button", { name: /^Model/ })
    .first()
    .click();
  await page.getByRole("option", { name: "claude-opus" }).click();
  await expect(page.getByText("1 thay đổi chưa phát hành")).toBeVisible();
  await expect(
    page.getByRole("button", { name: /^Phân tích bài giải \(1\)$/ }),
  ).toBeVisible();

  await page.getByRole("button", { name: "Huỷ thay đổi" }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Huỷ thay đổi" })
    .click();
  await expect(page.getByText(/thay đổi chưa phát hành/)).toHaveCount(0);
  // The model picker is a menu button now, so its current value is part of the button text.
  await expect(
    page.getByRole("button", { name: /^Model/ }).first(),
  ).toContainText("claude-sonnet");

  await page
    .getByRole("button", { name: /^Model/ })
    .first()
    .click();
  await page.getByRole("option", { name: "claude-opus" }).click();
  await publish.click();
  // The confirmation names what replaces the live prompt; cancelling publishes nothing.
  const confirm = page.getByRole("dialog", { name: "Phát hành thay đổi?" });
  await expect(
    confirm.getByText("Model: claude-sonnet thành claude-opus"),
  ).toBeVisible();
  await confirm.getByRole("button", { name: "Huỷ" }).click();
  await expect(page.getByText("1 thay đổi chưa phát hành")).toBeVisible();

  await publish.click();
  await confirm.getByRole("button", { name: "Phát hành" }).click();
  await expect(page.getByText("Đã lưu và phát hành cấu hình AI")).toBeVisible();
  await expect(page.getByText("v3.9")).toBeVisible();
});

test("edit popup saves into the draft, refuses an empty instruction and asks before dropping edits", async ({
  page,
}) => {
  await page.goto("/admin/ai-config");
  await page.getByRole("button", { name: "Chỉnh sửa" }).click();
  const dialog = page.getByRole("dialog", {
    name: "Sửa prompt: Phân tích bài giải",
  });
  const box = dialog.getByLabel("Chỉ thị hệ thống");

  await box.fill("   ");
  await dialog.getByRole("button", { name: "Lưu bản nháp" }).click();
  await expect(
    page.getByText("Chỉ thị hệ thống không được để trống."),
  ).toBeVisible();

  // Closing with unsaved text asks first; staying keeps the text.
  await box.fill("Chỉ thị mới cho bản nháp");
  await dialog.getByRole("button", { name: "Huỷ" }).click();
  await page
    .getByRole("dialog", { name: "Bỏ thay đổi?" })
    .getByRole("button", { name: "Tiếp tục sửa" })
    .click();
  await expect(box).toHaveValue("Chỉ thị mới cho bản nháp");

  await dialog.getByRole("button", { name: "Lưu bản nháp" }).click();
  await expect(page.getByText("Đã lưu bản nháp")).toBeVisible();
  await expect(page.getByText("Bản nháp").first()).toBeVisible();
  await expect(page.getByText("1 thay đổi chưa phát hành")).toBeVisible();
});

test("restoring an older version puts its instruction into the draft only", async ({
  page,
}) => {
  await page.goto("/admin/ai-config");
  await page.getByRole("button", { name: "Nhật ký phiên bản" }).click();
  const log = page.getByRole("dialog", { name: /Nhật ký phiên bản/ });
  // v3.8 is live, so only v3.7 can be restored.
  await expect(log.getByRole("button", { name: "Khôi phục" })).toHaveCount(1);
  await log.getByRole("button", { name: "Khôi phục" }).click();
  await page
    .getByRole("dialog", { name: "Khôi phục v3.7?" })
    .getByRole("button", { name: "Khôi phục" })
    .click();

  await expect(page.getByText("Đã khôi phục v3.7 vào bản nháp")).toBeVisible();
  await expect(page.getByText("1 thay đổi chưa phát hành")).toBeVisible();
  // The live version label is unchanged until publish.
  await expect(page.getByText("v3.8")).toBeVisible();
});

test("version log lists only the selected feature", async ({ page }) => {
  await page.goto("/admin/ai-config");

  await page.getByRole("button", { name: "Nhật ký phiên bản" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  // Review has v3.8 and v3.7 in the mock.
  await expect(dialog.getByRole("listitem")).toHaveCount(2);

  await dialog.getByRole("button", { name: "Đóng" }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
});

test("model is picked from the dev-declared list and temperature is gone", async ({
  page,
}) => {
  await page.goto("/admin/ai-config");

  await expect(page.getByText("Nhiệt độ")).toHaveCount(0);

  const model = page.getByRole("button", { name: /^Model/ }).first();
  await expect(model).toContainText("claude-sonnet");
  await model.click();
  await expect(page.getByRole("option")).toHaveCount(3);
  await page.getByRole("option", { name: "claude-opus" }).click();
  await expect(model).toContainText("claude-opus");
});

test("answer guards are switches; the anti-leak guard can be turned off but warns (F5-17)", async ({
  page,
}) => {
  await page.goto("/admin/ai-config");

  const locked = page.getByRole("switch", {
    name: "Không đưa lời giải đầy đủ",
  });
  await expect(locked).toHaveAttribute("aria-checked", "true");
  await expect(locked).toBeEnabled();
  await locked.click();
  await expect(locked).toHaveAttribute("aria-checked", "false");
  await expect(
    page.getByText(/Đã tắt: AI có thể đưa lời giải đầy đủ/).first(),
  ).toBeVisible();

  const cite = page.getByRole("switch", {
    name: "Trích dẫn dòng mã người học",
  });
  await expect(cite).toHaveAttribute("aria-checked", "true");
  await cite.click();
  await expect(cite).toHaveAttribute("aria-checked", "false");
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1020 });
    await page.addInitScript(
      (value) => localStorage.setItem("theme", value),
      theme,
    );
    await page.goto("/admin/ai-config");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.screenshot({
      path: `e2e/__screenshots__/ai-config-${theme}.png`,
      caret: "initial",
    });
  });
}

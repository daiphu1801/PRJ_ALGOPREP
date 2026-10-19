import { expect, test } from "@playwright/test";

/**
 * PROTOTYPE screen check for admin_queue_monitor after the 2026-10-10 redesign: a status strip, one
 * compact worker list, one controls card, count tabs over the job table, and the infrastructure
 * events moved to their own screen. Rejudge stays out of scope
 * (DEC-2026-0828-remove-rejudge-scope) and so does job priority (BD ADM0401 Q4).
 */
test("status strip, no priority, no rejudge, no exam wording", async ({
  page,
}) => {
  await page.goto("/admin/queue");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Hàng đợi chấm",
  );
  // The mock has an overloaded worker, which outranks the backlog.
  await expect(page.getByRole("status").first()).toContainText(
    "Worker quá tải",
  );
  await expect(page.getByText(/Cập nhật lúc/)).toBeVisible();
  await expect(page.getByText(/ưu tiên/i)).toHaveCount(0);
  await expect(page.getByText(/chấm lại/i)).toHaveCount(0);
  await expect(page.getByText(/kỳ thi/i)).toHaveCount(0);
});

test("infrastructure events are a link row, the feed lives on its own screen", async ({
  page,
}) => {
  await page.goto("/admin/queue");

  await expect(page.getByRole("list", { name: "Sự kiện hạ tầng" })).toHaveCount(
    0,
  );
  await page.getByRole("link", { name: "Xem tất cả sự kiện" }).click();
  await expect(page).toHaveURL(/\/admin\/infra-events\?level=error$/);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Sự kiện hạ tầng",
  );
});

test("job tabs carry counts and the table pages by the saved rows per page", async ({
  page,
}) => {
  await page.goto("/admin/queue");

  const table = page.getByRole("table", { name: "Job trong hàng đợi" });
  await expect(
    page.getByRole("button", { name: "Đang chờ 21" }),
  ).toHaveAttribute("aria-pressed", "true");
  await expect(table.locator("tbody tr")).toHaveCount(20);

  await page.getByRole("button", { name: "Sau" }).click();
  await expect(table.locator("tbody tr")).toHaveCount(1);

  await page.getByRole("button", { name: "Lỗi 1" }).click();
  await expect(table.locator("tbody tr")).toHaveCount(1);
  await expect(table.locator("tbody tr").first()).toContainText("Lỗi sandbox");
});

test("column sort reorders the job table and announces itself", async ({
  page,
}) => {
  await page.goto("/admin/queue");

  const table = page.getByRole("table", { name: "Job trong hàng đợi" });
  const waitHeader = table.getByRole("columnheader").filter({ hasText: "Chờ" });

  // Default: longest wait first.
  await expect(waitHeader).toHaveAttribute("aria-sort", "descending");
  await expect(table.locator("tbody tr").first()).toContainText("46s");

  await waitHeader.getByRole("button").click();
  await expect(waitHeader).toHaveAttribute("aria-sort", "ascending");
  await expect(table.locator("tbody tr").first()).toContainText("2s");
});

test("pausing asks for confirmation first and updates the status strip", async ({
  page,
}) => {
  await page.goto("/admin/queue");

  const pause = page.getByRole("switch", {
    name: "Tạm dừng tiêu thụ hàng đợi",
  });
  await pause.click();
  await expect(pause).toHaveAttribute("aria-checked", "false");
  await expect(page.getByRole("dialog")).toContainText(
    "Job đang chấm vẫn chạy hết",
  );

  await page.getByRole("button", { name: "Tiếp tục" }).click();
  await expect(pause).toHaveAttribute("aria-checked", "true");
  await expect(page.getByRole("status").first()).toContainText("Đang tạm dừng");
});

for (const theme of ["light", "dark"] as const) {
  test(`capture ${theme} theme for mockup comparison`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 940 });
    await page.addInitScript(
      (value) => localStorage.setItem("theme", value),
      theme,
    );
    await page.goto("/admin/queue");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.screenshot({
      path: `e2e/__screenshots__/queue-monitor-${theme}.png`,
      caret: "initial",
    });
  });
}

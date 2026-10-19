import { expect, test } from "@playwright/test";

/** PROTOTYPE screen check for admin_infra_events (ADM0404), split out of the queue screen. */
test("lists machine events, filters by level and pages with load more", async ({
  page,
}) => {
  await page.goto("/admin/infra-events");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Sự kiện hạ tầng",
  );
  // Boundary with the system log is stated and linked.
  await expect(
    page.getByRole("link", { name: "Nhật ký hệ thống" }).first(),
  ).toBeVisible();

  const list = page.getByRole("list", { name: "Danh sách sự kiện hạ tầng" });
  await expect(list.getByRole("listitem")).toHaveCount(20);

  await page.getByRole("button", { name: /^Tải thêm 20 dòng$/ }).click();
  await expect(list.getByRole("listitem")).toHaveCount(22);
  await expect(
    page.getByRole("button", { name: /^Tải thêm 20 dòng$/ }),
  ).toBeDisabled();

  await page.getByLabel("Tìm sự kiện hạ tầng").fill("outbox");
  await expect(list.getByRole("listitem")).toHaveCount(9);
});

test("the sidebar lists it between the queue and language config", async ({
  page,
}) => {
  await page.goto("/admin/infra-events");
  const nav = page.getByRole("navigation", { name: "Điều hướng khu quản trị" });
  const links = nav.getByRole("link");
  const names = await links.allInnerTexts();
  const queue = names.indexOf("Hàng đợi chấm");
  expect(queue).toBeGreaterThan(-1);
  expect(names[queue + 1]).toBe("Sự kiện hạ tầng");
  expect(names[queue + 2]).toBe("Ngôn ngữ và giới hạn");
});

test("?level=error opens with the level filter already applied", async ({
  page,
}) => {
  const list = page.getByRole("list", { name: "Danh sách sự kiện hạ tầng" });
  await page.goto("/admin/infra-events?level=error");
  await expect(list.getByRole("listitem")).toHaveCount(7);
  await page.goto("/admin/infra-events?level=bogus");
  await expect(list.getByRole("listitem")).toHaveCount(20);
});

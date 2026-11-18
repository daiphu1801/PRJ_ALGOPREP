// Avatar (F1-35) against the REAL backend and MinIO: choose, preview, save, see it in the header,
// then remove it. Needs the stack in e2e/README.md; skips itself otherwise.
import { expect, test } from "@playwright/test";
import { deflateSync } from "node:zlib";
import { APP_URL, requireRealStack, vi } from "./support/real-stack";
import { loginAsStudent } from "./support/real-session";

requireRealStack();

const label = vi.profile.avatar;

function crc(buffer: Buffer): number {
  let c = ~0;
  for (const byte of buffer) {
    c ^= byte;
    for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1));
  }
  return ~c >>> 0;
}

function chunk(type: string, data: Buffer): Buffer {
  const body = Buffer.concat([Buffer.from(type), data]);
  const out = Buffer.alloc(body.length + 8);
  out.writeUInt32BE(data.length, 0);
  body.copy(out, 4);
  out.writeUInt32BE(crc(body), body.length + 4);
  return out;
}

/** A solid 64x64 PNG, built in memory so the spec ships no binary fixture. */
function png(): Buffer {
  const side = 64;
  const header = Buffer.alloc(13);
  header.writeUInt32BE(side, 0);
  header.writeUInt32BE(side, 4);
  header[8] = 8;
  header[9] = 2;
  const row = Buffer.concat([Buffer.from([0]), Buffer.alloc(side * 3, 0x80)]);
  const raw = Buffer.concat(Array.from({ length: side }, () => row));
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk("IHDR", header),
    chunk("IDAT", deflateSync(raw)),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}

test("a learner picks a picture, previews it, saves it, sees it in the header and removes it", async ({
  page,
  request,
}) => {
  await loginAsStudent(page, request, "avatar-flow");
  await page.goto(`${APP_URL}/profile`);

  await page
    .getByTestId("avatar-file-input")
    .setInputFiles({ name: "a.png", mimeType: "image/png", buffer: png() });
  await expect(page.getByText(label.previewLabel)).toBeVisible();
  await page.screenshot({
    path: "e2e/__screenshots__/avatar/profile-preview.png",
  });

  await page.getByRole("button", { name: label.save }).click();
  await expect(page.getByText(label.uploaded)).toBeVisible();
  const header = page.getByRole("button", { name: vi.nav.accountMenu });
  await expect(header.locator("img")).toBeVisible();
  await page.screenshot({
    path: "e2e/__screenshots__/avatar/profile-saved.png",
  });

  await page.reload();
  await expect(header.locator("img")).toBeVisible();

  await page.getByRole("button", { name: label.remove }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: label.removeConfirm })
    .click();
  await expect(page.getByText(label.removed)).toBeVisible();
  await expect(header.locator("img")).toHaveCount(0);
});

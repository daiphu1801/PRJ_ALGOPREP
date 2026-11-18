// Tests of the profile-picture editor (DD USR0502 BR-SC-07, T-PF-21 to T-PF-26 and T-PF-29). Only the
// HTTP layer and the canvas downscale are faked; the real entity calls and error mapping run.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
const downscale = vi.fn();
vi.mock("@/shared/lib/downscale-image", () => ({
  downscaleToSquare: (...args: unknown[]) => downscale(...args),
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { AvatarEditor } from "./avatar-editor";

const a = viMessages.profile.avatar;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const ME = "/identity/me/avatar";

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

function setup(avatarVersion: number | null = null) {
  render(
    withTestProviders(
      <AvatarEditor userId="u1" name="Phú Đại" avatarVersion={avatarVersion} />,
    ),
  );
}

function pick(file: File) {
  fireEvent.change(screen.getByTestId("avatar-file-input"), {
    target: { files: [file] },
  });
}

const png = () => new File(["x"], "me.png", { type: "image/png" });
const small = new Blob(["s"], { type: "image/webp" });

function clickLink() {
  const link = document.createElement("a");
  link.href = "/dashboard";
  document.body.appendChild(link);
  fireEvent.click(link);
  return link;
}

beforeEach(() => {
  resetApiMock();
  downscale.mockReset();
  downscale.mockResolvedValue({ blob: small, filename: "avatar.webp" });
  URL.createObjectURL = vi.fn(() => "blob:preview");
  URL.revokeObjectURL = vi.fn();
});

describe("AvatarEditor", () => {
  it("shows a preview with Save and Cancel and sends nothing yet (T-PF-21)", async () => {
    setup();
    pick(png());
    expect(await screen.findByRole("button", { name: a.save })).toBeVisible();
    expect(screen.getByRole("button", { name: a.cancel })).toBeVisible();
    expect(screen.getByText(a.previewLabel)).toBeVisible();
    expect(downscale).toHaveBeenCalledWith(expect.any(File), 256);
    expect(apiMock.put).not.toHaveBeenCalled();
  });

  it("blocks an svg, another type or a file over 10 MB with a toast (T-PF-22)", async () => {
    const toasts = watch();
    setup();
    pick(new File(["<svg/>"], "a.svg", { type: "image/svg+xml" }));
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", a.fileTypeError]),
    );
    const big = new File(["x"], "big.png", { type: "image/png" });
    Object.defineProperty(big, "size", { value: 10 * 1024 * 1024 + 1 });
    pick(big);
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", a.fileTooBigError]),
    );
    expect(downscale).not.toHaveBeenCalled();
    expect(screen.queryByRole("button", { name: a.save })).toBeNull();
  });

  it("saves once with the downscaled blob as part file and drops the preview (T-PF-23)", async () => {
    apiMock.put.mockResolvedValue({ avatarVersion: 7 });
    const toasts = watch();
    setup();
    pick(png());
    fireEvent.click(await screen.findByRole("button", { name: a.save }));

    await waitFor(() => expect(apiMock.put).toHaveBeenCalledTimes(1));
    const [path, form] = apiMock.put.mock.calls[0] as [string, FormData];
    expect(path).toBe(ME);
    expect((form.get("file") as File).name).toBe("avatar.webp");
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["success", a.uploaded]),
    );
    expect(screen.queryByRole("button", { name: a.save })).toBeNull();
  });

  it.each(["IDT-V30", "IDT-452", "IDT-511"])(
    "keeps the preview and toasts the message of %s when the upload fails (T-PF-24)",
    async (code) => {
      apiMock.put.mockRejectedValue(new ApiError(code, 400));
      const toasts = watch();
      setup();
      pick(png());
      fireEvent.click(await screen.findByRole("button", { name: a.save }));
      await waitFor(() =>
        expect(toasts().at(-1)).toEqual(["error", apiErrors[code]]),
      );
      expect(screen.getByRole("button", { name: a.save })).toBeVisible();
    },
  );

  it("drops the preview on Cancel with no request and no confirmation (T-PF-25)", async () => {
    setup();
    pick(png());
    fireEvent.click(await screen.findByRole("button", { name: a.cancel }));
    expect(screen.queryByRole("button", { name: a.save })).toBeNull();
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(apiMock.put).not.toHaveBeenCalled();
    expect(apiMock.delete).not.toHaveBeenCalled();
  });

  it("asks before removing and calls #75 only on confirm (T-PF-26)", async () => {
    apiMock.delete.mockResolvedValue(undefined);
    apiMock.getBlob.mockResolvedValue(new Blob(["x"]));
    const toasts = watch();
    setup(5);
    fireEvent.click(screen.getByRole("button", { name: a.remove }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: a.cancel,
      }),
    );
    expect(apiMock.delete).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: a.remove }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: a.removeConfirm,
      }),
    );
    await waitFor(() => expect(apiMock.delete).toHaveBeenCalledWith(ME));
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["success", a.removed]),
    );
  });

  it("has no Remove button and makes no request without a picture (T-PF-28)", () => {
    setup(null);
    expect(screen.queryByRole("button", { name: a.remove })).toBeNull();
    expect(apiMock.getBlob).not.toHaveBeenCalled();
  });

  it("treats a pending preview as unsaved and Save and leave uploads first (T-PF-29)", async () => {
    apiMock.put.mockResolvedValue({ avatarVersion: 9 });
    setup();
    pick(png());
    await screen.findByRole("button", { name: a.save });

    const link = clickLink();
    const dialog = await screen.findByRole("dialog");
    expect(within(dialog).getByText(a.leaveBody)).toBeVisible();
    fireEvent.click(
      within(dialog).getByRole("button", { name: a.saveAndLeave }),
    );
    await waitFor(() => expect(apiMock.put).toHaveBeenCalledTimes(1));
    link.remove();
  });

  it("stays on the page when Save and leave fails (T-PF-29)", async () => {
    apiMock.put.mockRejectedValue(new ApiError("IDT-511", 503));
    setup();
    pick(png());
    await screen.findByRole("button", { name: a.save });
    const link = clickLink();
    const dialog = await screen.findByRole("dialog");
    fireEvent.click(
      within(dialog).getByRole("button", { name: a.saveAndLeave }),
    );
    await waitFor(() => expect(apiMock.put).toHaveBeenCalled());
    await waitFor(() => expect(screen.queryByText(a.leaveBody)).toBeNull());
    expect(screen.getByRole("button", { name: a.save })).toBeVisible();
    link.remove();
  });
});

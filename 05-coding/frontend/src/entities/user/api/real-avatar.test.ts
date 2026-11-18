import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import {
  realGetUserAvatar,
  realRemoveMyAvatar,
  realRemoveUserAvatar,
  realUploadMyAvatar,
} from "./real-avatar";

beforeEach(resetApiMock);

describe("real avatar calls (F1 #74 to #77)", () => {
  it("uploads multipart with exactly one part named file", async () => {
    apiMock.put.mockResolvedValue({ avatarVersion: 4 });
    const blob = new Blob(["x"], { type: "image/jpeg" });
    expect(await realUploadMyAvatar(blob, "avatar.jpg")).toEqual({
      avatarVersion: 4,
    });
    const [path, form] = apiMock.put.mock.calls[0] as [string, FormData];
    expect(path).toBe("/identity/me/avatar");
    expect([...form.keys()]).toEqual(["file"]);
    expect((form.get("file") as File).name).toBe("avatar.jpg");
  });

  it("removes mine and another user's picture on their own paths", async () => {
    apiMock.delete.mockResolvedValue(undefined);
    await realRemoveMyAvatar();
    await realRemoveUserAvatar("u 2");
    expect(apiMock.delete).toHaveBeenNthCalledWith(1, "/identity/me/avatar");
    expect(apiMock.delete).toHaveBeenNthCalledWith(
      2,
      "/identity/admin/users/u%202/avatar",
    );
  });

  it("reads the bytes with the version as query", async () => {
    apiMock.getBlob.mockResolvedValue(new Blob(["x"]));
    await realGetUserAvatar("u1", 9);
    expect(apiMock.getBlob).toHaveBeenCalledWith("/identity/users/u1/avatar", {
      v: 9,
    });
  });
});

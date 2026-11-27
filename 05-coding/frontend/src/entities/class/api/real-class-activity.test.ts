import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import { ApiError } from "@/shared/api";
import { realListClassActivity } from "./real-class-activity";

describe("realListClassActivity", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("reads #45 without a classId and keeps the type and the actor for the screen", async () => {
    const recent = new Date(Date.now() - 5 * 60_000).toISOString();
    get.mockResolvedValue({
      items: [
        {
          id: "a1",
          activityType: "SUBMISSION_ACCEPTED",
          actorUserId: "u1",
          actorDisplayName: "Nguyễn Văn An",
          scopeClassId: "c1",
          refType: "SUBMISSION",
          refId: "s1",
          occurredAt: recent,
        },
      ],
    });

    const feed = await realListClassActivity();

    expect(get).toHaveBeenCalledWith(
      "/identity/instructor/dashboard/recent-activity",
    );
    expect(feed).toEqual([
      {
        id: "a1",
        activityType: "SUBMISSION_ACCEPTED",
        actorName: "Nguyễn Văn An",
        text: "Nguyễn Văn An",
        timeLabel: "5 phút trước",
        attention: false,
      },
    ]);
  });

  it("an empty feed stays empty", async () => {
    get.mockResolvedValue({ items: [] });

    expect(await realListClassActivity()).toEqual([]);
  });

  it("rethrows a server error such as the 403 of a missing permission", async () => {
    get.mockRejectedValue(new ApiError("IDT-445", 403, "no"));

    await expect(realListClassActivity()).rejects.toMatchObject({
      code: "IDT-445",
    });
  });
});

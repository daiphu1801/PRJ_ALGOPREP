import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const put = vi.fn();
const download = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    put: (...args: unknown[]) => put(...args),
    download: (...args: unknown[]) => download(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realExportAuditLogs,
  realFetchActiveAdmins,
  realFetchAuditLogs,
  realFetchCategoryBreakdown,
  realFetchLogRetention,
  realSaveLogRetention,
  toAuditEvent,
} from "./real-audit-log";

const BASE = "/identity/admin/audit-logs";
const NONE = { query: "", category: "all", from: "", to: "" } as const;

const dto = {
  id: "9f2a53c1-0000-4000-8000-000000000001",
  createdAt: "2026-11-01T17:30:05Z",
  actorDisplayName: "Phú Đại",
  actorEmail: "dai@example.com",
  actionType: "USER_LOCKED",
  categoryCode: "PERMISSION",
  serviceCode: "permission",
  targetType: "USER",
  targetId: "u1",
  safeParams: { status: "DEACTIVATED", reason: "Đăng ký hàng loạt" },
};

describe("toAuditEvent (#39 item)", () => {
  it("reads the time in UTC+7, shortens the id and keeps the action for the sentence", () => {
    expect(toAuditEvent(dto)).toEqual({
      id: dto.id,
      shortId: "9f2a53c",
      // 17:30 UTC is already the next day in Vietnam.
      date: "2026-11-02",
      time: "00:30:05",
      category: "permission",
      action: { type: "USER_LOCKED", params: dto.safeParams },
      service: "permission",
      actor: "Phú Đại",
      actorEmail: "dai@example.com",
      reason: "Đăng ký hàng loạt",
    });
  });

  it("gives a reason only to a lock row and falls back to config for an unknown group", () => {
    const unlock = toAuditEvent({
      ...dto,
      actionType: "USER_UNLOCKED",
      safeParams: { status: "ACTIVE" },
    });
    expect(unlock.reason).toBeUndefined();
    expect(
      toAuditEvent({ ...dto, categoryCode: "NEW_GROUP", safeParams: null })
        .category,
    ).toBe("config");
  });
});

describe("realFetchAuditLogs (#39)", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("sends only the filters that are set, the category in upper case, and the cursor", async () => {
    get.mockResolvedValue({ items: [dto], nextCursor: "c2" });

    const page = await realFetchAuditLogs(
      { query: "  pdai ", category: "auth", from: "2026-10-01", to: "" },
      "c1",
    );

    expect(get).toHaveBeenCalledWith(BASE, {
      q: "pdai",
      categoryCode: "AUTH",
      dateFrom: "2026-10-01",
      cursor: "c1",
    });
    expect(page.nextCursor).toBe("c2");
    expect(page.events).toHaveLength(1);
  });

  it("sends nothing for the open filters and the first page", async () => {
    get.mockResolvedValue({ items: [], nextCursor: null });
    await realFetchAuditLogs({ ...NONE }, null);
    expect(get).toHaveBeenCalledWith(BASE, {});
  });

  it("rethrows the backend code of a refused filter", async () => {
    get.mockRejectedValue(new ApiError("IDT-443", 400));
    await expect(realFetchAuditLogs({ ...NONE }, null)).rejects.toMatchObject({
      code: "IDT-443",
    });
  });
});

describe("side blocks (#64, #65)", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("maps the active admins to a name, a role with the count, and a relative time", async () => {
    get.mockResolvedValue({
      items: [
        {
          actorUserId: "a1",
          displayName: "Phú Đại",
          roleName: "Quản trị",
          actionCount: 28,
          lastActionAt: new Date(Date.now() - 2 * 60_000).toISOString(),
        },
      ],
    });
    expect(await realFetchActiveAdmins()).toEqual([
      {
        id: "a1",
        name: "Phú Đại",
        meta: "Quản trị · 28 hành động",
        lastActionAt: "2 phút trước",
      },
    ]);
    expect(get).toHaveBeenCalledWith(`${BASE}/active-admins`);
  });

  it("maps the four groups, zeros kept", async () => {
    get.mockResolvedValue({
      items: [
        { categoryCode: "AUTH", count: 22 },
        { categoryCode: "CONTENT", count: 0 },
      ],
    });
    expect(await realFetchCategoryBreakdown()).toEqual([
      { category: "auth", count: 22 },
      { category: "content", count: 0 },
    ]);
  });
});

describe("realExportAuditLogs (#40)", () => {
  it("asks for the chosen columns with the filters and the language, and returns the file", async () => {
    const file = new Blob(["a,b"]);
    download.mockResolvedValue(file);

    const result = await realExportAuditLogs({
      params: { query: "pdai", category: "config", from: "", to: "2026-11-01" },
      columns: ["time", "content"],
      locale: "en",
    });

    expect(result).toBe(file);
    expect(download).toHaveBeenCalledWith(`${BASE}/export`, {
      params: {
        q: "pdai",
        categoryCode: "CONFIG",
        dateTo: "2026-11-01",
        columns: ["time", "content"],
        format: "csv",
      },
      headers: { "Accept-Language": "en" },
    });
  });
});

describe("log retention (#51, #52)", () => {
  beforeEach(() => {
    get.mockReset();
    put.mockReset();
  });

  it("reads and writes the policy in the server spelling and returns what is stored", async () => {
    get.mockResolvedValue({ retentionDays: 90, expiredPolicy: "DELETE" });
    expect(await realFetchLogRetention()).toEqual({
      retentionDays: 90,
      expiry: "delete",
    });

    put.mockResolvedValue({ retentionDays: 30, expiredPolicy: "ARCHIVE" });
    expect(
      await realSaveLogRetention({ retentionDays: 30, expiry: "archive" }),
    ).toEqual({ retentionDays: 30, expiry: "archive" });
    expect(put).toHaveBeenCalledWith("/identity/admin/settings/log-retention", {
      retentionDays: 30,
      expiredPolicy: "ARCHIVE",
    });
  });

  it("rethrows a refusal (no SYSTEM_AUDIT_LOG cell)", async () => {
    put.mockRejectedValue(new ApiError("IDT-445", 403));
    await expect(
      realSaveLogRetention({ retentionDays: 30, expiry: "delete" }),
    ).rejects.toMatchObject({ code: "IDT-445" });
  });
});

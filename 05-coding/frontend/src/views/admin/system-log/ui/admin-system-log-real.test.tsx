// View-level tests of ADM0403 against the REAL api path (03-dd/api/F1_identity.md #39, #40, #51, #52,
// #64 to #66): only the HTTP layer and `fetch` (the live stream) are faked. What matters: the server
// does the filtering and the paging, a row reads as a sentence built from its action type, a refusal
// reads as its error code and keeps the form, and the file is the server's.
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
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { AdminSystemLogView } from "./admin-system-log-view";

const m = viMessages.adminSystemLog;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const BASE = "/identity/admin/audit-logs";
const RETENTION = "/identity/admin/settings/log-retention";

const item = (n: number, over: Record<string, unknown> = {}) => ({
  id: `${n}abcdef0-aaaa-4000-8000-000000000000`,
  createdAt: "2026-11-02T03:00:00Z",
  actorDisplayName: "Phú Đại",
  actorEmail: "dai@example.com",
  actionType: "ROLE_CREATED",
  categoryCode: "PERMISSION",
  serviceCode: "permission",
  targetType: "ROLE",
  targetId: "r1",
  safeParams: { name: `Vai ${n}` },
  ...over,
});

type Serve = {
  pages?: Record<string, { items: unknown[]; nextCursor: string | null }>;
  retention?: { retentionDays: number; expiredPolicy: string };
};

function serve({ pages, retention }: Serve = {}) {
  const byCursor = pages ?? { first: { items: [item(1)], nextCursor: null } };
  apiMock.get.mockImplementation(
    (path: string, params?: Record<string, string>) => {
      if (path === BASE) {
        return Promise.resolve(byCursor[params?.cursor ?? "first"]);
      }
      if (path === `${BASE}/active-admins`) {
        return Promise.resolve({
          items: [
            {
              displayName: "Phú Đại",
              roleName: "Quản trị",
              actionCount: 3,
              lastActionAt: new Date().toISOString(),
            },
          ],
        });
      }
      if (path === `${BASE}/category-breakdown`) {
        return Promise.resolve({
          items: [{ categoryCode: "AUTH", count: 4 }],
        });
      }
      if (path === RETENTION) {
        return Promise.resolve(
          retention ?? { retentionDays: 90, expiredPolicy: "DELETE" },
        );
      }
      return Promise.reject(new Error(`unexpected GET ${path}`));
    },
  );
}

const listCalls = () =>
  apiMock.get.mock.calls.filter(([path]) => path === BASE) as [
    string,
    Record<string, string>,
  ][];

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () =>
    toasts.result.current.map((entry) => [entry.tone, entry.message]);
}

async function open() {
  render(withTestProviders(<AdminSystemLogView />));
  await screen.findByRole("list", { name: m.eventListLabel });
}

describe("AdminSystemLogView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    // The live stream is on by default; here it never answers, so it stays out of the way.
    vi.stubGlobal("fetch", () => new Promise(() => {}));
    serve();
  });

  it("builds each row's sentence from the action type and shows the short code", async () => {
    serve({
      pages: {
        first: {
          items: [
            item(1),
            item(2, {
              actionType: "ROLE_PERMISSION_CHANGED",
              safeParams: {
                function: "USER_MANAGEMENT",
                action: "UPDATE",
                granted: true,
              },
            }),
            item(3, {
              actionType: "USER_LOCKED",
              categoryCode: "PERMISSION",
              safeParams: {
                status: "DEACTIVATED",
                reason: "Đăng ký hàng loạt",
              },
            }),
            item(4, { actionType: "SOMETHING_NEW", safeParams: {} }),
          ],
          nextCursor: null,
        },
      },
    });
    await open();

    expect(screen.getByText("Tạo vai trò Vai 1")).toBeInTheDocument();
    expect(
      screen.getByText("Cấp quyền UPDATE trên USER_MANAGEMENT"),
    ).toBeInTheDocument();
    expect(screen.getByText("Khoá một tài khoản")).toBeInTheDocument();
    expect(
      screen.getByText(`${m.reasonLabel}: Đăng ký hàng loạt`),
    ).toBeInTheDocument();
    // An action the screen has no sentence for still shows, by its code.
    expect(screen.getByText("Hành động SOMETHING_NEW")).toBeInTheDocument();
    expect(screen.getByText("1abcdef")).toBeInTheDocument();
    expect(screen.getByText("Quản trị · 3 hành động")).toBeInTheDocument();
  });

  it("asks the server for the typed text after a pause and for the chosen day range", async () => {
    await open();
    expect(listCalls().at(-1)?.[1]).toEqual({});

    fireEvent.change(screen.getByLabelText(m.searchLabel), {
      target: { value: "pdai" },
    });
    await waitFor(() => expect(listCalls().at(-1)?.[1]).toEqual({ q: "pdai" }));

    fireEvent.change(screen.getByLabelText(m.dateFrom), {
      target: { value: "2026-10-01" },
    });
    await waitFor(() =>
      expect(listCalls().at(-1)?.[1]).toEqual({
        q: "pdai",
        dateFrom: "2026-10-01",
      }),
    );
  });

  it("does not ask for a reversed range and says nothing matches", async () => {
    await open();
    const before = listCalls().length;

    fireEvent.change(screen.getByLabelText(m.dateTo), {
      target: { value: "2026-10-01" },
    });
    fireEvent.change(screen.getByLabelText(m.dateFrom), {
      target: { value: "2026-10-05" },
    });

    expect(screen.getByText(m.emptyFiltered)).toBeInTheDocument();
    expect(listCalls().length).toBe(before + 1);
  });

  it("loads the next page with the cursor and keeps the rows when that fails, then retries it", async () => {
    serve({
      pages: {
        first: { items: [item(1)], nextCursor: "c2" },
        c2: { items: [item(2)], nextCursor: null },
      },
    });
    await open();
    expect(screen.getAllByText(/^Tạo vai trò/)).toHaveLength(1);

    // The next page fails once.
    const original = apiMock.get.getMockImplementation()!;
    apiMock.get.mockImplementation(
      (path: string, params?: { cursor?: string }) =>
        path === BASE && params?.cursor
          ? Promise.reject(new ApiError("UNKNOWN_ERROR", 500))
          : original(path, params),
    );
    fireEvent.click(screen.getByRole("button", { name: `Tải thêm 50 dòng` }));
    expect(await screen.findByText(m.loadMoreFailed)).toBeInTheDocument();
    expect(screen.getAllByText(/^Tạo vai trò/)).toHaveLength(1);

    apiMock.get.mockImplementation(original);
    fireEvent.click(screen.getByRole("button", { name: m.retry }));
    await waitFor(() =>
      expect(screen.getAllByText(/^Tạo vai trò/)).toHaveLength(2),
    );
    expect(listCalls().at(-1)?.[1]).toEqual({ cursor: "c2" });
  });

  it("saves the retention by the server spelling, warns on a shorter period, and keeps the dialog on a refusal", async () => {
    apiMock.put.mockRejectedValueOnce(new ApiError("IDT-445", 403));
    apiMock.put.mockResolvedValueOnce({
      retentionDays: 30,
      expiredPolicy: "DELETE",
    });
    const toasts = watch();
    await open();
    await screen.findByText(m.retention.footer.replace("{days}", "90"));

    fireEvent.click(screen.getByRole("button", { name: m.retention.open }));
    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText(/Thời hạn lưu/), {
      target: { value: "30" },
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: m.retention.save }),
    );
    // Shortening asks a second time before anything is sent.
    expect(
      await within(dialog).findByText(
        m.retention.confirmShorter.replace("{days}", "30"),
      ),
    ).toBeInTheDocument();
    expect(apiMock.put).not.toHaveBeenCalled();

    fireEvent.click(
      within(dialog).getByRole("button", { name: m.retention.confirm }),
    );
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-445"]]),
    );
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(
      screen.getByText(m.retention.footer.replace("{days}", "90")),
    ).toBeInTheDocument();

    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: m.retention.confirm,
      }),
    );
    await waitFor(() =>
      expect(
        screen.getByText(m.retention.footer.replace("{days}", "30")),
      ).toBeInTheDocument(),
    );
    expect(apiMock.put).toHaveBeenLastCalledWith(RETENTION, {
      retentionDays: 30,
      expiredPolicy: "DELETE",
    });
  });

  it("downloads the server's CSV with the screen's filters, and keeps the dialog on a refusal", async () => {
    const create = vi.fn(() => "blob:csv");
    vi.stubGlobal("URL", { createObjectURL: create, revokeObjectURL: vi.fn() });
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(() => {});
    apiMock.download.mockRejectedValueOnce(new ApiError("IDT-443", 400));
    apiMock.download.mockResolvedValueOnce(new Blob(["a,b"]));
    const toasts = watch();
    await open();
    await screen.findByText(m.retention.footer.replace("{days}", "90"));

    fireEvent.change(screen.getByLabelText(m.searchLabel), {
      target: { value: "pdai" },
    });
    fireEvent.click(screen.getByRole("button", { name: m.export }));
    const dialog = await screen.findByRole("dialog");
    fireEvent.click(within(dialog).getByLabelText(m.exportDialog.col.id));
    fireEvent.click(
      within(dialog).getByRole("button", { name: m.exportDialog.submit }),
    );

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-443"]]),
    );
    expect(click).not.toHaveBeenCalled();
    expect(screen.getByRole("dialog")).toBeInTheDocument();

    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: m.exportDialog.submit,
      }),
    );
    await waitFor(() => expect(click).toHaveBeenCalledTimes(1));
    expect(apiMock.download).toHaveBeenLastCalledWith(`${BASE}/export`, {
      params: {
        q: "pdai",
        columns: ["time", "actor", "service", "category", "content"],
        format: "csv",
      },
      headers: { "Accept-Language": "vi" },
    });
    click.mockRestore();
    vi.unstubAllGlobals();
  });
});

describe("the live feed", () => {
  const encoder = new TextEncoder();

  beforeEach(() => {
    resetApiMock();
    serve();
  });

  it("puts a new row at the top, once", async () => {
    const live = item(9, { safeParams: { name: "Vai live" } });
    const frame = (row: ReturnType<typeof item>) =>
      `id: ${row.id}\nevent: audit-log\ndata: ${JSON.stringify(row)}\n\n`;
    let release: () => void = () => {};
    const gate = new Promise<void>((resolve) => (release = resolve));
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          new ReadableStream({
            async start(controller) {
              // The stream speaks only after the first page is on screen.
              await gate;
              controller.enqueue(encoder.encode(frame(live) + frame(live)));
            },
          }),
          { status: 200, headers: { "Content-Type": "text/event-stream" } },
        ),
      ),
    );
    await open();
    release();

    expect(await screen.findByText("Tạo vai trò Vai live")).toBeInTheDocument();
    expect(screen.getAllByText("Tạo vai trò Vai live")).toHaveLength(1);
    // The row sits above the one the page loaded.
    const names = screen
      .getAllByText(/^Tạo vai trò/)
      .map((node) => node.textContent);
    expect(names[0]).toBe("Tạo vai trò Vai live");
  });

  it("does not add a row from another category when that category is filtered out", async () => {
    const other = item(8, {
      categoryCode: "AUTH",
      actionType: "PASSWORD_CHANGED",
      safeParams: {},
    });
    let release: () => void = () => {};
    const gate = new Promise<void>((resolve) => (release = resolve));
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          new ReadableStream({
            async start(controller) {
              await gate;
              controller.enqueue(
                encoder.encode(
                  `id: ${other.id}\nevent: audit-log\ndata: ${JSON.stringify(other)}\n\n`,
                ),
              );
            },
          }),
          { status: 200, headers: { "Content-Type": "text/event-stream" } },
        ),
      ),
    );
    await open();
    // Choose "Cấu hình": an AUTH row no longer belongs to the list.
    fireEvent.click(
      screen.getByRole("button", { name: new RegExp(m.categoryFilterLabel) }),
    );
    fireEvent.click(
      await screen.findByRole("option", { name: m.category.config }),
    );
    await waitFor(() =>
      expect(listCalls().at(-1)?.[1]).toEqual({ categoryCode: "CONFIG" }),
    );

    release();
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 50));
    });
    expect(screen.queryByText("Người dùng đổi mật khẩu")).toBeNull();
  });

  it("reloads the list when the server sends resync", async () => {
    let release: () => void = () => {};
    const gate = new Promise<void>((resolve) => (release = resolve));
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          new ReadableStream({
            async start(controller) {
              await gate;
              controller.enqueue(encoder.encode("event: resync\n\n"));
            },
          }),
          { status: 200, headers: { "Content-Type": "text/event-stream" } },
        ),
      ),
    );
    await open();
    const before = listCalls().length;

    release();

    await waitFor(() => expect(listCalls().length).toBeGreaterThan(before));
  });

  it("says why when the server refuses the stream", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ errorCode: "IDT-449" }), {
          status: 429,
        }),
      ),
    );
    const toasts = watch();
    await open();
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-449"]]),
    );
  });
});

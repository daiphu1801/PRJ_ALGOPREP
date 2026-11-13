// ADM0301 against the REAL api path (F5 #14 to #17): only the HTTP layer is faked, so the real
// modules, the AIR-* error mapping and the real vi texts are what is asserted.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
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
// `useMockQuery` reads the flag from `mock-mode` directly, not through the barrel mocked above.
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => "/admin/ai-config",
}));

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { AdminAiConfigView } from "./admin-ai-config-view";

const m = viMessages.adminAiConfig;
const errors = viMessages.apiErrors as Record<string, string>;
const BASE = "/ai/admin/prompt-templates";
const guards = { citeLines: true, answerInVietnamese: true, noFullSolution: true };

const row = (feature: string) => ({
  id: `active-${feature}`,
  featureCode: feature,
  version: "v3.8",
  status: "ACTIVE",
  systemInstruction: `live ${feature}`,
  modelId: "claude-sonnet",
  responseGuards: feature === "GENERATE_TESTCASE" ? null : guards,
  responseSchemaVersion: "1",
  maxTokens: 1600,
  publishedAt: "2026-08-11T05:00:00Z",
  createdAt: "2026-08-10T00:00:00Z",
});

const listDto = {
  items: ["SOLUTION_REVIEW", "MOCK_INTERVIEW", "GENERATE_TESTCASE"].map(row),
  modelOptions: ["claude-sonnet", "claude-haiku"],
};

function serve() {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === BASE) return listDto;
    if (path.endsWith("/history")) return { items: [] };
    throw new Error(`unexpected GET ${path}`);
  });
  apiMock.put.mockResolvedValue({});
  apiMock.post.mockResolvedValue(listDto);
}

const listReads = () =>
  apiMock.get.mock.calls.filter((call) => call[0] === BASE).length;

let toasts: { current: readonly { tone: string; message: string }[] };

async function open() {
  render(withTestProviders(<AdminAiConfigView />));
  await screen.findByText("v3.8");
}

const lastToast = () => toasts.current.at(-1)?.message;

beforeEach(() => {
  resetApiMock();
  serve();
  toasts = renderHook(() => useToasts()).result;
  act(() => toast.clear());
});

/** One guard toggle on the review tab: a pending change to publish. */
function makeChange() {
  fireEvent.click(
    screen.getByRole("switch", { name: m.guard.citeLines.label }),
  );
}

async function publishNow() {
  fireEvent.click(screen.getByRole("button", { name: m.publish }));
  fireEvent.click(
    await screen.findByRole("button", { name: m.publishDialog.confirm }),
  );
}

async function saveDraftNow(text: string) {
  fireEvent.click(screen.getByRole("button", { name: m.editPrompt }));
  fireEvent.change(screen.getByLabelText(m.editDialog.label), {
    target: { value: text },
  });
  fireEvent.click(screen.getByRole("button", { name: m.editDialog.save }));
}

describe("AdminAiConfigView load (#14, #15)", () => {
  it("shows the live prompt read from the server", async () => {
    await open();
    expect(screen.getByText("claude-sonnet")).toBeInTheDocument();
    expect(screen.getByText("1.600 token")).toBeInTheDocument();
    expect(apiMock.get).toHaveBeenCalledWith(BASE);
  });

  it("shows a stored server draft as pending work", async () => {
    apiMock.get.mockImplementation(async (path: string) =>
      path === BASE
        ? {
            ...listDto,
            items: [
              ...listDto.items,
              {
                ...row("SOLUTION_REVIEW"),
                id: "draft-review",
                status: "DRAFT",
                systemInstruction: "stored draft",
              },
            ],
          }
        : { items: [] },
    );
    await open();
    expect(screen.getByText(/1 thay đổi chưa phát hành/)).toBeInTheDocument();
  });

  it.each(["AIR-301", "AIR-V11", "AIR-503"])(
    "shows the %s text from apiErrors, never the server text",
    async (code) => {
      apiMock.get.mockRejectedValue(new ApiError(code, 403, "RAW SERVER TEXT"));
      render(withTestProviders(<AdminAiConfigView />));
      expect(await screen.findByText(errors[code]!)).toBeInTheDocument();
      expect(screen.queryByText("RAW SERVER TEXT")).toBeNull();
    },
  );
});

describe("AdminAiConfigView save draft (#16)", () => {
  it("sends the draft, toasts success and refetches the page", async () => {
    await open();
    const before = listReads();
    await saveDraftNow("Chi thi moi");
    await waitFor(() => expect(lastToast()).toBe(m.editDialog.saved));
    expect(apiMock.put).toHaveBeenCalledWith(`${BASE}/SOLUTION_REVIEW/draft`, {
      systemInstruction: "Chi thi moi",
      modelId: "claude-sonnet",
      responseGuards: guards,
    });
    await waitFor(() => expect(listReads()).toBeGreaterThan(before));
    expect(screen.getByText(/1 thay đổi chưa phát hành/)).toBeInTheDocument();
  });

  it("an empty instruction is refused on the client: no request", async () => {
    await open();
    await saveDraftNow("   ");
    expect(lastToast()).toBe(m.editDialog.errorEmpty);
    expect(apiMock.put).not.toHaveBeenCalled();
  });

  it.each([
    ["AIR-V11", 400],
    ["AIR-V12", 400],
    ["AIR-V13", 400],
    ["AIR-202", 422],
    ["AIR-301", 403],
  ])("%s: error toast from apiErrors, popup stays, no draft change", async (code, status) => {
    apiMock.put.mockRejectedValue(new ApiError(code, status, "RAW"));
    await open();
    await saveDraftNow("Chi thi moi");
    await waitFor(() => expect(lastToast()).toBe(errors[code]));
    expect(screen.getByLabelText(m.editDialog.label)).toBeInTheDocument();
    expect(toasts.current.some((t) => t.message === m.editDialog.saved)).toBe(
      false,
    );
  });

  it("an unknown failure falls back to UNKNOWN_ERROR", async () => {
    apiMock.put.mockRejectedValue(new Error("boom"));
    await open();
    await saveDraftNow("Chi thi moi");
    await waitFor(() => expect(lastToast()).toBe(errors.UNKNOWN_ERROR));
  });
});

describe("AdminAiConfigView publish (#17)", () => {
  it("publishes the pending change, toasts success, clears the pending state and refetches", async () => {
    await open();
    const before = listReads();
    makeChange();
    await publishNow();
    await waitFor(() => expect(lastToast()).toBe(m.publishDone));
    expect(apiMock.post).toHaveBeenCalledWith(`${BASE}/publish`, {
      changes: [
        {
          featureCode: "SOLUTION_REVIEW",
          expectedActivePromptId: "active-SOLUTION_REVIEW",
          systemInstruction: "live SOLUTION_REVIEW",
          modelId: "claude-sonnet",
          responseGuards: { ...guards, citeLines: false },
        },
      ],
    });
    expect(screen.queryByText(/thay đổi chưa phát hành/)).toBeNull();
    await waitFor(() => expect(listReads()).toBeGreaterThan(before));
  });

  it("nothing pending: no request", async () => {
    await open();
    fireEvent.click(screen.getByRole("button", { name: m.publish }));
    expect(lastToast()).toBe(m.noChanges);
    expect(apiMock.post).not.toHaveBeenCalled();
  });

  it.each([
    ["AIR-V11", 400],
    ["AIR-V12", 400],
    ["AIR-V13", 400],
    ["AIR-V14", 400],
    ["AIR-202", 422],
    ["AIR-205", 422],
    ["AIR-301", 403],
    ["AIR-503", 503],
  ])("%s: error toast from apiErrors, dialog closes, the change stays pending", async (code, status) => {
    apiMock.post.mockRejectedValue(new ApiError(code, status, "RAW"));
    await open();
    makeChange();
    await publishNow();
    await waitFor(() => expect(lastToast()).toBe(errors[code]));
    expect(toasts.current.some((t) => t.message === m.publishDone)).toBe(false);
    expect(screen.getByText(/1 thay đổi chưa phát hành/)).toBeInTheDocument();
    await waitFor(() =>
      expect(
        screen.queryByRole("button", { name: m.publishDialog.confirm }),
      ).toBeNull(),
    );
  });

  it("AIR-201: tells the admin and reloads the page, dropping the stale local copy", async () => {
    apiMock.post.mockRejectedValue(new ApiError("AIR-201", 409, "RAW"));
    await open();
    const before = listReads();
    makeChange();
    await publishNow();
    await waitFor(() => expect(lastToast()).toBe(errors["AIR-201"]));
    await waitFor(() => expect(listReads()).toBeGreaterThan(before));
    await waitFor(() =>
      expect(screen.queryByText(/thay đổi chưa phát hành/)).toBeNull(),
    );
  });

  it("an unknown failure falls back to UNKNOWN_ERROR", async () => {
    apiMock.post.mockRejectedValue(new Error("boom"));
    await open();
    makeChange();
    await publishNow();
    await waitFor(() => expect(lastToast()).toBe(errors.UNKNOWN_ERROR));
  });
});

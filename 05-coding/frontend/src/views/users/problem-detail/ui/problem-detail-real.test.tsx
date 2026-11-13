// USR0102 bookmark button against the REAL api path (03-dd/api/F2_problem-bank.md #11): only the HTTP
// layer is faked. The flag flips after the server confirmed the wanted state; a failure keeps it and
// shows the translated code. The workspace read itself is covered in entities/problem/api.
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
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
vi.mock("@/shared/ui/data/code-editor", () => ({
  CodeEditor: () => <textarea aria-label="editor" />,
}));
const push = vi.fn();
vi.mock("next/navigation", () => ({
  useParams: () => ({ problemId: "p1" }),
  useRouter: () => ({ push }),
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ProblemDetailView } from "./problem-detail-view";

const d = viMessages.problemDetail;
const apiErrors = viMessages.apiErrors as Record<string, string>;

function route(bookmarked: boolean) {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path.endsWith("/workspace"))
      return {
        id: "p1",
        code: "P-1",
        title: "Two Sum",
        levelCode: "EASY",
        topicNames: [],
        statementMd: "S",
        constraintsMd: "",
        functionWrapperSupported: true,
        acRate: null,
        bookmarked,
        workedExamples: [],
      };
    if (path.endsWith("/overview")) return { hiddenTestcaseCount: 0 };
    if (path.endsWith("/sample-testcases")) return { items: [] };
    if (path.endsWith("/execution-limits"))
      return {
        limits: ["JAVA", "CPP", "PYTHON"].map((language) => ({
          language,
          timeLimitMs: 1000,
          memoryLimitMb: 256,
        })),
      };
    if (path.endsWith("/languages/enabled"))
      return { items: ["JAVA", "CPP", "PYTHON"] };
    if (path.endsWith("/starter-code"))
      return {
        language: "JAVA",
        submissionModel: "FUNCTION_WRAPPER",
        sourceCode: "",
      };
    return { items: [] };
  });
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

describe("ProblemDetailView bookmark with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    push.mockReset();
    window.localStorage.clear();
  });

  it("saves with the wanted state and flips after the answer", async () => {
    route(false);
    apiMock.put.mockResolvedValue({ bookmarked: true });
    const toasts = watch();
    render(withTestProviders(<ProblemDetailView />));
    fireEvent.click(
      await screen.findByRole("button", { name: d.panel.btnSave }),
    );

    await waitFor(() =>
      expect(apiMock.put).toHaveBeenCalledWith(
        "/problem-bank/me/bookmarks/p1/state",
        { bookmarked: true },
      ),
    );
    expect(
      await screen.findByRole("button", { name: d.panel.btnUnsave }),
    ).toHaveAttribute("aria-pressed", "true");
    expect(toasts().at(-1)).toEqual(["success", d.toast.saved]);
  });

  it("removes with false and says so", async () => {
    route(true);
    apiMock.put.mockResolvedValue({ bookmarked: false });
    const toasts = watch();
    render(withTestProviders(<ProblemDetailView />));
    fireEvent.click(
      await screen.findByRole("button", { name: d.panel.btnUnsave }),
    );

    await waitFor(() =>
      expect(apiMock.put).toHaveBeenCalledWith(
        "/problem-bank/me/bookmarks/p1/state",
        { bookmarked: false },
      ),
    );
    await screen.findByRole("button", { name: d.panel.btnSave });
    expect(toasts().at(-1)).toEqual(["success", d.toast.unsaved]);
  });

  it("keeps the flag and toasts the PRB-010 text on a refusal", async () => {
    route(false);
    apiMock.put.mockRejectedValue(new ApiError("PRB-010", 404));
    const toasts = watch();
    render(withTestProviders(<ProblemDetailView />));
    fireEvent.click(
      await screen.findByRole("button", { name: d.panel.btnSave }),
    );

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["PRB-010"]]),
    );
    expect(
      screen.getByRole("button", { name: d.panel.btnSave }),
    ).toHaveAttribute("aria-pressed", "false");
  });
});

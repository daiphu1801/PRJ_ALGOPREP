// PD-AC-14 / F4-14: queue hint while the tracked Submit is PENDING, against the real api path with
// only HTTP and the STOMP channel faked.
import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
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
let socket: { message: (value: unknown) => void };
vi.mock("@/shared/api/stomp", () => ({
  subscribeJudge: (_destination: string, callbacks: typeof socket) => {
    socket = callbacks;
    return () => {};
  },
}));
vi.mock("next/navigation", () => ({
  useParams: () => ({ problemId: "p1" }),
  useRouter: () => ({ push: vi.fn() }),
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ProblemDetailView } from "./problem-detail-view";

let progress: Record<string, unknown>;
function route() {
  apiMock.post.mockResolvedValue({
    submissionId: "s1",
    status: "PENDING",
    totalCountHint: 5,
  });
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/judge/submissions/s1/progress") return progress;
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
        bookmarked: false,
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
        sourceCode: "class Solution {}",
      };
    return { items: [] };
  });
}
const base = {
  submissionId: "s1",
  seq: 1,
  isFinal: false,
  totalCount: 5,
  completedCount: 0,
  passedCount: 0,
  testcases: [],
};

describe("ProblemDetailView queue notice (PD-AC-14)", () => {
  beforeEach(() => {
    resetApiMock();
    window.localStorage.clear();
    progress = {
      ...base,
      status: "PENDING",
      queue: { ahead: 18, estimatedWaitSeconds: 9 },
    };
    route();
  });

  async function submitCode() {
    const button = await screen.findByRole("button", { name: "Submit" });
    await vi.waitFor(() => expect(button).toBeEnabled());
    fireEvent.click(button);
  }

  it("shows the queue line while PENDING and hides it once judging starts", async () => {
    render(withTestProviders(<ProblemDetailView />));
    await submitCode();

    expect(
      await screen.findByText(
        "Đang chờ chấm: phía trước còn 18 bài, ước tính khoảng 9 giây",
      ),
    ).toBeInTheDocument();

    progress = { ...base, seq: 2, status: "JUDGING", queue: null };
    await act(async () =>
      socket.message({ submissionId: "s1", seq: 2, isFinal: false }),
    );
    await vi.waitFor(() =>
      expect(screen.queryByText(/Đang chờ chấm/)).not.toBeInTheDocument(),
    );
  });

  it("does not report untouched testcases as failed while PENDING (F-01)", async () => {
    render(withTestProviders(<ProblemDetailView />));
    await submitCode();
    await screen.findByText(/Đang chờ chấm/);

    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    await vi.waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
    expect(screen.getByText(/0 đạt/)).toBeInTheDocument();
    expect(screen.queryByText(/không đạt/)).not.toBeInTheDocument();
    // The status badge stays neutral until a final verdict exists (not the red negative style).
    const badge = screen.getByText(/Đang chờ · 0\/\d+/);
    expect(badge.className).toContain("surface-hover");
    expect(badge.className).not.toContain("admin-negative");
  });

  it("shows only the count when the estimate is null", async () => {
    progress = {
      ...base,
      status: "PENDING",
      queue: { ahead: 4, estimatedWaitSeconds: null },
    };
    render(withTestProviders(<ProblemDetailView />));
    await submitCode();

    expect(
      await screen.findByText("Đang chờ chấm: phía trước còn 4 bài"),
    ).toBeInTheDocument();
  });

  it("shows no queue line for a snapshot that is already COMPILING", async () => {
    progress = { ...base, status: "COMPILING", queue: null };
    render(withTestProviders(<ProblemDetailView />));
    await submitCode();

    await vi.waitFor(() => expect(apiMock.get).toHaveBeenCalledWith("/judge/submissions/s1/progress"));
    expect(screen.queryByText(/Đang chờ chấm/)).not.toBeInTheDocument();
  });
});

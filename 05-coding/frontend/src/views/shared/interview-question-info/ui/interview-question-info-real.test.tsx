// SHR0303 against the REAL api path (F6 #20): only the HTTP layer is faked. Mock-data behaviour stays in
// interview-question-info-view.test.tsx.
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
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
// The topic and level lists belong to another slice; this screen only reads them.
vi.mock("@/entities/interview-question", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  useInterviewTopics: () => [
    {
      id: "t-2",
      key: "CS_THEORY",
      label: "Lý thuyết CS",
      usesStarFramework: false,
    },
  ],
  useInterviewLevels: () => [
    { id: "l-2", key: "MEDIUM", label: "Trung bình", tone: "warn" },
  ],
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { InterviewQuestionInfoView } from "./interview-question-info-view";

const m = viMessages.interviewQuestionInfo;
const ta = viMessages.interviewQuestionAuthoring;
const BASE = "/interview-bank/admin/questions";
const Q = "11111111-1111-4111-8111-111111111111";

const detail = (over: Record<string, unknown> = {}) => ({
  id: Q,
  displayNo: 7,
  topicId: "t-2",
  levelId: "l-2",
  title: "Hash table xử lý collision thế nào?",
  contentMarkdown: "Mô tả thêm của câu hỏi",
  followUpQuestions: ["Load factor?"],
  rubric: [
    { criterionCode: "Định nghĩa", description: null, weightPercent: 100 },
  ],
  status: "ACTIVE",
  practiceAvailable: true,
  usage: { usageCount: 12, averageScorePercent: 76.4 },
  createdAt: "2026-11-01T00:00:00Z",
  updatedAt: "2026-11-02T00:00:00Z",
  ...over,
});

const open = () =>
  render(
    withTestProviders(
      <InterviewQuestionInfoView
        questionId={Q}
        basePath="/admin/interview-questions"
      />,
    ),
  );

describe("InterviewQuestionInfoView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    apiMock.get.mockResolvedValue(detail());
  });

  it("reads the detail by UUID and shows label, content, usage percent and practice state", async () => {
    open();
    expect(
      await screen.findByText(m.title.replace("{code}", "IQ-007")),
    ).toBeInTheDocument();
    expect(apiMock.get).toHaveBeenCalledWith(`${BASE}/${Q}`);
    expect(
      screen.getByText("Hash table xử lý collision thế nào?"),
    ).toBeInTheDocument();
    expect(screen.getByText("Mô tả thêm của câu hỏi")).toBeInTheDocument();
    expect(screen.getAllByText("Lý thuyết CS").length).toBeGreaterThan(0);
    expect(screen.getByText("Trung bình")).toBeInTheDocument();
    expect(screen.getByText("12")).toBeInTheDocument();
    expect(screen.getByText("76%")).toBeInTheDocument();
    expect(screen.getByText(m.practiceOn)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: m.edit })).toHaveAttribute(
      "href",
      `/admin/interview-questions/${Q}/edit`,
    );
  });

  it("shows a dash for the average before any completed attempt and practice off without a rubric", async () => {
    apiMock.get.mockResolvedValue(
      detail({
        rubric: [],
        practiceAvailable: false,
        usage: { usageCount: 0, averageScorePercent: null },
      }),
    );
    open();
    expect(await screen.findByText(m.practiceOff)).toBeInTheDocument();
    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("shows the not-found state for 404 INB-010 (unknown or retired id)", async () => {
    apiMock.get.mockRejectedValue(new ApiError("INB-010", 404));
    open();
    expect(
      await screen.findByText(ta.notFoundBody.replace("{code}", Q)),
    ).toBeInTheDocument();
  });

  it.each([
    ["INB-301", 403],
    ["INB-503", 503],
  ])("shows the error block with retry for %s", async (code, status) => {
    apiMock.get.mockRejectedValue(new ApiError(code, status));
    open();
    expect(
      await screen.findByRole("button", { name: viMessages.common.retry }),
    ).toBeInTheDocument();
  });
});

// USR0601 Khu vuc F NO 10-14 against the REAL api path (F6 #8).
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../../messages/vi.json";
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

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { InterviewTopicsTable } from "./interview-topics-table";

const m = viMessages.dashboard;
const row = (over: Record<string, unknown>) => ({
  topicId: "t1",
  topicCode: "OS",
  topicName: "Hệ điều hành",
  sortOrder: 1,
  questionsAttempted: 0,
  totalQuestions: 0,
  completionPercent: 0,
  lastPracticedAt: null,
  ...over,
});

describe("InterviewTopicsTable (F6 #8)", () => {
  beforeEach(() => resetApiMock());

  it("shows attempted / total and a dash for a topic never practised, one row per topic", async () => {
    apiMock.get.mockResolvedValue({
      items: [
        row({
          questionsAttempted: 2,
          totalQuestions: 5,
          lastPracticedAt: "2026-11-01T03:00:00Z",
        }),
        row({ topicId: "t2", topicName: "CSDL", totalQuestions: 4 }),
        row({ topicId: "t3", topicName: "Mới tạo" }),
      ],
    });
    render(withTestProviders(<InterviewTopicsTable />));
    expect(await screen.findByText("2 / 5")).toBeInTheDocument();
    expect(screen.getByText("0 / 4")).toBeInTheDocument();
    expect(screen.getByText("0 / 0")).toBeInTheDocument();
    expect(screen.getAllByText("-")).toHaveLength(2);
    expect(apiMock.get).toHaveBeenCalledWith(
      "/interview-bank/me/topic-progress",
    );
  });

  it("shows the empty copy when the bank has no topic", async () => {
    apiMock.get.mockResolvedValue({ items: [] });
    render(withTestProviders(<InterviewTopicsTable />));
    expect(await screen.findByText(m.emptyGeneric)).toBeInTheDocument();
  });

  it("fails alone with the block error copy", async () => {
    apiMock.get.mockImplementation(async () => {
      throw new Error("boom");
    });
    render(withTestProviders(<InterviewTopicsTable />));
    expect(await screen.findByText(m.errorGeneric)).toBeInTheDocument();
  });
});

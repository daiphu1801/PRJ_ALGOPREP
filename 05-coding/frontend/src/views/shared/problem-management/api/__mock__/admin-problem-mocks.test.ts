// The subtitle totals must come from the same rows the table lists (F1, owner review 2026-10-09).
import { createElement, type ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { loadAdminProblemPage } from "./admin-problem-mocks";
import { useInterviewQuestionPage } from "@/entities/interview-question";

describe("header totals", () => {
  it("problem totals equal the listed rows", async () => {
    const page = await loadAdminProblemPage();
    expect(page.totalProblems).toBe(page.problems.length);
    expect(page.publishedCount).toBe(
      page.problems.filter((problem) => problem.status === "published").length,
    );
  });

  it("question total equals the listed questions", async () => {
    const client = new QueryClient();
    const wrapper = ({ children }: { children: ReactNode }) =>
      createElement(QueryClientProvider, { client }, children);
    const { result } = renderHook(() => useInterviewQuestionPage(), { wrapper });
    await waitFor(() => expect(result.current.data).toBeDefined());
    const page = result.current.data!;
    expect(page.totalQuestions).toBe(page.questions.length);
  });
});

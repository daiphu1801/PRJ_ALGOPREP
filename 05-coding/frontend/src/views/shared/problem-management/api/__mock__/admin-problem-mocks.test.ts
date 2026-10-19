// The subtitle totals must come from the same rows the table lists (F1, owner review 2026-10-09).
import { describe, expect, it } from "vitest";
import { loadAdminProblemPage } from "./admin-problem-mocks";
import { fetchInterviewQuestionPage } from "@/entities/interview-question";

describe("header totals", () => {
  it("problem totals equal the listed rows", async () => {
    const page = await loadAdminProblemPage();
    expect(page.totalProblems).toBe(page.problems.length);
    expect(page.publishedCount).toBe(
      page.problems.filter((problem) => problem.status === "published").length,
    );
  });

  it("question total equals the listed questions", () => {
    const page = fetchInterviewQuestionPage();
    expect(page.totalQuestions).toBe(page.questions.length);
  });
});

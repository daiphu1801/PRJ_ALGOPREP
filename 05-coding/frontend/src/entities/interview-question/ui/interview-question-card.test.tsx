import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { InterviewQuestion } from "../model/types";
import { InterviewQuestionCard } from "./interview-question-card";

// Only the fields the card reads; the rest of the type is student-facing detail it ignores.
const question = {
  code: "IQ-014",
  question: "Explain how a hash map handles collisions",
  followUps: ["What is the load factor?", "When does it resize?"],
  rubric: [
    { label: "Correctness", weight: 50 },
    { label: "Depth", weight: 30 },
    { label: "Clarity", weight: 20 },
  ],
} as unknown as InterviewQuestion;

function renderCard(actions?: React.ReactNode) {
  return render(
    <InterviewQuestionCard
      question={question}
      topicLabel="Data structures"
      levelLabel="Medium"
      levelTone="neutral"
      followUpsLabel="Follow-ups"
      rubricLabel="Rubric"
      usageLabel="Used 184 times"
      actions={actions}
    />,
  );
}

describe("InterviewQuestionCard", () => {
  it("shows code, level, topic and the question as a level-3 heading", () => {
    renderCard();

    expect(screen.getByText("IQ-014")).toBeInTheDocument();
    expect(screen.getByText("Medium")).toBeInTheDocument();
    expect(screen.getByText("Data structures")).toBeInTheDocument();
    expect(
      screen.getByRole("heading", {
        level: 3,
        name: "Explain how a hash map handles collisions",
      }),
    ).toBeInTheDocument();
  });

  it("lists every follow-up under its heading", () => {
    renderCard();

    expect(screen.getByText("Follow-ups")).toBeInTheDocument();
    const items = screen.getAllByRole("listitem");
    expect(items.map((li) => li.textContent)).toEqual([
      "→What is the load factor?",
      "→When does it resize?",
    ]);
  });

  it("draws one weighted bar per rubric criterion", () => {
    renderCard();

    expect(screen.getByText("Rubric")).toBeInTheDocument();
    for (const [label, weight] of [
      ["Correctness", "50%"],
      ["Depth", "30%"],
      ["Clarity", "20%"],
    ] as const) {
      expect(screen.getByText(label)).toBeInTheDocument();
      expect(screen.getByText(weight)).toBeInTheDocument();
    }
    expect(screen.getAllByRole("progressbar")).toHaveLength(3);
  });

  it("renders the usage line and only shows the action row when actions are given", () => {
    const { unmount } = renderCard();
    expect(screen.getByText("Used 184 times")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
    unmount();

    renderCard(<button type="button">Edit</button>);
    const usage = screen.getByText("Used 184 times").parentElement!;
    expect(
      within(usage).getByRole("button", { name: "Edit" }),
    ).toBeInTheDocument();
  });
});

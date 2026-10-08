import { describe, expect, it } from "vitest";
import { CSV_TEMPLATE, parseQuestionsCsv } from "./csv-import";

const ctx = {
  topics: [{ key: "csTheory", label: "Lý thuyết CS" }],
  levels: [
    { key: "EASY", label: "Dễ" },
    { key: "MEDIUM", label: "Trung bình" },
  ],
  existing: ["Câu đã có"],
};

describe("parseQuestionsCsv", () => {
  it("imports the downloadable template as-is", () => {
    const result = parseQuestionsCsv(CSV_TEMPLATE, ctx);
    expect(result.errors).toEqual([]);
    expect(result.drafts).toHaveLength(1);
    expect(result.drafts[0]).toMatchObject({
      topic: "csTheory",
      level: "MEDIUM",
    });
    expect(result.drafts[0]!.followUps).toHaveLength(2);
    expect(result.drafts[0]!.rubric).toEqual([
      { label: "Định nghĩa collision", weight: 30 },
      { label: "So sánh hai cơ chế", weight: 40 },
      { label: "Chốt bằng load factor", weight: 30 },
    ]);
  });

  it("reports each bad row with its line and still imports the good ones", () => {
    const csv = [
      "topic,level,question,rubric",
      "csTheory,EASY,Câu tốt,",
      "nope,EASY,Sai chủ đề,",
      "csTheory,Dễ,Rubric lệch,A:50|B:40",
      "csTheory,Dễ,Rubric hỏng,A-50",
      "csTheory,Dễ,câu đã có,",
      "csTheory,Dễ,Câu tốt,",
      "csTheory,,Thiếu mức,",
    ].join("\n");
    const result = parseQuestionsCsv(csv, ctx);
    expect(result.drafts.map((draft) => draft.question)).toEqual(["Câu tốt"]);
    expect(result.errors.map(({ line, code }) => [line, code])).toEqual([
      [3, "unknownTopic"],
      [4, "rubricSum"],
      [5, "badRubric"],
      [6, "duplicate"],
      [7, "duplicate"],
      [8, "missingField"],
    ]);
    expect(result.total).toBe(7);
  });

  it("rejects a file that lacks a required column or has no data rows", () => {
    expect(
      parseQuestionsCsv("topic,question,rubric\nx,y,", ctx).fileError,
    ).toEqual({
      code: "missingColumns",
      detail: "level",
    });
    expect(
      parseQuestionsCsv("topic,level,question\nx,y,z", ctx).fileError,
    ).toEqual({
      code: "missingColumns",
      detail: "rubric",
    });
    expect(
      parseQuestionsCsv("topic,level,question,rubric", ctx).fileError?.code,
    ).toBe("empty");
    expect(parseQuestionsCsv("", ctx).fileError?.code).toBe("empty");
  });

  it("reads the optional columns and rejects the whole file above 500 rows", () => {
    const withOptional = [
      "topic,level,question,rubric,content,suggested_approach,core_keywords,sample_answer",
      "csTheory,Dễ,Câu đủ cột,,Nội dung,Ý một|Ý hai,từ một|từ hai,Khung mẫu",
    ].join("\n");
    expect(parseQuestionsCsv(withOptional, ctx).drafts[0]).toMatchObject({
      content: "Nội dung",
      suggestedApproach: ["Ý một", "Ý hai"],
      coreKeywords: ["từ một", "từ hai"],
      sampleAnswerFramework: "Khung mẫu",
    });
    const rows = Array.from({ length: 501 }, (_, i) => `csTheory,Dễ,Câu ${i},`);
    const tooMany = parseQuestionsCsv(
      ["topic,level,question,rubric", ...rows].join("\n"),
      ctx,
    );
    expect(tooMany.fileError?.code).toBe("tooManyRows");
    expect(tooMany.drafts).toEqual([]);
  });
});

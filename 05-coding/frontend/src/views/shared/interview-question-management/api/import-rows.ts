// Turns the validated CSV drafts into the rows of F6 #24 (`ImportRow`, DD 24.2). The server wants the
// physical line of each record (header = line 1) to report skipped rows against; the client parse keeps
// it only for rejected rows, so the line of an accepted draft is recovered here: the accepted records
// are the data records minus the rejected lines, in file order, one per draft.
import { parseCsv, type CsvRecord } from "@/shared/lib";
import type {
  CsvImportParse,
  ImportRowInput,
} from "@/entities/interview-question";

export function toImportRows(
  text: string,
  parse: CsvImportParse,
): ImportRowInput[] {
  const rejected = new Set(parse.errors.map((error) => error.line));
  const accepted: CsvRecord[] = parseCsv(text)
    .slice(1)
    .filter((record) => !rejected.has(record.line));
  return parse.drafts.map((draft, index) => {
    const row: ImportRowInput = {
      line: accepted[index]!.line,
      topic: draft.topic,
      level: draft.level,
      question: draft.question,
      rubric: draft.rubric.map((item) => ({
        criterionCode: item.label,
        weightPercent: item.weight,
      })),
    };
    if (draft.followUps.length > 0) row.followUps = draft.followUps;
    if (draft.content) row.content = draft.content;
    if (draft.suggestedApproach?.length)
      row.suggestedApproach = draft.suggestedApproach;
    if (draft.coreKeywords?.length) row.coreKeywords = draft.coreKeywords;
    if (draft.sampleAnswerFramework)
      row.sampleAnswer = draft.sampleAnswerFramework;
    return row;
  });
}

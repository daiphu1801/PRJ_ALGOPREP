// F2-16: a duplicate opens pre-filled from the source and unpublished, and stores nothing until Save.
import { describe, expect, it } from "vitest";
import { listCreatedProblems } from "../../model/created-problems";
import { loadProblemDraft, saveProblemDraft } from "./problem-draft-mocks";

describe("loadProblemDraft duplicate", () => {
  it("returns the source content as an unsaved draft copy", async () => {
    const source = await loadProblemDraft("#121");
    const copy = await loadProblemDraft(undefined, "#121");
    expect(copy.draft.title).toBe(`${source.draft.title} (bản sao)`);
    expect(copy.draft.status).toBe("draft");
    expect(copy.savedAt).toBeNull();
    expect(copy.draft.testcases).toEqual(source.draft.testcases);
  });

  it("creates the problem on the first save of a copy and only updates it on later saves", async () => {
    const before = listCreatedProblems().length;
    const copy = await loadProblemDraft(undefined, "#121");
    const code = await saveProblemDraft(copy.draft);
    expect(code).toMatch(/^#2\d{3}$/);
    expect(listCreatedProblems()).toHaveLength(before + 1);
    expect(listCreatedProblems().at(-1)).toMatchObject({
      code,
      title: copy.draft.title,
      status: "draft",
    });

    expect(
      await saveProblemDraft({ ...copy.draft, title: "Renamed" }, code),
    ).toBeUndefined();
    expect(listCreatedProblems()).toHaveLength(before + 1);
    expect(listCreatedProblems().at(-1)!.title).toBe("Renamed");
    expect((await loadProblemDraft(code)).draft.title).toBe("Renamed");
  });
});

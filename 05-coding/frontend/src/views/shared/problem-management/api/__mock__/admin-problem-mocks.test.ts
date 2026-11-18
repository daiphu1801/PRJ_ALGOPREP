// The mock must answer the real contract: totals ignore the filter, a page past the end is the last page,
// and a bulk action overlays the rows without touching the literal data.
import { beforeEach, describe, expect, it } from "vitest";
import {
  loadAdminProblemList,
  loadAttentionList,
  loadTopicDistribution,
  mockBulkDelete,
  mockBulkDifficulty,
  mockBulkPublish,
  resetAdminProblemMock,
} from "./admin-problem-mocks";
import type { AdminListParams } from "../../model/types";

const base: AdminListParams = {
  q: "",
  level: "all",
  status: "all",
  sort: "code",
  dir: "asc",
  page: 1,
  pageSize: 8,
};

beforeEach(() => resetAdminProblemMock());

describe("loadAdminProblemList", () => {
  it("totals equal the rows in scope and ignore the filter (header totals, owner review 2026-10-09)", async () => {
    const all = await loadAdminProblemList({ ...base, pageSize: 50 });
    const filtered = await loadAdminProblemList({
      ...base,
      status: "draft",
      pageSize: 50,
    });
    expect(all.totals.total).toBe(all.page.totalItems);
    expect(filtered.totals).toEqual(all.totals);
    expect(filtered.page.totalItems).toBeLessThan(all.page.totalItems);
    expect(all.totals.published).toBe(
      all.items.filter((problem) => problem.status === "published").length,
    );
  });

  it("answers the last page when the page is past the end", async () => {
    const page = await loadAdminProblemList({ ...base, page: 99 });
    expect(page.page.number).toBe(page.page.totalPages);
    expect(page.items.length).toBeGreaterThan(0);
  });

  it("sorts by the server field the screen asked for", async () => {
    const page = await loadAdminProblemList({
      ...base,
      sort: "submissions",
      dir: "desc",
    });
    const counts = page.items.map((problem) => problem.submissionCount);
    expect(counts).toEqual([...counts].sort((a, b) => b - a));
  });

  it("filters by level and by a word of the title", async () => {
    const hard = await loadAdminProblemList({ ...base, level: "HARD" });
    expect(hard.items.every((problem) => problem.difficulty === "HARD")).toBe(
      true,
    );
    const word = await loadAdminProblemList({ ...base, q: "coin change" });
    expect(word.items).toHaveLength(1);
  });
});

describe("bulk overlays", () => {
  it("delete removes the row everywhere, publish and difficulty change it", async () => {
    const before = await loadAdminProblemList({ ...base, pageSize: 50 });
    const draft = before.items.find((problem) => problem.status === "draft")!;
    const other = before.items.find((problem) => problem.id !== draft.id)!;

    expect(await mockBulkPublish([draft.id])).toEqual({
      succeededIds: [draft.id],
      failedItems: [],
    });
    await mockBulkDifficulty([other.id], "HARD");
    await mockBulkDelete([draft.id]);

    const after = await loadAdminProblemList({ ...base, pageSize: 50 });
    expect(
      after.items.find((problem) => problem.id === draft.id),
    ).toBeUndefined();
    expect(
      after.items.find((problem) => problem.id === other.id)?.difficulty,
    ).toBe("HARD");
    expect(after.totals.total).toBe(before.totals.total - 1);
  });

  it("reports an unknown id on its own and keeps the rest", async () => {
    const page = await loadAdminProblemList(base);
    const known = page.items[0]!.id;
    const result = await mockBulkDelete([known, "no-such"]);
    expect(result.succeededIds).toEqual([known]);
    expect(result.failedItems).toEqual([{ id: "no-such", code: "PRB-010" }]);
  });

  it("publishing more than 20 is rejected like the server does", async () => {
    await expect(
      mockBulkPublish(Array.from({ length: 21 }, (_, n) => String(n))),
    ).rejects.toMatchObject({ code: "PRB-001" });
  });
});

describe("side blocks", () => {
  it("are derived from the rows listed, never typed in", async () => {
    const list = await loadAdminProblemList({ ...base, pageSize: 50 });
    const distribution = await loadTopicDistribution();
    expect(distribution.totalProblemsWithTopic).toBe(list.totals.total);
    expect(distribution.items.reduce((sum, item) => sum + item.count, 0)).toBe(
      list.totals.total,
    );
    const attention = await loadAttentionList();
    expect(attention.map((item) => item.ruleCode)).toEqual([
      "noTestcase",
      "lowAcceptRate",
      "hiddenFromLearners",
      "staleDraft",
    ]);
  });
});

import { describe, expect, it } from "vitest";
import {
  READABLE_SUBMISSION_IDS,
  fetchMySubmissionsPage,
  fetchSubmissionDetail,
} from "./submission-mocks";

describe("fetchSubmissionDetail fallback", () => {
  it("resolves every row of the history list; derived rows mirror their list item", () => {
    const { items } = fetchMySubmissionsPage({ pageSize: 100 });
    for (const item of items) {
      const detail = fetchSubmissionDetail(item.id);
      expect(detail, item.id).not.toBeNull();
      // Hand-written fixtures keep their own values; only derived details mirror the list row.
      if (READABLE_SUBMISSION_IDS.includes(item.id)) continue;
      expect(detail?.status).toBe(item.status);
      expect(detail?.testcases.length).toBe(item.totalCount ?? 0);
    }
  });

  it("still returns null for an unknown id", () => {
    expect(fetchSubmissionDetail("SB-0")).toBeNull();
  });
});

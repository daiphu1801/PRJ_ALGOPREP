import { describe, expect, it } from "vitest";
import { fetchAiConfigPage } from "../api/__mock__/ai-config-mocks";
import {
  applyPublish,
  bumpVersion,
  describeChanges,
  pendingByFeature,
  restoreVersion,
  totalPending,
  withInstruction,
} from "./changes";

describe("admin_ai_config unpublished changes", () => {
  it("counts a changed model and guard on the tab they belong to", () => {
    const saved = fetchAiConfigPage();
    const draft = fetchAiConfigPage();
    draft.prompts[0]!.modelId = "claude-opus";
    draft.guards.interview.citeLines = true;

    expect(pendingByFeature(saved, draft)).toEqual({
      review: 1,
      interview: 1,
      testcase: 0,
    });
    expect(totalPending(pendingByFeature(saved, draft))).toBe(2);
    expect(totalPending(pendingByFeature(saved, saved))).toBe(0);
  });

  it("publishing bumps the version of a changed tab only and logs it", () => {
    const saved = fetchAiConfigPage();
    const draft = fetchAiConfigPage();
    draft.prompts[1]!.modelId = "claude-opus";

    const next = applyPublish(saved, draft);
    expect(next.prompts[1]).toMatchObject({ version: "v2.6", status: "live" });
    expect(next.prompts[0]!.version).toBe("v3.8");
    expect(next.versionHistory[0]).toMatchObject({
      feature: "interview",
      version: "v2.6",
    });
    expect(totalPending(pendingByFeature(next, next))).toBe(0);
  });

  it("leaves an unexpected version string alone", () => {
    expect(bumpVersion("v3.8")).toBe("v3.9");
    expect(bumpVersion("draft")).toBe("draft");
  });

  it("an edited instruction counts as one change and is listed for the publish confirmation", () => {
    const saved = fetchAiConfigPage();
    const draft = withInstruction(
      fetchAiConfigPage(),
      "interview",
      "Instruction moi",
    );
    draft.prompts[1]!.modelId = "claude-haiku";

    expect(pendingByFeature(saved, draft).interview).toBe(2);
    expect(describeChanges(saved, draft)).toEqual([
      {
        feature: "interview",
        instruction: true,
        model: { from: "claude-sonnet", to: "claude-haiku" },
        guards: 0,
      },
    ]);
    expect(describeChanges(saved, saved)).toEqual([]);
  });

  it("restoring a version brings back only its instruction, as a draft change", () => {
    const saved = fetchAiConfigPage();
    const old = saved.versionHistory.find(
      (e) => e.feature === "review" && e.version === "v3.7",
    )!;
    const draft = restoreVersion(fetchAiConfigPage(), old);

    expect(draft.prompts[0]!.systemInstruction).toBe(old.systemInstruction);
    expect(draft.prompts[0]!.version).toBe("v3.8");
    expect(pendingByFeature(saved, draft).review).toBe(1);

    const next = applyPublish(saved, draft);
    expect(next.versionHistory[0]).toMatchObject({
      feature: "review",
      version: "v3.9",
      systemInstruction: old.systemInstruction,
    });
  });
});

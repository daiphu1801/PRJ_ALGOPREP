import { describe, expect, it } from "vitest";
import { parseSubmissionOrigin } from "./origin";

describe("submission origin", () => {
  it("accepts the three calling screens and falls back to the submission list", () => {
    expect(parseSubmissionOrigin("student")).toBe("student");
    expect(parseSubmissionOrigin("class")).toBe("class");
    expect(parseSubmissionOrigin("submissions")).toBe("submissions");
    expect(parseSubmissionOrigin("whatever")).toBe("submissions");
    expect(parseSubmissionOrigin(null)).toBe("submissions");
    expect(parseSubmissionOrigin(undefined)).toBe("submissions");
  });
});

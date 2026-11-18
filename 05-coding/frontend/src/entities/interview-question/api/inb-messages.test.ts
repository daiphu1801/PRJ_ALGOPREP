import { describe, expect, it } from "vitest";
import en from "../../../../messages/en.json";
import vi from "../../../../messages/vi.json";

// Every INB code the learner endpoints (#2 to #7), the catalogue reads (#9, #10) and the catalogue
// writes (#11 to #18) can answer with (03-dd/api/F6_interview-bank.md Bảng 7).
const CODES = [
  "INB-010",
  "INB-011",
  "INB-012",
  "INB-020",
  "INB-021",
  "INB-030",
  "INB-031",
  "INB-032",
  "INB-034",
  "INB-040",
  "INB-041",
  "INB-042",
  "INB-050",
  "INB-301",
  "INB-302",
  "INB-501",
  "INB-502",
  "INB-503",
  "INB-V03",
  "INB-V04",
  "INB-V05",
  "INB-V06",
  "INB-V07",
  "INB-V08",
  "INB-V13",
  "INB-V14",
];

describe("INB codes of the learner and catalogue endpoints", () => {
  it.each(CODES)("%s has text in vi and en", (code) => {
    const viText = (vi.apiErrors as Record<string, string>)[code];
    const enText = (en.apiErrors as Record<string, string>)[code];
    expect(viText).toBeTruthy();
    expect(enText).toBeTruthy();
    // Never the raw code as text.
    expect(viText).not.toContain(code);
    expect(enText).not.toContain(code);
  });
});

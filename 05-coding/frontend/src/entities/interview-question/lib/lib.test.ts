import { describe, expect, it } from "vitest";
import { nextReviewDays } from "./next-review";
import { splitOutline } from "./split-outline";
import {
  fromApiRating,
  toApiRating,
  toApiStatus,
} from "../model/recall-map";

describe("splitOutline", () => {
  it("splits bullet and numbered lines into items", () => {
    expect(splitOutline("- a\n* b\n1. c\n12. d")).toEqual(["a", "b", "c", "d"]);
  });

  it("appends a continuation line to the item before it", () => {
    expect(splitOutline("- first\n  more\n- second")).toEqual([
      "first more",
      "second",
    ]);
  });

  it("keeps a leading plain line as an item of its own, and a heading-like line as one item", () => {
    expect(splitOutline("Bẫy thường gặp")).toEqual(["Bẫy thường gặp"]);
    expect(splitOutline("Mở đầu\n- ý một")).toEqual(["Mở đầu", "ý một"]);
  });

  it("returns nothing for an empty, blank or missing string", () => {
    expect(splitOutline("")).toEqual([]);
    expect(splitOutline("  \n\n")).toEqual([]);
    expect(splitOutline(null)).toEqual([]);
    expect(splitOutline(undefined)).toEqual([]);
  });

  it("handles CRLF line breaks", () => {
    expect(splitOutline("- a\r\n- b")).toEqual(["a", "b"]);
  });
});

describe("nextReviewDays", () => {
  const now = Date.parse("2026-11-11T00:00:00Z");

  it("is exactly the interval right after a rating", () => {
    expect(nextReviewDays("2026-11-14T00:00:00Z", now)).toBe(3);
  });

  it("rounds a part of a day up", () => {
    expect(nextReviewDays("2026-11-11T05:00:00Z", now)).toBe(1);
  });

  it("is 0 or negative when the review is already due", () => {
    expect(nextReviewDays("2026-11-11T00:00:00Z", now)).toBe(0);
    expect(nextReviewDays("2026-11-01T00:00:00Z", now)).toBeLessThan(0);
  });
});

describe("recall map", () => {
  it("maps the three ratings both ways", () => {
    expect(toApiRating("known")).toBe("KNOWN");
    expect(toApiRating("vague")).toBe("VAGUE");
    expect(toApiRating("forgotten")).toBe("FORGOTTEN");
    expect(fromApiRating("KNOWN")).toBe("known");
    expect(fromApiRating("VAGUE")).toBe("vague");
    expect(fromApiRating("FORGOTTEN")).toBe("forgotten");
  });

  it("reads a missing or unknown rating as never rated", () => {
    expect(fromApiRating(null)).toBeNull();
    expect(fromApiRating(undefined)).toBeNull();
    expect(fromApiRating("GOOD")).toBeNull();
  });

  it("maps the status filter of the screen to the one of the API", () => {
    expect(toApiStatus("all")).toBe("ALL");
    expect(toApiStatus("new")).toBe("UNSEEN");
    expect(toApiStatus("reviewing")).toBe("REVIEWING");
    expect(toApiStatus("known")).toBe("KNOWN");
  });
});

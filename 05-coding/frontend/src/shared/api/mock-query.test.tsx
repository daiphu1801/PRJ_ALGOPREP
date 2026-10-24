import { describe, expect, it } from "vitest";
import { notImplemented } from "./mock-query";

describe("notImplemented", () => {
  it("throws a 501 NOT_IMPLEMENTED naming the DD file", () => {
    expect(() => notImplemented("03-dd/api/F2_problem-bank.md")()).toThrow(
      /F2_problem-bank/,
    );
  });
});

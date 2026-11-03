import { describe, expect, it } from "vitest";
import { ApiError, isNotFound, toApiError } from "./api-error";

describe("toApiError", () => {
  it("reads the flat RFC 9457 body the backend really sends", () => {
    const error = toApiError(422, {
      errorCode: "IDT-112",
      details: { restoreTicket: "t0ken", expiresInSeconds: 300 },
    });
    expect(error).toBeInstanceOf(ApiError);
    expect(error.code).toBe("IDT-112");
    expect(error.status).toBe(422);
    expect(error.details).toEqual({ restoreTicket: "t0ken", expiresInSeconds: 300 });
  });

  it("still reads the nested shape documented in the DD", () => {
    const error = toApiError(401, {
      error: { code: "IDT-105", details: { a: 1 } },
    });
    expect(error.code).toBe("IDT-105");
    expect(error.details).toEqual({ a: 1 });
  });

  it("prefers the flat code when both shapes are present", () => {
    expect(
      toApiError(400, { errorCode: "IDT-102", error: { code: "IDT-105" } }).code,
    ).toBe("IDT-102");
  });

  it("falls back to UNKNOWN_ERROR for missing, empty or non-string codes", () => {
    expect(toApiError(500, undefined).code).toBe("UNKNOWN_ERROR");
    expect(toApiError(500, { errorCode: "" }).code).toBe("UNKNOWN_ERROR");
    expect(toApiError(500, { errorCode: 42 }).code).toBe("UNKNOWN_ERROR");
    expect(toApiError(0, {}).status).toBe(0);
  });

  it("ignores details that are not an object", () => {
    expect(toApiError(400, { errorCode: "X", details: "oops" }).details).toBeUndefined();
    expect(toApiError(400, { errorCode: "X", details: ["a"] }).details).toBeUndefined();
  });

  it("keeps isNotFound working", () => {
    expect(isNotFound(toApiError(404, {}))).toBe(true);
    expect(isNotFound(toApiError(401, {}))).toBe(false);
  });
});

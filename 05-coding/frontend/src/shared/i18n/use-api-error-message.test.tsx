import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import en from "../../../messages/en.json";
import vi from "../../../messages/vi.json";
import { ApiError } from "@/shared/api/api-error";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { useApiErrorMessage } from "./use-api-error-message";

function translator() {
  return renderHook(useApiErrorMessage, {
    wrapper: ({ children }) => withTestProviders(children),
  }).result.current;
}

describe("useApiErrorMessage", () => {
  it("translates a known backend error code", () => {
    const message = translator();
    expect(message(new ApiError("IDT-102", 401))).toBe(vi.apiErrors["IDT-102"]);
    expect(message(new ApiError("IDT-V02", 422))).toContain("72 byte");
  });

  it("falls back to the generic text for unknown codes and non-API errors", () => {
    const message = translator();
    expect(message(new ApiError("IDT-999", 500))).toBe(
      vi.apiErrors.UNKNOWN_ERROR,
    );
    expect(message(new Error("boom"))).toBe(vi.apiErrors.UNKNOWN_ERROR);
    expect(message(undefined)).toBe(vi.apiErrors.UNKNOWN_ERROR);
  });

  it("covers every error code the identity backend can send, in both languages", () => {
    // Codes from IdentityErrorCodes plus the validation codes raised by RegistrationValidation.
    const backendCodes = [
      "IDT-101",
      "IDT-102",
      "IDT-103",
      "IDT-104",
      "IDT-105",
      "IDT-106",
      "IDT-107",
      "IDT-111",
      "IDT-112",
      "IDT-113",
      "IDT-114",
      "IDT-400",
      "IDT-445",
      "IDT-447",
      "IDT-503",
      "IDT-504",
      "IDT-505",
      "IDT-109",
      "IDT-110",
      "IDT-V28",
      "IDT-V01",
      "IDT-V02",
      "IDT-V03",
      "IDT-V04",
      "IDT-V11",
      "IDT-V12",
      "IDT-V19",
      "IDT-V20",
    ];
    for (const code of backendCodes) {
      expect(vi.apiErrors, code).toHaveProperty([code]);
      expect(en.apiErrors, code).toHaveProperty([code]);
    }
  });

  it("covers the class, RBAC, audit, retention and admin-user codes of F1, in both languages", () => {
    // IdentityErrorCodes, RbacErrorCodes, AuditReadErrorCodes and the DD error table (F1 #25 to #66).
    const codes = [
      "IDT-201",
      "IDT-410",
      "IDT-411",
      "IDT-412",
      "IDT-413",
      "IDT-414",
      "IDT-415",
      "IDT-416",
      "IDT-420",
      "IDT-421",
      "IDT-422",
      "IDT-423",
      "IDT-424",
      "IDT-425",
      "IDT-432",
      "IDT-440",
      "IDT-441",
      "IDT-442",
      "IDT-443",
      "IDT-444",
      "IDT-446",
      "IDT-448",
      "IDT-449",
      "IDT-450",
      "IDT-V05",
      "IDT-V06",
      "IDT-V07",
      "IDT-V08",
      "IDT-V09",
      "IDT-V10",
      "IDT-V13",
      "IDT-V14",
      "IDT-V15",
      "IDT-V16",
      "IDT-V17",
      "IDT-V18",
      "IDT-V21",
      "IDT-V22",
      "IDT-V23",
      "IDT-V24",
      "IDT-V25",
      "IDT-V26",
    ];
    for (const code of codes) {
      expect(vi.apiErrors, code).toHaveProperty([code]);
      expect(en.apiErrors, code).toHaveProperty([code]);
    }
  });
});

describe("useApiErrorMessage for F2 problem-bank", () => {
  // The DD error table (03-dd/api/F2_problem-bank.md section 60): every code PRB-001 to PRB-058 in use.
  const codes = [
    1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15, 20, 21, 22, 23, 25, 30, 31, 32, 33,
    36, 37, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55,
    56, 57, 58,
  ].map((n) => `PRB-${String(n).padStart(3, "0")}`);

  it("has a vi and an en text for every PRB code", () => {
    for (const code of codes) {
      expect(vi.apiErrors, code).toHaveProperty([code]);
      expect(en.apiErrors, code).toHaveProperty([code]);
    }
  });

  it("translates a PRB code and never shows the raw code", () => {
    const message = translator();
    const text = message(new ApiError("PRB-032", 422));
    expect(text).toBe(vi.apiErrors["PRB-032"]);
    expect(text).not.toContain("PRB-");
  });
});

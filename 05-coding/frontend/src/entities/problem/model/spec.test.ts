// Rules of the "Đặc tả" tab: what makes a spec complete, the combinations it refuses, and how the one
// shared signature turns into each language's spelling.
import { describe, expect, it } from "vitest";
import type { ProblemSpec } from "./draft-types";
import {
  allowsUnorderedSet,
  defaultType,
  derivedName,
  epsilonValid,
  exceedsSchema,
  isIdentifier,
  elementKinds,
  isSpecComplete,
  isTypeValid,
  languageName,
  renderSignature,
  signatureIssues,
} from "./spec";

function spec(overrides: Partial<ProblemSpec> = {}): ProblemSpec {
  return {
    signature: {
      functionName: "min_window",
      returnType: { kind: "STRING" },
      parameters: [
        { id: "a", name: "s", type: { kind: "STRING" } },
        { id: "b", name: "t", type: { kind: "STRING" } },
      ],
      nameOverrides: {},
    },
    stdinFormat: "Dòng 1: s",
    stdoutFormat: "Một dòng",
    matchingStrategy: "EXACT",
    epsilon: "",
    ...overrides,
  };
}

describe("spec rules", () => {
  it("accepts a fully declared spec", () => {
    expect(isSpecComplete(spec())).toBe(true);
  });

  it("needs both formats", () => {
    expect(isSpecComplete(spec({ stdinFormat: "  " }))).toBe(false);
    expect(isSpecComplete(spec({ stdoutFormat: "" }))).toBe(false);
  });

  it("flags a bad function name, a bad or duplicate parameter name, and a bad override", () => {
    const base = spec().signature;
    expect(signatureIssues({ ...base, functionName: "2bad" })).toContain(
      "functionName",
    );
    expect(
      signatureIssues({
        ...base,
        parameters: [{ id: "p", name: "", type: { kind: "INT" } }],
      }),
    ).toContain("parameterName:p");
    const twice = [
      { id: "a", name: "x", type: { kind: "INT" as const } },
      { id: "b", name: "x", type: { kind: "INT" as const } },
    ];
    expect(signatureIssues({ ...base, parameters: twice })).toEqual([
      "parameterDuplicate:b",
    ]);
    expect(
      signatureIssues({ ...base, nameOverrides: { java: "not valid" } }),
    ).toEqual(["override:java"]);
    expect(signatureIssues({ ...base, nameOverrides: { java: "  " } })).toEqual(
      [],
    );
  });

  it("needs a positive epsilon only for EPSILON", () => {
    expect(
      isSpecComplete(spec({ matchingStrategy: "EPSILON", epsilon: "" })),
    ).toBe(false);
    expect(
      isSpecComplete(spec({ matchingStrategy: "EPSILON", epsilon: "0" })),
    ).toBe(false);
    expect(
      isSpecComplete(spec({ matchingStrategy: "EPSILON", epsilon: "1e-6" })),
    ).toBe(true);
    expect(epsilonValid(spec({ epsilon: "abc" }))).toBe(false);
  });

  it("refuses UNORDERED_SET for a tree or linked-list return type", () => {
    const tree = spec();
    tree.signature = {
      ...tree.signature,
      returnType: defaultType("BINARY_TREE"),
    };
    expect(allowsUnorderedSet(tree)).toBe(false);
    expect(isSpecComplete({ ...tree, matchingStrategy: "UNORDERED_SET" })).toBe(
      false,
    );
    expect(isSpecComplete({ ...tree, matchingStrategy: "EXACT" })).toBe(true);
  });

  it("drops the signature requirement when a type falls outside the schema (Standard I/O only)", () => {
    const incomplete = spec();
    incomplete.signature = {
      ...incomplete.signature,
      functionName: "",
      returnType: defaultType("OTHER"),
    };
    expect(exceedsSchema(incomplete)).toBe(true);
    expect(isSpecComplete(incomplete)).toBe(true);
    expect(isSpecComplete({ ...incomplete, stdinFormat: "" })).toBe(false);
  });

  it("gives a container its element kind and an array one dimension", () => {
    expect(defaultType("ARRAY")).toEqual({
      kind: "ARRAY",
      of: { kind: "INT" },
      dimensions: 1,
    });
    expect(defaultType("LIST")).toEqual({ kind: "LIST", of: { kind: "INT" } });
    expect(defaultType("STRING")).toEqual({ kind: "STRING" });
    expect(isIdentifier("min_window")).toBe(true);
  });
});

describe("per-language spelling of the shared signature", () => {
  it("derives snake_case for Python and camelCase for Java and C++ from either spelling", () => {
    for (const typed of ["min_window", "minWindow", "Min Window"]) {
      expect(derivedName(typed, "python")).toBe("min_window");
      expect(derivedName(typed, "java")).toBe("minWindow");
      expect(derivedName(typed, "cpp")).toBe("minWindow");
    }
    expect(derivedName("", "java")).toBe("");
  });

  it("uses an override when written, otherwise the derived name", () => {
    const base = spec().signature;
    expect(languageName(base, "java")).toBe("minWindow");
    expect(
      languageName(
        { ...base, nameOverrides: { java: "smallestWindow" } },
        "java",
      ),
    ).toBe("smallestWindow");
    expect(
      languageName(
        { ...base, nameOverrides: { java: "smallestWindow" } },
        "python",
      ),
    ).toBe("min_window");
  });

  it("renders the starter signature line in each language", () => {
    const base = spec().signature;
    expect(renderSignature(base, "python")).toBe(
      "def min_window(s: str, t: str) -> str:",
    );
    expect(renderSignature(base, "java")).toBe(
      "public String minWindow(String s, String t)",
    );
    expect(renderSignature(base, "cpp")).toBe(
      "string minWindow(string s, string t)",
    );
  });

  it("maps containers to each language's own type", () => {
    const signature = {
      ...spec().signature,
      functionName: "two_sum",
      returnType: {
        kind: "ARRAY" as const,
        of: { kind: "INT" as const },
        dimensions: 1,
      },
      parameters: [
        {
          id: "n",
          name: "nums",
          type: { kind: "LIST" as const, of: { kind: "INT" as const } },
        },
        {
          id: "m",
          name: "grid",
          type: {
            kind: "ARRAY" as const,
            of: { kind: "LONG" as const },
            dimensions: 2,
          },
        },
      ],
    };
    expect(renderSignature(signature, "java")).toBe(
      "public int[] twoSum(List<Integer> nums, long[][] grid)",
    );
    expect(renderSignature(signature, "cpp")).toBe(
      "vector<int> twoSum(vector<int> nums, vector<vector<long long>> grid)",
    );
    expect(renderSignature(signature, "python")).toBe(
      "def two_sum(nums: List[int], grid: List[List[int]]) -> List[int]:",
    );
  });
});

describe("nested types (BD SHR0202 Q10)", () => {
  const listOfListOfString = {
    kind: "LIST" as const,
    of: { kind: "LIST" as const, of: { kind: "STRING" as const } },
  };

  it("lets only LIST hold a container, and only up to three levels", () => {
    expect(elementKinds("ARRAY", 1)).toEqual([
      "INT",
      "LONG",
      "DOUBLE",
      "BOOLEAN",
      "CHAR",
      "STRING",
    ]);
    expect(elementKinds("LINKED_LIST", 1)).not.toContain("LIST");
    expect(elementKinds("LIST", 1)).toContain("LIST");
    expect(elementKinds("LIST", 2)).toContain("BINARY_TREE");
    expect(elementKinds("LIST", 3)).not.toContain("LIST");
  });

  it("validates a nested type and rejects one that breaks the rules", () => {
    expect(isTypeValid(listOfListOfString)).toBe(true);
    expect(
      isTypeValid({
        kind: "ARRAY",
        of: { kind: "LIST", of: { kind: "INT" } },
        dimensions: 1,
      }),
    ).toBe(false);
    expect(isTypeValid({ kind: "LIST" })).toBe(false);
    const fourDeep = {
      kind: "LIST" as const,
      of: {
        kind: "LIST" as const,
        of: { kind: "LIST" as const, of: listOfListOfString },
      },
    };
    expect(isTypeValid(fourDeep)).toBe(false);
  });

  it("renders List of List in each language, boxing scalars inside Java generics", () => {
    const signature = {
      ...spec().signature,
      functionName: "group_anagrams",
      returnType: listOfListOfString,
    };
    expect(renderSignature(signature, "java")).toContain(
      "public List<List<String>> groupAnagrams(",
    );
    expect(renderSignature(signature, "cpp")).toContain(
      "vector<vector<string>> groupAnagrams(",
    );
    expect(renderSignature(signature, "python")).toContain(
      "-> List[List[str]]:",
    );

    const ints = {
      ...signature,
      returnType: {
        kind: "LIST" as const,
        of: {
          kind: "ARRAY" as const,
          of: { kind: "INT" as const },
          dimensions: 1,
        },
      },
    };
    expect(renderSignature(ints, "java")).toContain(
      "public List<int[]> groupAnagrams(",
    );
  });
});

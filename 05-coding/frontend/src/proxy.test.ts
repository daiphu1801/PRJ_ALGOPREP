// @vitest-environment node
import { NextRequest } from "next/server";
import { describe, expect, it } from "vitest";
import { proxy } from "./proxy";

describe("proxy", () => {
  it("lets the browser reach local object storage for presigned testcase files", () => {
    const policy = proxy(
      new NextRequest("http://localhost:3000/login"),
    ).headers.get("Content-Security-Policy");
    const connect = policy
      ?.split("; ")
      .find((directive) => directive.startsWith("connect-src "));
    expect(connect?.split(" ")).toContain("http://localhost:9000");
  });
});

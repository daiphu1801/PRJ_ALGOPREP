import { afterEach, describe, expect, it } from "vitest";
import { tokenStore } from "./token-store";
import { currentUserScope } from "./user-scope";

afterEach(() => tokenStore.set(null));

describe("personalized cache partition", () => {
  it("partitions subjects and leaves the bearer token out of the key", () => {
    const first = `header.${btoa(JSON.stringify({ sub: "learner-a", sid: "private-session" }))}.signature`;
    tokenStore.set(first);
    expect(currentUserScope()).toBe("learner-a");
    tokenStore.set(
      `header.${btoa(JSON.stringify({ sub: "learner-b" }))}.signature`,
    );
    expect(currentUserScope()).toBe("learner-b");
    tokenStore.set(null);
    expect(currentUserScope()).toBe("anonymous");
  });

  it("never reuses a previous subject after a malformed token", () => {
    tokenStore.set(
      `header.${btoa(JSON.stringify({ sub: "learner-a" }))}.signature`,
    );
    expect(currentUserScope()).toBe("learner-a");
    tokenStore.set("not-a-token");
    expect(currentUserScope()).toBe("anonymous");
  });
});

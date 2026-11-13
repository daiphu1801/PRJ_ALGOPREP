// Test-only stand-in for the URL part of `next/navigation`, for views whose state lives in the query
// string. `router.replace` really changes the (fake) URL and re-renders the readers, so a test can
// click a filter and then assert the next request, or start from a prefilled URL. Usage:
//
//   vi.mock("next/navigation", async () =>
//     (await import("@/shared/test/mock-url")).navigationMock({ push }),
//   );
//   import { mockUrl } from "@/shared/test/mock-url";
//   beforeEach(() => mockUrl.reset("/problems?level=EASY"));
import { useMemo, useSyncExternalStore } from "react";

let current = "/";
const listeners = new Set<() => void>();

function emit() {
  listeners.forEach((listener) => listener());
}

export const mockUrl = {
  /** Resets the fake URL, with or without a query string. */
  reset(url = "/") {
    current = url;
    emit();
  },
  /** Path plus query, as the last `replace` left it. */
  get href() {
    return current;
  },
  get search() {
    return new URLSearchParams(current.split("?")[1] ?? "");
  },
  pathname() {
    return current.split("?")[0]!;
  },
};

export function navigationMock(router: Record<string, unknown> = {}) {
  return {
    useRouter: () => ({
      ...router,
      replace: (url: string) => {
        current = url;
        emit();
      },
    }),
    usePathname: () => mockUrl.pathname(),
    useSearchParams: () => {
      const href = useSyncExternalStore(
        (listener) => {
          listeners.add(listener);
          return () => listeners.delete(listener);
        },
        () => current,
        () => current,
      );
      return useMemo(() => new URLSearchParams(href.split("?")[1] ?? ""), [href]);
    },
  };
}

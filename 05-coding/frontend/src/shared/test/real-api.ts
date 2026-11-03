// Test-only helper for view-level tests that must run the REAL api path (real-*.ts modules and
// their error-code mapping) with only the HTTP layer faked. Usage in a test file:
//
//   vi.mock("@/shared/api", async (importOriginal) =>
//     (await import("@/shared/test/real-api")).realApiModule(
//       await importOriginal<Record<string, unknown>>(),
//     ),
//   );
//   import { apiMock } from "@/shared/test/real-api";
//
// `withMockData` then always takes the real branch, `api.get/post/patch` are `vi.fn()`s the test
// scripts, and the real `ApiError` class is kept so `instanceof` checks keep working.
import { vi } from "vitest";

export const apiMock = {
  get: vi.fn(),
  download: vi.fn(),
  post: vi.fn(),
  patch: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
};

export function realApiModule(original: Record<string, unknown>) {
  return {
    ...original,
    api: apiMock,
    isMockMode: () => false,
    withMockData: <T>(_mock: () => unknown, real: () => Promise<T>) => real(),
  };
}

export function resetApiMock() {
  apiMock.get.mockReset();
  apiMock.download.mockReset();
  apiMock.post.mockReset();
  apiMock.patch.mockReset();
  apiMock.put.mockReset();
  apiMock.delete.mockReset();
}

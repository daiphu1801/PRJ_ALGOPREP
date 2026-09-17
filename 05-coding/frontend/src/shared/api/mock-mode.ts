// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// App-wide mock/real switch: change NEXT_PUBLIC_MOCK_DATA in .env.local, no code changes needed.
// Entity-specific mock data does NOT live here — each entity keeps its own
// entities/<x>/api/__mock__/ (required by vibecode-pipeline SKILL.md Layer 3, "Mandatory traces"),
// so that `grep -r __mock__ src/` always lists every mock in the repo.
import { env } from "@/shared/config";

export function isMockMode(): boolean {
  return env.mockData;
}

/**
 * Picks the data source based on NEXT_PUBLIC_MOCK_DATA, used at the entities/<x>/api/*.ts boundary.
 * When the prototype graduates (see vibecode-pipeline SKILL.md Layer 3 "Graduation"), drop the
 * withMockData call and its __mock__/ folder, keeping fetchReal as-is.
 */
export async function withMockData<T>(mockFn: () => Promise<T> | T, fetchReal: () => Promise<T>): Promise<T> {
  return isMockMode() ? mockFn() : fetchReal();
}

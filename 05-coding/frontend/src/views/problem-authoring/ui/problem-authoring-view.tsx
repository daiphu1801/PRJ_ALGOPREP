import { getT } from "@/shared/i18n/server";

// Empty shell for the shared 'problem_authoring' screen (A2 + A3, mounted at /instructor/problems/[problemId]
// and /admin/problems/[problemId] — DEC-2026-0825-shared-content-authoring-screens). Pending
// 02-bd/screens/shared/problem_authoring.md to lock the layout — the heaviest screen in the RD (4 tabs,
// with the Technical Spec tab still pending resolution at open question Q4; the testcase-versioning
// panel F2-09 was already resolved at Q2).
export async function ProblemAuthoringView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">problem-authoring</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}

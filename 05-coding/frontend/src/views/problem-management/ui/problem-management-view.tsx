import { getT } from "@/shared/i18n/server";

// Empty shell for the shared 'problem_management' screen (A2 + A3, mounted at /instructor/problems and
// /admin/problems — DEC-2026-0825-shared-content-authoring-screens). Waiting on 02-bd/screens/shared/ to lock the layout.
// Data scope (instructor's own problems vs. the whole bank) is an application/API-layer concern, not this view's.
export async function ProblemManagementView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">problem-management</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}

import { getT } from "@/shared/i18n/server";

// Khung rỗng cho màn dùng chung 'problem_authoring' (A2 + A3, mount ở /instructor/problems/[problemId] và
// /admin/problems/[problemId] — DEC-2026-0825-shared-content-authoring-screens). Chờ
// 02-bd/screens/shared/problem_authoring.md chốt bố cục — màn nặng nhất trong RD (4 tab + tab Đặc tả kỹ
// thuật đang chờ chốt ở Câu hỏi mở Q4, panel phiên bản testcase F2-09 ở Q2 đã chốt).
export async function ProblemAuthoringView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">problem-authoring</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}

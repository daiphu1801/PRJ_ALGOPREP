import { getT } from "@/shared/i18n/server";

// View mẫu dùng i18n phía SERVER (Server Component) — đối chiếu với views/auth dùng phía client.
// Hai đường này chứng minh cả facade shared/i18n và shared/i18n/server đều chạy thật.
export async function ProblemListView() {
  const t = await getT();

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">{t("problemList.title")}</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("common.pendingDesign")}</p>
    </section>
  );
}

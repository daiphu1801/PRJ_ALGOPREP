import { getT } from "@/shared/i18n/server";

// Sample view using i18n on the SERVER side (Server Component) — contrasted with views/auth, which
// uses the client side. Together these two paths prove both the shared/i18n facade and
// shared/i18n/server actually work.
export async function ProblemListView() {
  const t = await getT();

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">{t("problemList.title")}</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("common.pendingDesign")}</p>
    </section>
  );
}

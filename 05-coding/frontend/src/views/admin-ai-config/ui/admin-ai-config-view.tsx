import { getT } from "@/shared/i18n/server";

// Empty shell for the 'admin-ai-config' screen — waiting on 02-bd/screens/ + 03-dd/screens/ to lock
// the layout and real components. Slug source: 01-rd/screens/. Do NOT add business logic here before the DD exists.
export async function AdminAiConfigView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">admin-ai-config</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}

import { getT } from "@/shared/i18n/server";

// Empty shell for the 'admin-system-log' screen — waiting on 02-bd/screens/ + 03-dd/screens/ to lock
// the layout and real components. Slug source: 01-rd/screens/. Do NOT add business logic here before the DD exists.
export async function AdminSystemLogView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">admin-system-log</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}

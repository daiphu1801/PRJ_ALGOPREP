import { getT } from "@/shared/i18n/server";

// Empty shell for the 'profile' screen — waiting on 02-bd/screens/ + 03-dd/screens/ to
// lock down the layout and real components. Slug source: 01-rd/screens/. Do NOT add business logic
// here before the DD exists.
export async function ProfileView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">profile</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}

import { getT } from "@/shared/i18n/server";

// Khung rỗng cho màn hình 'interview-bank-list' — chờ 02-bd/screens/ + 03-dd/screens/ chốt bố cục và
// component thật. Nguồn slug: 01-rd/screens/. KHÔNG thêm logic nghiệp vụ ở đây trước khi có DD.
export async function InterviewBankListView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">interview-bank-list</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}

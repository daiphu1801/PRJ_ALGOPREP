// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { getT } from "@/shared/i18n/server";

/**
 * Applies to EVERY Student screen — 02-bd/screens/users/_shell.md mục 3, decided 2026-09-21 in the
 * same pass as the Admin and Instructor footers.
 *
 * That BD section is explicit that this is a consistency decision, not a reading of the prototype:
 * only 1 of the 11 Student prototypes actually has a `<footer>`
 * (09-layoutBase/Ngân hàng bài toán.dc.html:428). It is written down that way so a later reader
 * does not think the other 10 were overlooked. An earlier pass of this shell got that backwards
 * and logged the footer as an open question — it was already closed.
 *
 * Three regions, contents from the one prototype that has them (dc.html:428-450):
 * left brand + copyright, middle cluster status, right version.
 *
 * The status line is STATIC, dropping the prototype's live "hàng đợi {n} bài" counter — the
 * recommendation in _shell.md mục 6 Q3, so that a footer present on every screen does not make
 * every screen poll judge-orchestration. Revisit in 03-dd/api/judge-orchestration.md.
 */
export async function StudentFooter() {
  const t = await getT("studentShell");

  return (
    <footer className="mt-6 border-t border-[var(--color-border)] pt-4 pb-6">
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
        <div className="min-w-0">
          <p className="flex items-center gap-2 text-[13px] font-semibold text-[var(--color-text)]">
            AlgoPrep
            <span className="rounded border border-[var(--color-admin-warn)] px-1.5 pb-px pt-0.5 font-mono text-[10px] font-semibold tracking-[0.14em] text-[var(--color-admin-warn)]">
              GO-JUDGE
            </span>
          </p>
          <p className="mt-0.5 text-xs text-[var(--color-text-muted)]">
            {t("footer.copyright")}
          </p>
        </div>

        <p className="flex items-center gap-2 text-[12.5px] text-[var(--color-text-muted)]">
          <span
            className="h-1.5 w-1.5 rounded-full bg-[var(--color-success)]"
            aria-hidden="true"
          />
          {t("footer.clusterStatus")}
        </p>

        <p className="ml-auto font-mono text-[12.5px] text-[var(--color-text-subtle)]">
          {t("footer.version")}
        </p>
      </div>
    </footer>
  );
}

// The judge engine the project runs on (DEC-2026-0823-go-judge-default-engine), drawn as a small
// outlined tag (09-layoutBase/Dashboard AlgoPrep.dc.html:53). Shared by the student header and footer
// so the two never drift apart.
export function JudgeEngineTag() {
  return (
    <span className="rounded border border-[var(--color-admin-warn)] px-1.5 pb-px pt-0.5 font-mono text-[10px] font-semibold tracking-[0.14em] text-[var(--color-admin-warn)]">
      GO-JUDGE
    </span>
  );
}

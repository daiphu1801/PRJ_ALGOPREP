// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Generic per-field error line — takes only a message string, no domain knowledge, so it belongs
// in shared/ui per 01-rd/system/frontend_architecture.md section 2.A-B ("chỉ nhận children/prop
// trình bày"). First consumer is features/auth-by-credentials (BD Q1: errors show inline under the
// field, never a toast/modal — 01-rd/screens/shared/auth.md:87).
export function InlineFieldError({ message }: { message?: string }) {
  if (!message) return null;
  return (
    <p role="alert" className="mt-1 text-xs text-[var(--color-danger)]">
      {message}
    </p>
  );
}

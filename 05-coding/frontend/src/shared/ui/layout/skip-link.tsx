// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Skip to main content" link: the first tab stop of each area's shell, hidden until it receives focus.
// Without it a keyboard user tabs through the whole sidebar (a dozen stops) on every page before
// reaching the content. The target is the shell's <main id="main-content" tabIndex={-1}>.
type SkipLinkProps = {
  /** Already-translated text, e.g. "Bỏ qua tới nội dung chính". */
  label: string;
};

export const MAIN_CONTENT_ID = "main-content";

export function SkipLink({ label }: SkipLinkProps) {
  return (
    <a
      href={`#${MAIN_CONTENT_ID}`}
      className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-[60] focus:rounded-xl focus:border focus:border-[var(--color-border)] focus:bg-[var(--color-background)] focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:shadow-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-primary)]"
    >
      {label}
    </a>
  );
}

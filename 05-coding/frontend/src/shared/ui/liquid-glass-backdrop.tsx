// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// The 3 fixed, blurred color blobs behind the Admin shell — this IS the "liquid glass" look that
// makes `.glass-surface` cards (translucent + backdrop-blur) read as glass instead of flat panels.
// Ported 1:1 from 09-layoutBase/Admin - Tổng quan.dc.html:51-53 (position/size/blur radius), using
// the --color-blob-1/2/3 tokens (globals.css) for the actual colors. Purely decorative — no domain
// type, no admin-specific logic — so it lives in shared/ui per
// 01-rd/system/frontend_architecture.md section 2.A-B, even though only the Admin shell uses it
// today.
export function LiquidGlassBackdrop() {
  // No negative z-index: `fixed` already takes this out of normal flow, and being the FIRST
  // child of the shell means later siblings (toolbar/sidebar/main, all default z-index:auto)
  // paint on top of it in document order — simpler and less fragile than relying on a negative
  // z-index resolving correctly against whichever ancestor happens to own the stacking context.
  return (
    <div aria-hidden="true" className="pointer-events-none fixed inset-0 overflow-hidden">
      <div
        className="absolute -left-36 -top-56 h-[620px] w-[620px] rounded-full blur-[100px]"
        style={{ background: `radial-gradient(circle, var(--color-blob-1), transparent 68%)` }}
      />
      <div
        className="absolute -bottom-64 -right-40 h-[720px] w-[720px] rounded-full blur-[110px]"
        style={{ background: `radial-gradient(circle, var(--color-blob-2), transparent 68%)` }}
      />
      <div
        className="absolute left-[55%] top-[35%] h-[520px] w-[520px] rounded-full blur-[130px]"
        style={{ background: `radial-gradient(circle, var(--color-blob-3), transparent 70%)` }}
      />
    </div>
  );
}

import { ChevronRight, type LucideIcon } from "lucide-react";
import { cn } from "@/shared/lib";

type NavGroupHeaderProps = {
  label: string;
  icon: LucideIcon;
  isOpen: boolean;
  /** Icon-only rail mode — hides the label and chevron, centers the icon, shows a `title` tooltip instead. */
  collapsed: boolean;
  onClick: () => void;
};

/**
 * Promoted alongside `NavLink` (2026-09-16, same reasoning: no AlgoPrep business type, only
 * presentation + an `onClick` callback — `01-rd/system/frontend_architecture.md` section 2.A). The
 * expand/collapse chevron and its rotation are generic "toggle group" behavior, not admin-nav
 * specific — this component doesn't know `ADMIN_NAV_GROUPS` exists, the caller owns that state.
 */
export function NavGroupHeader({ label, icon: Icon, isOpen, collapsed, onClick }: NavGroupHeaderProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-expanded={isOpen}
      title={collapsed ? label : undefined}
      className={cn(
        "flex w-full items-center gap-2.5 rounded-md px-3 py-1.5 text-xs font-semibold uppercase tracking-wide text-[var(--color-text-muted)] hover:bg-[var(--color-surface-hover)]",
        collapsed ? "justify-center" : "justify-between",
      )}
    >
      <span className={cn("flex min-w-0 items-center gap-2.5", collapsed && "justify-center")}>
        <Icon aria-hidden="true" className="h-4 w-4 shrink-0" />
        {!collapsed && <span className="truncate normal-case">{label}</span>}
      </span>
      {/* Hidden entirely in the collapsed rail rather than shown rotated-but-cramped. */}
      {!collapsed && (
        <ChevronRight
          aria-hidden="true"
          className={cn("h-3.5 w-3.5 shrink-0 text-[var(--color-text-muted)] transition-transform", isOpen && "rotate-90")}
        />
      )}
    </button>
  );
}

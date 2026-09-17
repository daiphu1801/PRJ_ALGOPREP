import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/shared/lib";

type NavLinkProps = {
  href: string;
  label: string;
  isActive: boolean;
  /** Icon-only rail mode — hides the label, centers the row, shows `label` as a `title` tooltip instead. */
  collapsed: boolean;
  /** Omit for a plain text row (nested sub-items); pass one for a top-level/group-header row. */
  icon?: LucideIcon;
};

/**
 * Promoted from `widgets/app-shell/ui/admin-sidebar.tsx` (2026-09-16) — it took no AlgoPrep
 * business type (`Problem`/`Submission`/admin nav config, etc.), only presentation props
 * (href/label/icon/isActive/collapsed), so per `01-rd/system/frontend_architecture.md` section 2.A
 * ("component có biết về nghiệp vụ không") it belongs in `shared/ui` regardless of how many places
 * use it today. Falls back to the app's generic `--color-*` tokens when the admin-only
 * `--admin-nav-hover`/`--admin-active-border` custom properties aren't defined by an ancestor
 * (i.e. outside `.admin-shell`), so it renders sanely if `student`/`instructor` ever grow a sidebar
 * nav too instead of only ever working inside the Admin shell.
 */
export function NavLink({ href, label, isActive, collapsed, icon: Icon }: NavLinkProps) {
  return (
    <Link
      href={href}
      title={collapsed ? label : undefined}
      aria-current={isActive ? "page" : undefined}
      className={cn(
        "block rounded-md border border-transparent px-3 py-1.5 text-sm transition-colors",
        Icon ? "flex items-center gap-2.5" : collapsed && "truncate text-center",
        Icon && collapsed && "justify-center",
        isActive
          ? "bg-[var(--color-primary)] text-[var(--color-on-primary)] [border-color:var(--admin-active-border,transparent)]"
          : "text-[var(--color-text)] hover:bg-[var(--admin-nav-hover,var(--color-surface-hover))]",
      )}
    >
      {Icon && <Icon aria-hidden="true" className="h-4 w-4 shrink-0" />}
      {Icon ? !collapsed && <span className="min-w-0 flex-1 truncate">{label}</span> : label}
    </Link>
  );
}

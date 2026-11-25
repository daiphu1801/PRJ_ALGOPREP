import type { ReactNode } from "react";
import { useT } from "@/shared/i18n";
import { Button, SegmentedTabs } from "@/shared/ui";
import type { Role } from "../api";

export function PermissionRoleFilter({
  roles,
  value,
  onValueChange,
  onAddRole,
  addRoleFields,
  roleAction,
}: {
  roles: readonly Role[];
  value: string;
  onValueChange: (value: string) => void;
  onAddRole: () => void;
  addRoleFields?: ReactNode;
  roleAction: ReactNode;
}) {
  const t = useT("adminPermissionMatrix");

  return (
    <div className="sticky top-0 z-20 mb-3 min-w-0 shrink-0 space-y-2 rounded-2xl border border-[var(--color-border)] bg-[var(--color-background)] px-3 py-2.5 shadow-sm">
      <div className="flex min-w-0 flex-wrap items-center gap-2">
        <SegmentedTabs
          className="min-w-0 [&>button]:h-9"
          label={t("roleSwitcherLabel")}
          value={value}
          onValueChange={onValueChange}
          options={roles.map((role) => ({
            value: role.key,
            label: role.label,
          }))}
        />
        {!addRoleFields ? (
          <Button
            variant="ghost"
            size="sm"
            onClick={onAddRole}
            className="border border-dashed border-[var(--color-border)]"
          >
            {t("addRole")}
          </Button>
        ) : null}
        {roleAction}
      </div>
      {addRoleFields}
    </div>
  );
}

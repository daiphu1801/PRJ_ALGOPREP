// Last step of SHR0204: publish the rows that passed and, in the instructor area, give them to a class.
// Mounted only while open, so each opening starts with no class chosen.
"use client";

import { useState } from "react";
import { useInstructorClasses } from "@/entities/class";
import { useT } from "@/shared/i18n";
import { ConfirmDialog, SelectField } from "@/shared/ui";

type ClassOption = { id: string; name: string };

type Props = {
  count: number;
  /** Only an instructor has classes (F2-12). */
  canAssign: boolean;
  onClose: () => void;
  onConfirm: (target: ClassOption | null) => void;
};

export function ImportPublishDialog({
  count,
  canAssign,
  onClose,
  onConfirm,
}: Props) {
  const t = useT("problemImport");
  const [target, setTarget] = useState<ClassOption | null>(null);
  return (
    <ConfirmDialog
      open
      onClose={onClose}
      onConfirm={() => onConfirm(target)}
      title={t("publish.title", { count })}
      confirmLabel={t("publish.confirm", { count })}
      cancelLabel={t("cancel")}
    >
      <p className="mb-3">{t("publish.body")}</p>
      {canAssign ? <ClassPicker value={target} onChange={setTarget} /> : null}
    </ConfirmDialog>
  );
}

function ClassPicker({
  value,
  onChange,
}: {
  value: ClassOption | null;
  onChange: (target: ClassOption | null) => void;
}) {
  const t = useT("problemImport");
  const query = useInstructorClasses();
  const classes = query.data ?? [];
  return (
    <>
      <SelectField
        label={t("publish.classLabel")}
        value={value?.id ?? ""}
        disabled={classes.length === 0}
        onChange={(event) =>
          onChange(
            classes.find((item) => item.id === event.target.value) ?? null,
          )
        }
        options={[
          { value: "", label: t("publish.noClass") },
          ...classes.map((item) => ({ value: item.id, label: item.name })),
        ]}
      />
      {query.isError ? (
        <p className="mt-1 text-xs">{t("publish.classesFailed")}</p>
      ) : null}
    </>
  );
}

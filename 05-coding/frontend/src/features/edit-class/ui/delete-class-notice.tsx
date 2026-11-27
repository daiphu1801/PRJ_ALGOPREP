// Body of the "Xoá lớp" confirmation, shared by the class list and the class detail (INS0201, INS0205): says what
// the instructor is about to lose, so a hard delete is never confirmed blind (plan 06-plan/nexa-plan/261125-2330-lop-hoc-xu-ly-case-can-thiet.md, D3).
// Mount it only while the dialog is open: it reads the problems given to the class then.
"use client";

import { useClassAssignedProblems } from "@/entities/class-assignment";
import { useT } from "@/shared/i18n";

export function DeleteClassNotice({
  classId,
  studentCount,
}: {
  classId: string;
  studentCount: number;
}) {
  const t = useT("classManagement");
  const assigned = useClassAssignedProblems(classId);
  return (
    <>
      <p>{t("popup.deleteClassBody")}</p>
      <p className="mt-2 font-medium text-[var(--color-text)]">
        {assigned.data
          ? t("popup.deleteClassImpact", {
              students: studentCount,
              problems: assigned.data.length,
            })
          : t("popup.deleteClassImpactStudents", { students: studentCount })}
      </p>
    </>
  );
}

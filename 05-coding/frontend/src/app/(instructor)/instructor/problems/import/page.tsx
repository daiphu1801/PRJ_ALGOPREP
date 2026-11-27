import { ProblemImportView } from "@/views/shared/problem-management";

// F2-19, shared A2 and A3 screen (DEC-2026-1125-bulk-problem-import). Only an instructor has classes to assign to.
export default function Page() {
  return <ProblemImportView basePath="/instructor/problems" canAssign />;
}

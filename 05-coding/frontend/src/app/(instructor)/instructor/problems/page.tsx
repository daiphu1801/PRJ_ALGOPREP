import { ProblemManagementView } from "@/views/shared/problem-management";

// Shared A2+A3 screen (DEC-2026-0825-shared-content-authoring-screens); topic management stays ADMIN-only.
export default function Page() {
  return <ProblemManagementView basePath="/instructor/problems" />;
}

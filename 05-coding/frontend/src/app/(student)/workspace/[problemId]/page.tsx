import { ProblemDetailView } from "@/views/users/problem-detail";

// The solving workspace itself (editor + console). The statement-only page lives at /problems/[problemId].
export default function Page() {
  return <ProblemDetailView />;
}

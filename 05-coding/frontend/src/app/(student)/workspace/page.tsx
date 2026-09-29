import { redirect } from "next/navigation";
import { fetchProblemListPage } from "@/entities/problem";

/**
 * The id-less "Workspace" nav destination. Behaviour is already settled by
 * 02-bd/screens/users/_shell.md mục 6 Q2 (closed 2026-09-22,
 * DEC-2026-0922-users-and-admin-conflict-resolutions): open the most recent unfinished draft, and
 * fall back to the problem list when there is none.
 *
 * A redirect rather than a screen of its own: `problem_detail` IS the workspace, so a second
 * screen here would only be a chooser for something the list already does better.
 *
 * PROTOTYPE — the "most recent draft" comes from the mock catalogue. The real source is
 * `judge.submissions` per that same Q2, once 03-dd/api/judge-orchestration.md exists.
 */
export default function Page() {
  const [latestDraft] = fetchProblemListPage().inProgress;
  redirect(latestDraft ? `/problems/${latestDraft.problemId}` : "/problems");
}

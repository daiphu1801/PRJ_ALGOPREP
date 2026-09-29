import { MockInterviewView } from "@/views/users/mock-interview";

/**
 * The id-less "Phỏng vấn giả lập" nav destination (02-bd/screens/users/_shell.md mục 2.2, row 4).
 *
 * Same view as /submissions/[submissionId]/interview: `MockInterviewView` opens on its own entry
 * screen where the learner picks a source and configures the session, and never read the route's
 * submissionId. Both routes therefore render it unchanged rather than one of them wrapping it.
 */
export default function Page() {
  return <MockInterviewView />;
}

// Opened in a new tab from the authoring screen; shows the last saved copy (BD SHR0202 Q6).
import { ProblemPreviewView } from "@/views/shared/problem-preview";

export default async function Page({ params }: { params: Promise<{ problemId: string }> }) {
  const { problemId } = await params;
  return <ProblemPreviewView problemId={problemId} />;
}

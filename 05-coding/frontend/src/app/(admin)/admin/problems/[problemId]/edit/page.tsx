import { ProblemAuthoringView } from "@/views/shared/problem-authoring";

export default async function Page({
  params,
}: {
  params: Promise<{ problemId: string }>;
}) {
  const { problemId } = await params;
  return (
    <ProblemAuthoringView basePath="/admin/problems" problemId={problemId} />
  );
}

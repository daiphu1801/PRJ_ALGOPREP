// Read-only detail (DEC-2026-1002-split-detail-and-edit-pages). The edit form lives at ./edit.
import { ProblemInfoView } from "@/views/shared/problem-info";

export default async function Page({
  params,
}: {
  params: Promise<{ problemId: string }>;
}) {
  const { problemId } = await params;
  return (
    <ProblemInfoView problemId={problemId} basePath="/instructor/problems" />
  );
}

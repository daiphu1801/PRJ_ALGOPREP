// Read-only detail. The edit form moved to ./edit; list rows link here, the "Sửa" button links there.
import { ProblemInfoView } from "@/views/shared/problem-info";

export default async function Page({ params }: { params: Promise<{ problemId: string }> }) {
  const { problemId } = await params;
  return <ProblemInfoView problemId={problemId} basePath="/admin/problems" />;
}

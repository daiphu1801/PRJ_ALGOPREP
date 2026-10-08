import { ProblemAuthoringView } from "@/views/shared/problem-authoring";

// `?from=<code>` opens a duplicate of that problem (F2-16), unsaved until the author presses Save.
export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ from?: string }>;
}) {
  const { from } = await searchParams;
  return <ProblemAuthoringView basePath="/admin/problems" fromId={from} />;
}

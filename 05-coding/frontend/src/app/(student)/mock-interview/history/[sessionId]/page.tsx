import { MockInterviewSessionDetailView } from "@/views/users/mock-interview";

export default async function Page({
  params,
}: {
  params: Promise<{ sessionId: string }>;
}) {
  const { sessionId } = await params;
  return <MockInterviewSessionDetailView sessionId={sessionId} />;
}

import { ClassDetailView } from "@/views/teacher/class-detail";

export default async function Page({
  params,
}: {
  params: Promise<{ classId: string }>;
}) {
  const { classId } = await params;
  return <ClassDetailView classId={classId} />;
}

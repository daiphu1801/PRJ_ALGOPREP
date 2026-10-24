import { MyClassDetailView } from "@/views/users/my-class-detail";

export default async function Page({
  params,
}: {
  params: Promise<{ classId: string }>;
}) {
  const { classId } = await params;
  return <MyClassDetailView classId={classId} />;
}

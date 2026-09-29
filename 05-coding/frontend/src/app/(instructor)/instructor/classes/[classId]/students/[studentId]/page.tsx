import { ClassStudentDetailView } from "@/views/teacher/class-student-detail";

type PageProps = { params: Promise<{ classId: string; studentId: string }> };

export default async function Page({ params }: PageProps) {
  const { classId, studentId } = await params;
  return <ClassStudentDetailView classId={classId} studentId={studentId} />;
}

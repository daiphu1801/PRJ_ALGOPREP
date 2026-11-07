// Real calls of the class cluster (03-dd/api/F1_identity.md #19 to #27, #25A, #25B). Built and used:
// the instructor's own classes, invite codes, roster and removal (INS0201, INS0205), and the learner's
// join and own classes (USR0701, USR0702). Not built, so not here: #28, #29 and #56 to #59 (they need
// F4 metrics); the screens that read them keep running on mock data.
import { api } from "@/shared/api";
import { formatDateVN, formatRelativeVN } from "@/shared/lib";
import type {
  ClassForm,
  ClassRosterRow,
  ClassSchedule,
  ClassSummary,
  InviteCode,
  JoinedClass,
  MyClass,
  StudentStatus,
} from "../model/types";

const CLASSES = "/identity/classes";

/** `ScheduleSpec` as the server sends it: parts the class does not have come back as null. */
type ScheduleDto = {
  days?: number[] | null;
  startTime?: string | null;
  endTime?: string | null;
  room?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  note?: string | null;
};

type ClassDto = {
  id: string;
  name: string;
  description: string | null;
  schedule: ScheduleDto | null;
  studentCount: number;
  createdAt: string;
};

type InviteCodeDto = { id: string; code: string; expiresAt: string };

export function toSchedule(
  dto: ScheduleDto | null | undefined,
): ClassSchedule | undefined {
  if (!dto) return undefined;
  const schedule: ClassSchedule = {
    days: dto.days ?? [],
    startTime: dto.startTime ?? undefined,
    endTime: dto.endTime ?? undefined,
    room: dto.room ?? undefined,
    startDate: dto.startDate ?? undefined,
    endDate: dto.endDate ?? undefined,
    note: dto.note ?? undefined,
  };
  // `{}` is how the server stores "no schedule".
  const empty =
    schedule.days.length === 0 &&
    !schedule.startTime &&
    !schedule.room &&
    !schedule.startDate &&
    !schedule.note;
  return empty ? undefined : schedule;
}

/** Only the parts that are set go out; an empty object clears the schedule on PATCH. */
function scheduleBody(schedule: ClassSchedule | undefined): ScheduleDto {
  if (!schedule) return {};
  return Object.fromEntries(
    Object.entries(schedule).filter(
      ([, value]) => value !== undefined && value !== "",
    ),
  );
}

export function toClassSummary(dto: ClassDto): ClassSummary {
  return {
    id: dto.id,
    name: dto.name,
    description: dto.description ?? undefined,
    schedule: toSchedule(dto.schedule),
    studentCount: dto.studentCount,
    createdAt: dto.createdAt,
  };
}

function toInviteCode(dto: InviteCodeDto): InviteCode {
  return {
    id: dto.id,
    code: dto.code,
    expiresAtLabel: `Còn hạn tới ${formatDateVN(dto.expiresAt)}`,
  };
}

/** #19: the instructor's classes, newest first; no completion, average or absent figures (F4). */
export async function realListInstructorClasses(): Promise<ClassSummary[]> {
  const { items } = await api.get<{ items: ClassDto[] }>(`${CLASSES}/my`);
  return items.map(toClassSummary);
}

/** #20: creates the class and its first invite code in one transaction. */
export async function realCreateClass(form: ClassForm): Promise<ClassSummary> {
  const created = await api.post<Omit<ClassDto, "id"> & { classId: string }>(
    CLASSES,
    {
      name: form.name,
      ...(form.description ? { description: form.description } : {}),
      ...(form.schedule ? { schedule: scheduleBody(form.schedule) } : {}),
    },
  );
  return toClassSummary({ ...created, id: created.classId });
}

/**
 * #21: a PATCH keeps what is absent, so the form's blanks are sent as the clearing value: an empty
 * description clears it, `{}` clears the schedule.
 */
export async function realUpdateClass(
  id: string,
  form: ClassForm,
): Promise<ClassSummary> {
  return toClassSummary(
    await api.patch<ClassDto>(`${CLASSES}/${id}`, {
      name: form.name,
      description: form.description ?? "",
      schedule: scheduleBody(form.schedule),
    }),
  );
}

/** #22: 204; the invite codes and the enrolments go with the class. */
export async function realDeleteClass(id: string): Promise<void> {
  await api.delete(`${CLASSES}/${id}`);
}

/** #24: the codes that have not expired, newest first. */
export async function realListInviteCodes(
  classId: string,
): Promise<InviteCode[]> {
  const { items } = await api.get<{ items: InviteCodeDto[] }>(
    `${CLASSES}/${classId}/invite-codes`,
  );
  return items.map(toInviteCode);
}

/** #23: another code, valid for 7 days; the older ones stay valid. */
export async function realCreateInviteCode(
  classId: string,
): Promise<InviteCode> {
  return toInviteCode(
    await api.post<InviteCodeDto>(`${CLASSES}/${classId}/invite-codes`),
  );
}

/** #26: who is in the class now, with the judge figures when the server could read them. */
export async function realListRoster(
  classId: string,
): Promise<ClassRosterRow[]> {
  const { items } = await api.get<{
    items: {
      studentId: string;
      displayName: string;
      email: string | null;
      phone: string | null;
      joinedAt: string;
      status: string | null;
      avgScore: number | null;
      completionPercent: number | null;
      lastSubmittedAt: string | null;
    }[];
  }>(`${CLASSES}/${classId}/students`);
  return items.map((student) => ({
    id: student.studentId,
    classId,
    name: student.displayName,
    email: student.email ?? undefined,
    phone: student.phone ?? undefined,
    joinedAt: student.joinedAt,
    metrics: student.status
      ? {
          status: student.status.toLowerCase() as StudentStatus,
          avgScore: student.avgScore,
          completionPct: student.completionPercent,
          lastActiveLabel: student.lastSubmittedAt
            ? formatRelativeVN(student.lastSubmittedAt)
            : "-",
        }
      : undefined,
  }));
}

/** #27: marks the learner as left (the history stays); repeating it is harmless. */
export async function realLeaveClass(input: {
  classId: string;
  studentId: string;
}): Promise<void> {
  await api.post(
    `${CLASSES}/${input.classId}/students/${input.studentId}/leave`,
  );
}

type MyClassDto = {
  classId: string;
  name: string;
  description: string;
  schedule: ScheduleDto | null;
  instructorName: string;
  joinedAt: string;
};

function toMyClass(dto: MyClassDto): MyClass {
  return {
    id: dto.classId,
    name: dto.name,
    description: dto.description || undefined,
    schedule: toSchedule(dto.schedule),
    instructorName: dto.instructorName,
    joinedAtLabel: formatDateVN(dto.joinedAt),
  };
}

/** #25A: the classes the learner is in now, newest join first. */
export async function realListMyClasses(): Promise<MyClass[]> {
  const { items } = await api.get<{ items: MyClassDto[] }>(
    "/identity/me/classes",
  );
  return items.map(toMyClass);
}

/** #25B: one class of the learner; IDT-411 when it is not theirs, gone, or left. */
export async function realGetMyClass(classId: string): Promise<MyClass> {
  return toMyClass(
    await api.get<MyClassDto>(`/identity/me/classes/${classId}`),
  );
}

/** #25: joins by invite code; a wrong or expired code is IDT-410, too many wrong ones IDT-412. */
export async function realJoinClass(code: string): Promise<JoinedClass> {
  const joined = await api.post<{ classId: string; className: string }>(
    `${CLASSES}/join`,
    { code },
  );
  return { id: joined.classId, name: joined.className };
}

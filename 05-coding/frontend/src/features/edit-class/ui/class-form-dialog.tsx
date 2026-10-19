// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Create / edit class popup (BD INS0201 Popup Tạo/Sửa lớp), shared by the class list and the class
// detail page, so it lives in a feature slice instead of being copied into a second view.
"use client";

import { useState } from "react";
import type { ClassSchedule, ClassSummary } from "@/entities/class";
import type { useT } from "@/shared/i18n";
import { toastFirstError } from "@/shared/lib/toast-store";
import { Button, Modal, TextArea, TextField } from "@/shared/ui";

export type ClassDialogState =
  { mode: "create" } | { mode: "edit"; target: ClassSummary } | null;

// BD INS0201 Sheet 6: name is required, and the text fields have length limits (name 120, description
// 500). The schedule is structured since 2026-10-11 (owner): weekdays, time range, room, period, note.
const NAME_MAX = 120;
const DESCRIPTION_MAX = 500;
const ROOM_MAX = 100;
const NOTE_MAX = 200;
const WEEKDAYS = [1, 2, 3, 4, 5, 6, 7];

export type ClassFormValue = {
  name: string;
  description?: string;
  schedule?: ClassSchedule;
};

export function ClassFormDialog({
  dialog,
  onClose,
  onSubmit,
  t,
}: {
  dialog: ClassDialogState;
  onClose: () => void;
  onSubmit: (form: ClassFormValue) => void;
  t: ReturnType<typeof useT>;
}) {
  // The parent re-mounts this component (via `key`) for every open, so the initial state below is
  // the class being edited, not whatever the previous open left behind.
  const editing = dialog?.mode === "edit" ? dialog.target : null;
  const sched = editing?.schedule;
  const [name, setName] = useState(editing?.name ?? "");
  const [description, setDescription] = useState(editing?.description ?? "");
  const [days, setDays] = useState<number[]>(sched?.days ?? []);
  const [startTime, setStartTime] = useState(sched?.startTime ?? "");
  const [endTime, setEndTime] = useState(sched?.endTime ?? "");
  const [room, setRoom] = useState(sched?.room ?? "");
  const [startDate, setStartDate] = useState(sched?.startDate ?? "");
  const [endDate, setEndDate] = useState(sched?.endDate ?? "");
  const [note, setNote] = useState(sched?.note ?? "");
  const [touched, setTouched] = useState(false);

  if (!dialog) return null;

  const nameEmpty = !name.trim();
  const nameTooLong = name.length > NAME_MAX;
  const descriptionTooLong = description.length > DESCRIPTION_MAX;
  const roomTooLong = room.length > ROOM_MAX;
  const noteTooLong = note.length > NOTE_MAX;
  const timePairMissing = Boolean(startTime) !== Boolean(endTime);
  const timeOrderBad = Boolean(startTime && endTime) && endTime <= startTime;
  const daysMissing = Boolean(startTime || endTime) && days.length === 0;
  const dateOrderBad = Boolean(startDate && endDate) && endDate < startDate;

  function toggleDay(day: number) {
    setDays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day],
    );
  }

  function submit() {
    setTouched(true);
    const problems: string[] = [];
    if (nameEmpty) problems.push(t("validation.nameRequired"));
    else if (nameTooLong || descriptionTooLong || roomTooLong || noteTooLong)
      problems.push(t("validation.tooLong"));
    else if (daysMissing) problems.push(t("validation.scheduleDaysRequired"));
    else if (timePairMissing) problems.push(t("validation.timePairRequired"));
    else if (timeOrderBad) problems.push(t("validation.timeOrder"));
    else if (dateOrderBad) problems.push(t("validation.dateOrder"));
    if (problems.length > 0) {
      toastFirstError(problems);
      return;
    }
    const schedule: ClassSchedule = {
      days: [...days].sort((x, y) => x - y),
      startTime: startTime || undefined,
      endTime: endTime || undefined,
      room: room.trim() || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      note: note.trim() || undefined,
    };
    const hasSchedule =
      schedule.days.length > 0 ||
      schedule.startTime ||
      schedule.room ||
      schedule.startDate ||
      schedule.note;
    onSubmit({
      name: name.trim(),
      description: description.trim() || undefined,
      schedule: hasSchedule ? schedule : undefined,
    });
  }

  return (
    <Modal
      open
      onClose={onClose}
      className="max-w-xl"
      title={
        dialog.mode === "create"
          ? t("popup.createClassTitle")
          : t("popup.editClassTitle")
      }
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            {t("cancel")}
          </Button>
          <Button size="sm" onClick={submit}>
            {t("popup.saveClass")}
          </Button>
        </>
      }
    >
      <div className="flex max-h-[70vh] flex-col gap-3 overflow-y-auto p-1 -m-1">
        <TextField
          label={t("popup.classNameLabel")}
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          invalid={touched && (nameEmpty || nameTooLong)}
        />
        <TextArea
          label={t("popup.descriptionLabel")}
          value={description}
          rows={3}
          onChange={(e) => setDescription(e.target.value)}
          invalid={touched && descriptionTooLong}
        />

        <fieldset className="flex flex-col gap-3 rounded-lg border border-[var(--color-border)] p-3">
          <legend className="px-1 text-[12.5px] font-semibold text-[var(--color-text)]">
            {t("schedule.heading")}
          </legend>
          <div>
            <p className="mb-1.5 text-[12.5px] text-[var(--color-text-muted)]">
              {t("schedule.daysLabel")}
            </p>
            <div
              role="group"
              aria-label={t("schedule.daysLabel")}
              className="flex flex-wrap gap-1.5"
            >
              {WEEKDAYS.map((day) => {
                const on = days.includes(day);
                return (
                  <button
                    key={day}
                    type="button"
                    role="checkbox"
                    aria-checked={on}
                    onClick={() => toggleDay(day)}
                    className={
                      "h-8 min-w-10 rounded-md border px-2 text-[12.5px] font-semibold transition-colors " +
                      (on
                        ? "border-transparent bg-[var(--color-primary)] text-[var(--color-on-primary)]"
                        : "border-[var(--color-border)] text-[var(--color-text-muted)] hover:bg-[var(--color-surface-hover)]") +
                      (touched && daysMissing && !on
                        ? " border-[var(--color-admin-negative)]"
                        : "")
                    }
                  >
                    {t(`schedule.dayShort.${day}`)}
                  </button>
                );
              })}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <TextField
              type="time"
              label={t("schedule.startTime")}
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              invalid={touched && (timePairMissing || timeOrderBad)}
            />
            <TextField
              type="time"
              label={t("schedule.endTime")}
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              invalid={touched && (timePairMissing || timeOrderBad)}
            />
          </div>
          <TextField
            label={t("schedule.room")}
            value={room}
            onChange={(e) => setRoom(e.target.value)}
            invalid={touched && roomTooLong}
          />
          <div className="grid grid-cols-2 gap-3">
            <TextField
              type="date"
              label={t("schedule.startDate")}
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              invalid={touched && dateOrderBad}
            />
            <TextField
              type="date"
              label={t("schedule.endDate")}
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              invalid={touched && dateOrderBad}
            />
          </div>
          <TextField
            label={t("schedule.note")}
            value={note}
            onChange={(e) => setNote(e.target.value)}
            invalid={touched && noteTooLong}
          />
        </fieldset>
      </div>
    </Modal>
  );
}

package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.domain.model.ScheduleSpec;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Form rules of a class: name IDT-V14, description IDT-V15, schedule IDT-V10 and IDT-V16. */
final class ClassValidation {
  private static final int NAME_MAX = 120;
  private static final int DESCRIPTION_MAX = 500;
  private static final int ROOM_MAX = 100;
  private static final int NOTE_MAX = 200;
  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

  private ClassValidation() {}

  /** Required, 1 to 120 characters once trimmed. */
  static String name(String sent) {
    String value = sent == null ? "" : sent.trim();
    if (value.isEmpty() || value.length() > NAME_MAX) {
      throw new DomainRuleViolationException("IDT-V14", "Class name must be 1 to 120 characters");
    }
    return value;
  }

  /** Optional, at most 500 characters; empty after trimming means no description. */
  static String description(String sent) {
    String value = sent == null ? "" : sent.trim();
    if (value.length() > DESCRIPTION_MAX) {
      throw new DomainRuleViolationException("IDT-V15", "Description is too long");
    }
    return value.isEmpty() ? null : value;
  }

  /** Returns the normalised schedule, or null when nothing is set (an empty object clears). */
  static ScheduleSpec schedule(ScheduleSpec sent) {
    if (sent == null) {
      return null;
    }
    String room = limited(sent.room(), ROOM_MAX, "Room is too long");
    String note = limited(sent.note(), NOTE_MAX, "Note is too long");
    List<Integer> days = days(sent.days());
    LocalTime start = time(sent.startTime());
    LocalTime end = time(sent.endTime());
    if ((start == null) != (end == null)) {
      throw invalid("Start and end time must be given together");
    }
    if (start != null && (!end.isAfter(start) || days.isEmpty())) {
      throw invalid("End time must be after start time and at least one day is needed");
    }
    LocalDate from = date(sent.startDate());
    LocalDate to = date(sent.endDate());
    if (from != null && to != null && to.isBefore(from)) {
      throw invalid("End date must not be before start date");
    }
    if (days.isEmpty()
        && start == null
        && from == null
        && to == null
        && room == null
        && note == null) {
      return null;
    }
    return new ScheduleSpec(
        days,
        start == null ? null : TIME.format(start),
        end == null ? null : TIME.format(end),
        room,
        from == null ? null : from.toString(),
        to == null ? null : to.toString(),
        note);
  }

  private static String limited(String sent, int max, String message) {
    String value = sent == null ? "" : sent.trim();
    if (value.length() > max) {
      throw new DomainRuleViolationException("IDT-V10", message);
    }
    return value.isEmpty() ? null : value;
  }

  private static List<Integer> days(List<Integer> sent) {
    if (sent == null) {
      return List.of();
    }
    Set<Integer> seen = new HashSet<>();
    for (Integer day : sent) {
      if (day == null || day < 1 || day > 7 || !seen.add(day)) {
        throw invalid("Days must be distinct numbers from 1 to 7");
      }
    }
    return sent.stream().sorted().toList();
  }

  private static LocalTime time(String sent) {
    if (sent == null || sent.isBlank()) {
      return null;
    }
    try {
      return LocalTime.parse(sent.trim(), TIME);
    } catch (DateTimeParseException e) {
      throw invalid("Time must be HH:mm");
    }
  }

  private static LocalDate date(String sent) {
    if (sent == null || sent.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(sent.trim());
    } catch (DateTimeParseException e) {
      throw invalid("Date must be yyyy-MM-dd");
    }
  }

  private static DomainRuleViolationException invalid(String message) {
    return new DomainRuleViolationException("IDT-V16", message);
  }
}

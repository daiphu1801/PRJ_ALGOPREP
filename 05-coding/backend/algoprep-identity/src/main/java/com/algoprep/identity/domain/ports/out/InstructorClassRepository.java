package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.ClassInviteCode;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.model.ScheduleSpec;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Storage of classes, invite codes and enrolments for the instructor side (#19 to #27). */
public interface InstructorClassRepository {
  void create(UUID id, UUID instructorId, String name, String description, ScheduleSpec schedule);

  /** Newest first, id ascending on a tie. */
  List<InstructorClass> listOwned(UUID instructorId);

  /** Empty when the class does not exist or belongs to someone else. */
  Optional<InstructorClass> findOwned(UUID classId, UUID instructorId);

  void update(UUID classId, String name, String description, ScheduleSpec schedule);

  /** Hard delete; false when the class is missing or not owned. Children cascade in the DB. */
  boolean deleteOwned(UUID classId, UUID instructorId);

  boolean isOwner(UUID classId, UUID instructorId);

  /** Expiry is set by the database clock (now + 7 days); empty when the code already exists. */
  Optional<ClassInviteCode> insertInviteCode(UUID id, UUID classId, UUID createdBy, String code);

  /** Unexpired codes only, newest first. */
  List<ClassInviteCode> listActiveInviteCodes(UUID classId);

  /** Enrolments with {@code left_at IS NULL}. */
  List<ClassRosterEntry> listActiveRoster(UUID classId);

  /**
   * Marks the student as left and returns the stored {@code left_at}. A student who already left
   * keeps the original moment. Empty when the student never had an enrolment in the class.
   */
  Optional<Instant> markLeft(UUID classId, UUID studentId);
}

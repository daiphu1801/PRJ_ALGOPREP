package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.identity.domain.model.ScheduleSpec;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * F1 class management persistence: instructor-owned classes, invite codes, roster, student view.
 */
class JdbcClassRepositoriesTest {
  private final FakeDb db = new FakeDb();
  private final JdbcInstructorClassRepository instructor =
      new JdbcInstructorClassRepository(db.provider());
  private final JdbcStudentClassRepository student = new JdbcStudentClassRepository(db.provider());
  private final UUID id = UUID.randomUUID();
  private final UUID owner = UUID.randomUUID();
  private final ScheduleSpec schedule =
      new ScheduleSpec(List.of(2, 4), "08:00", "10:00", "A1", "2026-01-01", "2026-06-01", "n");

  @Test
  void createBindsScheduleAsTextParts() {
    instructor.create(id, owner, "Algo", "d", schedule);
    assertThat(db.last().sql()).contains("INSERT INTO identity.classes");
    assertThat(db.last().values()).contains(id, owner, "Algo", "2,4", "08:00", "A1");
  }

  @Test
  void createWithoutScheduleBindsNulls() {
    instructor.create(id, owner, "Algo", null, null);
    assertThat(db.last().values()).containsNull();
    instructor.create(
        id, owner, "Algo", null, new ScheduleSpec(List.of(), null, null, null, null, null, null));
    assertThat(db.last().values()).doesNotContain("");
  }

  @Test
  void listAndFindOwnedMapClassWithSchedule() {
    db.rows(
        "FROM identity.classes c",
        Map.of(
            "id",
            id,
            "name",
            "Algo",
            "has_schedule",
            true,
            "days",
            List.of(1, 3),
            "student_count",
            7,
            "created_at",
            OffsetDateTime.parse("2026-02-01T00:00:00Z")));
    var list = instructor.listOwned(owner);
    assertThat(list).hasSize(1);
    assertThat(list.get(0).schedule().days()).containsExactly(1, 3);
    assertThat(list.get(0).studentCount()).isEqualTo(7);
    assertThat(instructor.findOwned(id, owner)).isPresent();
    assertThat(db.last().values()).containsExactly(id, owner);
  }

  @Test
  void mapClassWithoutScheduleHasNullSchedule() {
    db.rows("FROM identity.classes c", Map.of("has_schedule", false, "id", id));
    assertThat(instructor.listOwned(owner).get(0).schedule()).isNull();
    assertThat(new JdbcInstructorClassRepository(new FakeDb().provider()).findOwned(id, owner))
        .isEmpty();
  }

  @Test
  void updateDeleteAndOwnership() {
    instructor.update(id, "N", "d", schedule);
    assertThat(db.last().sql()).contains("UPDATE identity.classes SET name");
    assertThat(instructor.deleteOwned(id, owner)).isTrue();
    db.updateCount("DELETE FROM identity.classes", 0);
    assertThat(instructor.deleteOwned(id, owner)).isFalse();
    db.rows("SELECT EXISTS", Map.of("exists", true));
    assertThat(instructor.isOwner(id, owner)).isTrue();
  }

  @Test
  void inviteCodeInsertListAndConflict() {
    assertThat(instructor.insertInviteCode(id, id, owner, "ABC")).isEmpty();
    db.rows("RETURNING id, code, expires_at", Map.of("id", id, "code", "ABC"));
    assertThat(instructor.insertInviteCode(id, id, owner, "ABC"))
        .get()
        .extracting("code")
        .isEqualTo("ABC");
    db.rows("FROM identity.class_invite_codes\n", Map.of("id", id, "code", "Z"));
    assertThat(instructor.listActiveInviteCodes(id)).hasSize(1);
  }

  @Test
  void rosterMapsStudents() {
    db.generic("FROM identity.class_enrollments e JOIN", 2);
    assertThat(instructor.listActiveRoster(id)).hasSize(2);
  }

  @Test
  void markLeftReturnsTimestampFromUpdateOrFallsBackToEarlierLeave() {
    db.rows("RETURNING left_at", Map.of("left_at", OffsetDateTime.parse("2026-03-01T00:00:00Z")));
    assertThat(instructor.markLeft(id, owner)).isPresent();
    assertThat(db.calls()).hasSize(1);
    FakeDb db2 =
        new FakeDb()
            .rows(
                "left_at IS NOT NULL",
                Map.of("left_at", OffsetDateTime.parse("2026-03-02T00:00:00Z")));
    var repo2 = new JdbcInstructorClassRepository(db2.provider());
    assertThat(repo2.markLeft(id, owner)).isPresent();
    assertThat(db2.calls()).hasSize(2);
    assertThat(new JdbcInstructorClassRepository(new FakeDb().provider()).markLeft(id, owner))
        .isEmpty();
  }

  @Test
  void joinByCodeEnrolsOnlyWhenCodeIsValid() {
    assertThat(student.joinByCode(owner, "BAD")).isEmpty();
    assertThat(db.calls()).hasSize(1);
    db.rows("FROM identity.class_invite_codes i", Map.of("id", id, "name", "Algo"));
    assertThat(student.joinByCode(owner, "OK")).get().extracting("name").isEqualTo("Algo");
    assertThat(db.last().sql()).contains("INSERT INTO identity.class_enrollments");
    assertThat(db.last().values()).contains(id, owner);
  }

  @Test
  void studentListsParseScheduleJson() {
    db.rows(
        "FROM identity.class_enrollments e",
        Map.of(
            "id",
            id,
            "name",
            "A",
            "description",
            "",
            "schedule",
            "{\"room\":\"A1\"}",
            "instructor_name",
            "T",
            "joined_at",
            java.sql.Timestamp.valueOf("2026-01-01 00:00:00")),
        Map.of("id", id, "name", "B", "schedule", "null"));
    var all = student.listActive(owner);
    assertThat(all).hasSize(2);
    assertThat(all.get(0).schedule()).isEqualTo(Map.of("room", "A1"));
    FakeDb one = new FakeDb().generic("FROM identity.class_enrollments e", 1);
    one.rows("FROM identity.class_enrollments e", Map.of("schedule", "{}"));
    assertThat(new JdbcStudentClassRepository(one.provider()).findActive(owner, id)).isPresent();
  }
}

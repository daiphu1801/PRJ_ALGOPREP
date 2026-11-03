package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.ClassInviteCode;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.model.ScheduleSpec;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The schedule jsonb is built and read in SQL, so no JSON library is needed here. Timestamps come
 * from the database clock.
 */
@Repository
class JdbcInstructorClassRepository implements InstructorClassRepository {
  /** Builds the jsonb from bound text values; null parts are stripped, an empty object is NULL. */
  private static final String SCHEDULE_JSON =
      """
      NULLIF(jsonb_strip_nulls(jsonb_build_object(
        'days', to_jsonb(string_to_array(CAST(:days AS text), ',')::int[]),
        'startTime', CAST(:startTime AS text),
        'endTime', CAST(:endTime AS text),
        'room', CAST(:room AS text),
        'startDate', CAST(:startDate AS text),
        'endDate', CAST(:endDate AS text),
        'note', CAST(:note AS text))), '{}'::jsonb)
      """;

  private static final String CLASS_COLUMNS =
      """
      SELECT c.id, c.name, c.description, c.created_at, c.schedule IS NOT NULL AS has_schedule,
             ARRAY(SELECT jsonb_array_elements_text(c.schedule->'days')::int) AS days,
             c.schedule->>'startTime' AS start_time, c.schedule->>'endTime' AS end_time,
             c.schedule->>'room' AS room, c.schedule->>'startDate' AS start_date,
             c.schedule->>'endDate' AS end_date, c.schedule->>'note' AS note,
             (SELECT count(*) FROM identity.class_enrollments e
               WHERE e.class_id = c.id AND e.left_at IS NULL) AS student_count
      FROM identity.classes c
      """;

  private static final RowMapper<InstructorClass> CLASS_MAPPER =
      (rs, n) -> {
        ScheduleSpec schedule = null;
        if (rs.getBoolean("has_schedule")) {
          List<Integer> days = new ArrayList<>();
          for (Object day : (Object[]) rs.getArray("days").getArray()) {
            days.add(((Number) day).intValue());
          }
          schedule =
              new ScheduleSpec(
                  days,
                  rs.getString("start_time"),
                  rs.getString("end_time"),
                  rs.getString("room"),
                  rs.getString("start_date"),
                  rs.getString("end_date"),
                  rs.getString("note"));
        }
        return new InstructorClass(
            rs.getObject("id", UUID.class),
            rs.getString("name"),
            rs.getString("description"),
            schedule,
            rs.getInt("student_count"),
            instant(rs.getObject("created_at", OffsetDateTime.class)));
      };

  private static final RowMapper<ClassInviteCode> CODE_MAPPER =
      (rs, n) ->
          new ClassInviteCode(
              rs.getObject("id", UUID.class),
              rs.getString("code"),
              instant(rs.getObject("expires_at", OffsetDateTime.class)));

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcInstructorClassRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  private static Instant instant(OffsetDateTime time) {
    return time.toInstant();
  }

  private static String daysText(ScheduleSpec s) {
    return s == null || s.days() == null || s.days().isEmpty()
        ? null
        : s.days().stream().map(String::valueOf).collect(Collectors.joining(","));
  }

  private static JdbcClient.StatementSpec bindSchedule(
      JdbcClient.StatementSpec spec, ScheduleSpec s) {
    return spec.param("days", daysText(s))
        .param("startTime", s == null ? null : s.startTime())
        .param("endTime", s == null ? null : s.endTime())
        .param("room", s == null ? null : s.room())
        .param("startDate", s == null ? null : s.startDate())
        .param("endDate", s == null ? null : s.endDate())
        .param("note", s == null ? null : s.note());
  }

  @Override
  public void create(
      UUID id, UUID instructorId, String name, String description, ScheduleSpec schedule) {
    bindSchedule(
            jdbc.getObject()
                .sql(
                    "INSERT INTO identity.classes (id, instructor_id, name, description, schedule)"
                        + " VALUES (:id, :instructor, :name, :description, "
                        + SCHEDULE_JSON
                        + ")")
                .param("id", id)
                .param("instructor", instructorId)
                .param("name", name)
                .param("description", description),
            schedule)
        .update();
  }

  @Override
  public List<InstructorClass> listOwned(UUID instructorId) {
    return jdbc.getObject()
        .sql(CLASS_COLUMNS + " WHERE c.instructor_id = :id ORDER BY c.created_at DESC, c.id ASC")
        .param("id", instructorId)
        .query(CLASS_MAPPER)
        .list();
  }

  @Override
  public Optional<InstructorClass> findOwned(UUID classId, UUID instructorId) {
    return jdbc.getObject()
        .sql(CLASS_COLUMNS + " WHERE c.id = :id AND c.instructor_id = :instructor")
        .param("id", classId)
        .param("instructor", instructorId)
        .query(CLASS_MAPPER)
        .optional();
  }

  @Override
  public void update(UUID classId, String name, String description, ScheduleSpec schedule) {
    bindSchedule(
            jdbc.getObject()
                .sql(
                    "UPDATE identity.classes SET name = :name, description = :description,"
                        + " schedule = "
                        + SCHEDULE_JSON
                        + ", updated_at = clock_timestamp() WHERE id = :id")
                .param("id", classId)
                .param("name", name)
                .param("description", description),
            schedule)
        .update();
  }

  @Override
  public boolean deleteOwned(UUID classId, UUID instructorId) {
    return jdbc.getObject()
            .sql("DELETE FROM identity.classes WHERE id = :id AND instructor_id = :instructor")
            .param("id", classId)
            .param("instructor", instructorId)
            .update()
        > 0;
  }

  @Override
  public boolean isOwner(UUID classId, UUID instructorId) {
    return jdbc.getObject()
        .sql(
            "SELECT EXISTS (SELECT 1 FROM identity.classes WHERE id = :id"
                + " AND instructor_id = :instructor)")
        .param("id", classId)
        .param("instructor", instructorId)
        .query(Boolean.class)
        .single();
  }

  @Override
  public Optional<ClassInviteCode> insertInviteCode(
      UUID id, UUID classId, UUID createdBy, String code) {
    return jdbc.getObject()
        .sql(
            """
            INSERT INTO identity.class_invite_codes (id, class_id, code, expires_at, created_by)
            VALUES (:id, :classId, :code, now() + interval '7 days', :createdBy)
            ON CONFLICT (code) DO NOTHING
            RETURNING id, code, expires_at
            """)
        .param("id", id)
        .param("classId", classId)
        .param("code", code)
        .param("createdBy", createdBy)
        .query(CODE_MAPPER)
        .optional();
  }

  @Override
  public List<ClassInviteCode> listActiveInviteCodes(UUID classId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT id, code, expires_at FROM identity.class_invite_codes
            WHERE class_id = :id AND expires_at > now()
            ORDER BY created_at DESC, id ASC
            """)
        .param("id", classId)
        .query(CODE_MAPPER)
        .list();
  }

  @Override
  public List<ClassRosterEntry> listActiveRoster(UUID classId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT u.id, u.display_name, u.email, u.phone, e.joined_at
            FROM identity.class_enrollments e JOIN identity.users u ON u.id = e.student_id
            WHERE e.class_id = :id AND e.left_at IS NULL
            ORDER BY e.joined_at ASC, u.id ASC
            """)
        .param("id", classId)
        .query(
            (rs, n) ->
                new ClassRosterEntry(
                    rs.getObject("id", UUID.class),
                    rs.getString("display_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    instant(rs.getObject("joined_at", OffsetDateTime.class))))
        .list();
  }

  @Override
  public Optional<Instant> markLeft(UUID classId, UUID studentId) {
    JdbcClient client = jdbc.getObject();
    Optional<Instant> marked =
        client
            .sql(
                "UPDATE identity.class_enrollments SET left_at = clock_timestamp()"
                    + " WHERE class_id = :c AND student_id = :s AND left_at IS NULL"
                    + " RETURNING left_at")
            .param("c", classId)
            .param("s", studentId)
            .query((rs, n) -> instant(rs.getObject("left_at", OffsetDateTime.class)))
            .optional();
    if (marked.isPresent()) {
      return marked;
    }
    return client
        .sql(
            "SELECT left_at FROM identity.class_enrollments"
                + " WHERE class_id = :c AND student_id = :s AND left_at IS NOT NULL")
        .param("c", classId)
        .param("s", studentId)
        .query((rs, n) -> instant(rs.getObject("left_at", OffsetDateTime.class)))
        .optional();
  }
}

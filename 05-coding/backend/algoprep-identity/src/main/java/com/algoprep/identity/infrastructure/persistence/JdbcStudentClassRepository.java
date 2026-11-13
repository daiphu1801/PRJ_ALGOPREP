package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.ClassRef;
import com.algoprep.identity.domain.model.MyClass;
import com.algoprep.identity.domain.ports.out.StudentClassRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Reads only {@code classes}, {@code class_enrollments} and the instructor's display name. */
@Repository
class JdbcStudentClassRepository implements StudentClassRepository {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String SELECT =
      """
      SELECT c.id, c.name, COALESCE(c.description, '') AS description,
             c.schedule::text AS schedule, u.display_name AS instructor_name, e.joined_at
      FROM identity.class_enrollments e
      JOIN identity.classes c ON c.id = e.class_id
      JOIN identity.users u ON u.id = c.instructor_id
      WHERE e.student_id = :student AND e.left_at IS NULL
      """;
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcStudentClassRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<ClassRef> joinByCode(UUID studentId, String code) {
    JdbcClient client = jdbc.getObject();
    Optional<ClassRef> found =
        client
            .sql(
                """
        SELECT c.id, c.name
        FROM identity.class_invite_codes i JOIN identity.classes c ON c.id = i.class_id
        WHERE i.code = :code AND i.expires_at > now()
        """)
            .param("code", code)
            .query((rs, n) -> new ClassRef(rs.getObject("id", UUID.class), rs.getString("name")))
            .optional();
    found.ifPresent(
        ref ->
            client
                .sql(
                    """
        INSERT INTO identity.class_enrollments (id, class_id, student_id)
        VALUES (:id, :classId, :student)
        ON CONFLICT (class_id, student_id)
        DO UPDATE SET left_at = NULL WHERE class_enrollments.left_at IS NOT NULL
        """)
                .param("id", UUID.randomUUID())
                .param("classId", ref.id())
                .param("student", studentId)
                .update());
    return found;
  }

  @Override
  public List<MyClass> listActive(UUID studentId) {
    return jdbc.getObject()
        .sql(SELECT + " ORDER BY e.joined_at DESC, c.id ASC")
        .param("student", studentId)
        .query((rs, n) -> map(rs))
        .list();
  }

  @Override
  public Optional<MyClass> findActive(UUID studentId, UUID classId) {
    return jdbc.getObject()
        .sql(SELECT + " AND c.id = :classId")
        .param("student", studentId)
        .param("classId", classId)
        .query((rs, n) -> map(rs))
        .optional();
  }

  private static MyClass map(ResultSet rs) throws SQLException {
    String schedule = rs.getString("schedule");
    return new MyClass(
        rs.getObject("id", UUID.class),
        rs.getString("name"),
        rs.getString("description"),
        schedule == null ? null : JSON.readValue(schedule, Object.class),
        rs.getString("instructor_name"),
        rs.getTimestamp("joined_at").toInstant());
  }
}

package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.SubmissionClassScope;
import com.algoprep.identity.domain.ports.out.SubmissionClassScopeRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSubmissionClassScopeRepository implements SubmissionClassScopeRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcSubmissionClassScopeRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<SubmissionClassScope> read(UUID instructorId) {
    return jdbc.getObject()
        .sql(
            """
        SELECT c.id, c.name, e.student_id, u.display_name, e.joined_at, e.left_at
        FROM identity.classes c
        LEFT JOIN identity.class_enrollments e ON e.class_id=c.id
        LEFT JOIN identity.users u ON u.id=e.student_id
        WHERE c.instructor_id=:instructor ORDER BY c.id, e.student_id
        """)
        .param("instructor", instructorId)
        .query(
            (rs, n) -> {
              var joined = rs.getObject("joined_at", OffsetDateTime.class);
              var left = rs.getObject("left_at", OffsetDateTime.class);
              return new SubmissionClassScope(
                  rs.getObject("id", UUID.class),
                  rs.getString("name"),
                  rs.getObject("student_id", UUID.class),
                  rs.getString("display_name"),
                  joined == null ? null : joined.toInstant(),
                  left == null ? null : left.toInstant());
            })
        .list();
  }
}

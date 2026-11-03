package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.ClassAssignmentSnapshot;
import com.algoprep.problembank.domain.ports.out.ClassAssignmentReadRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcClassAssignmentReadRepository implements ClassAssignmentReadRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcClassAssignmentReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<ClassAssignmentSnapshot> findAssignments(Set<UUID> classIds) {
    if (classIds.isEmpty()) {
      return List.of();
    }
    return jdbc.getObject()
        .sql(
            """
            SELECT a.class_id, a.problem_id, p.title, a.assigned_at, a.removed_at
            FROM problem.class_assignments a JOIN problem.problems p ON p.id=a.problem_id
            WHERE a.class_id IN (:classIds)
            ORDER BY a.class_id, a.problem_id, a.assigned_at, a.id
            """)
        .param("classIds", classIds)
        .query(
            (rs, row) -> {
              Timestamp removed = rs.getTimestamp("removed_at");
              return new ClassAssignmentSnapshot(
                  rs.getObject("class_id", UUID.class),
                  rs.getObject("problem_id", UUID.class),
                  rs.getString("title"),
                  rs.getTimestamp("assigned_at").toInstant(),
                  removed == null ? null : removed.toInstant());
            })
        .list();
  }
}

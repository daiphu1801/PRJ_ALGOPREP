package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.StudentClassModels.MyAssignment;
import com.algoprep.problembank.domain.ports.out.StudentClassAssignmentReadRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStudentClassAssignmentReadRepository implements StudentClassAssignmentReadRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcStudentClassAssignmentReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<MyAssignment> active(Set<UUID> classIds, boolean publishedOnly) {
    if (classIds.isEmpty()) {
      return List.of();
    }
    return jdbc.getObject()
        .sql(
            """
            SELECT a.class_id, a.problem_id, p.code, p.title, l.display_name, a.assigned_at,
                   (p.deleted OR p.status <> 'PUBLISHED') AS withdrawn
            FROM problem.class_assignments a
            JOIN problem.problems p ON p.id = a.problem_id
            JOIN problem.problem_levels l ON l.id = p.level_id
            WHERE a.class_id IN (:classIds) AND a.removed_at IS NULL
              AND (NOT CAST(:publishedOnly AS boolean) OR (p.status = 'PUBLISHED' AND NOT p.deleted))
            ORDER BY a.class_id, a.assigned_at DESC, a.problem_id
            """)
        .param("classIds", classIds)
        .param("publishedOnly", publishedOnly)
        .query(
            (rs, row) ->
                new MyAssignment(
                    rs.getObject("class_id", UUID.class),
                    rs.getObject("problem_id", UUID.class),
                    rs.getString("code"),
                    rs.getString("title"),
                    rs.getString("display_name"),
                    rs.getTimestamp("assigned_at").toInstant(),
                    rs.getBoolean("withdrawn")))
        .list();
  }
}

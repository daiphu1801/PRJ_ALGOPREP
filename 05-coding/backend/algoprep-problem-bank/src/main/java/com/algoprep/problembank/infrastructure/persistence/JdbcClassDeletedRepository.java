package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.ports.out.ClassDeletedRepository;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcClassDeletedRepository implements ClassDeletedRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcClassDeletedRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public int removeAssignments(UUID classId) {
    return jdbc.getObject()
        .sql(
            """
        UPDATE problem.class_assignments SET removed_at = now()
        WHERE class_id = :classId AND removed_at IS NULL
        """)
        .param("classId", classId)
        .update();
  }
}

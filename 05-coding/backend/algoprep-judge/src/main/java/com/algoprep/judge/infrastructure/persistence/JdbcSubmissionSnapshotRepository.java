package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.AcceptedSubmissionSnapshot;
import com.algoprep.judge.domain.model.SubmissionSnapshotLookup;
import com.algoprep.judge.domain.ports.out.SubmissionSnapshotRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSubmissionSnapshotRepository implements SubmissionSnapshotRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcSubmissionSnapshotRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<SubmissionSnapshotLookup> find(UUID submissionId) {
    return jdbc.getObject()
        .sql(
            """
        SELECT id,user_id,problem_id,language,status,
          CASE WHEN status='ACCEPTED' THEN source_code ELSE NULL END AS source_code,
          runtime_ms,memory_kb FROM judge.submissions WHERE id=:id
        """)
        .param("id", submissionId)
        .query(
            (rs, n) -> {
              String status = rs.getString("status");
              AcceptedSubmissionSnapshot snapshot = null;
              if ("ACCEPTED".equals(status)) {
                snapshot =
                    new AcceptedSubmissionSnapshot(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("problem_id", UUID.class),
                        rs.getString("language"),
                        rs.getString("source_code"),
                        rs.getObject("runtime_ms", Integer.class),
                        rs.getObject("memory_kb", Integer.class));
              }
              return new SubmissionSnapshotLookup(status, snapshot);
            })
        .optional();
  }
}

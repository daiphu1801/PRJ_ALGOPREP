package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.LearnerSubmissionCandidate;
import com.algoprep.judge.domain.ports.out.LearnerSubmissionListRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcLearnerSubmissionListRepository implements LearnerSubmissionListRepository {
  private static final String COLUMNS =
      "id,problem_id,language,status,passed_testcase_count,total_testcase_count,runtime_ms,memory_kb,submitted_at";
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcLearnerSubmissionListRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<LearnerSubmissionCandidate> inProgress(
      UUID student, LearnerSubmissionCandidate.Cursor after, int batchSize) {
    String sql =
        "WITH latest AS (SELECT DISTINCT ON (problem_id) "
            + COLUMNS
            + " FROM judge.submissions WHERE user_id=:student ORDER BY problem_id,submitted_at DESC,id DESC)"
            + " SELECT "
            + COLUMNS
            + " FROM latest WHERE status<>'ACCEPTED'"
            + (after == null ? "" : " AND (submitted_at,id)<(CAST(:at AS timestamptz),:id)")
            + " ORDER BY submitted_at DESC,id DESC LIMIT :limit";
    var query = jdbc.getObject().sql(sql).param("student", student).param("limit", batchSize);
    if (after != null) {
      query = query.param("at", after.submittedAt().toString()).param("id", after.submissionId());
    }
    return query.query(JdbcLearnerSubmissionListRepository::candidate).list();
  }

  @Override
  public List<LearnerSubmissionCandidate> accepted(UUID student, int limit) {
    return jdbc.getObject()
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM judge.submissions WHERE user_id=:student AND status='ACCEPTED' ORDER BY submitted_at DESC,id DESC LIMIT :limit")
        .param("student", student)
        .param("limit", limit)
        .query(JdbcLearnerSubmissionListRepository::candidate)
        .list();
  }

  private static LearnerSubmissionCandidate candidate(ResultSet rs, int ignored)
      throws SQLException {
    return new LearnerSubmissionCandidate(
        rs.getObject("id", UUID.class),
        rs.getObject("problem_id", UUID.class),
        rs.getString("language"),
        rs.getString("status"),
        rs.getObject("passed_testcase_count", Integer.class),
        rs.getObject("total_testcase_count", Integer.class),
        rs.getObject("runtime_ms", Integer.class),
        rs.getObject("memory_kb", Integer.class),
        rs.getObject("submitted_at", OffsetDateTime.class).toInstant());
  }
}

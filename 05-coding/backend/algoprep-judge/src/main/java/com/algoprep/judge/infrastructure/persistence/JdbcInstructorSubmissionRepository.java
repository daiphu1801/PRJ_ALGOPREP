package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.InstructorReadFilter;
import com.algoprep.judge.domain.model.ScopedSubmission;
import com.algoprep.judge.domain.ports.out.InstructorSubmissionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
class JdbcInstructorSubmissionRepository implements InstructorSubmissionRepository {
  private static final String SCOPE =
      """
      WITH windows AS (
        SELECT * FROM jsonb_to_recordset(CAST(:windows AS jsonb)) AS w(
          "classId" uuid,"studentId" uuid,"problemId" uuid,"joinedAt" timestamptz,
          "leftAt" timestamptz,"assignedAt" timestamptz,"removedAt" timestamptz)),
      candidates AS (
        SELECT s.id,w."classId" AS class_id,
          row_number() OVER(PARTITION BY s.id ORDER BY w."assignedAt" DESC,w."classId") AS rn
        FROM judge.submissions s JOIN windows w ON s.user_id=w."studentId"
          AND s.problem_id=w."problemId" AND s.submitted_at>=w."joinedAt"
          AND (w."leftAt" IS NULL OR s.submitted_at<=w."leftAt")
          AND s.submitted_at>=w."assignedAt"
          AND (w."removedAt" IS NULL OR s.submitted_at<w."removedAt"))
      """;
  private final ObjectProvider<JdbcClient> jdbc;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

  JdbcInstructorSubmissionRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public long count(InstructorReadFilter f) {
    return query("SELECT count(*)", f, "").query(Long.class).single();
  }

  @Override
  public List<ScopedSubmission> list(InstructorReadFilter f, int offset, int limit) {
    return query(
            """
        SELECT s.id,c.class_id,s.user_id,s.problem_id,s.language,s.status,
        s.passed_testcase_count,s.total_testcase_count,s.score_ratio,s.submitted_at
        """,
            f,
            " ORDER BY s.submitted_at DESC,s.id DESC OFFSET :offset LIMIT :limit")
        .param("offset", offset)
        .param("limit", limit)
        .query(
            (rs, n) ->
                new ScopedSubmission(
                    rs.getObject("id", UUID.class),
                    rs.getObject("class_id", UUID.class),
                    rs.getObject("user_id", UUID.class),
                    rs.getObject("problem_id", UUID.class),
                    rs.getString("language"),
                    rs.getString("status"),
                    rs.getInt("passed_testcase_count"),
                    rs.getObject("total_testcase_count", Integer.class),
                    rs.getBigDecimal("score_ratio"),
                    rs.getObject("submitted_at", OffsetDateTime.class).toInstant()))
        .list();
  }

  private JdbcClient.StatementSpec query(String select, InstructorReadFilter f, String tail) {
    String where = " FROM candidates c JOIN judge.submissions s ON s.id=c.id WHERE c.rn=1";
    if (f.classId() != null) {
      where += " AND c.class_id=:class";
    }
    if (f.submissionId() != null) {
      where += " AND s.id=:id";
    }
    if ("accepted".equals(f.result())) {
      where += " AND s.status='ACCEPTED'";
    }
    if ("failed".equals(f.result())) {
      where +=
          " AND s.status IN ('WRONG_ANSWER','TIME_LIMIT_EXCEEDED','RUNTIME_ERROR','COMPILE_ERROR')";
    }
    if (f.keywordStudents() != null) {
      where +=
          " AND ("
              + (f.keywordStudents().isEmpty() ? "false" : "s.user_id IN (:students)")
              + " OR "
              + (f.keywordProblems().isEmpty() ? "false" : "s.problem_id IN (:problems)")
              + ")";
    }
    var q =
        jdbc.getObject()
            .sql(SCOPE + select + where + tail)
            .param("windows", json.writeValueAsString(f.windows()));
    if (f.classId() != null) {
      q = q.param("class", f.classId());
    }
    if (f.submissionId() != null) {
      q = q.param("id", f.submissionId());
    }
    if (f.keywordStudents() != null && !f.keywordStudents().isEmpty()) {
      q = q.param("students", f.keywordStudents());
    }
    if (f.keywordProblems() != null && !f.keywordProblems().isEmpty()) {
      q = q.param("problems", f.keywordProblems());
    }
    return q;
  }
}

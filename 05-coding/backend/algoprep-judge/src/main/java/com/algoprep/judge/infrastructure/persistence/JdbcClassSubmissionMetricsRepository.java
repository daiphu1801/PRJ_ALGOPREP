package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.ClassMetricSubmission;
import com.algoprep.judge.domain.model.ClassMetricsScope;
import com.algoprep.judge.domain.ports.out.ClassSubmissionMetricsRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
class JdbcClassSubmissionMetricsRepository implements ClassSubmissionMetricsRepository {
  private static final String SQL =
      """
      WITH students AS (
        SELECT * FROM jsonb_to_recordset(CAST(:students AS jsonb))
        AS x("studentId" uuid,"joinedAt" timestamptz,"leftAt" timestamptz)),
      assignments AS (
        SELECT * FROM jsonb_to_recordset(CAST(:assignments AS jsonb))
        AS x("problemId" uuid,"assignedAt" timestamptz,"removedAt" timestamptz))
      SELECT s.id,s.user_id,s.problem_id,s.status,s.score_ratio,s.submitted_at
      FROM judge.submissions s JOIN students st ON st."studentId"=s.user_id
      WHERE s.submitted_at>=st."joinedAt"
        AND (st."leftAt" IS NULL OR s.submitted_at<=st."leftAt")
        AND s.submitted_at<=:asOf
        AND EXISTS (SELECT 1 FROM assignments a WHERE a."problemId"=s.problem_id
          AND s.submitted_at>=a."assignedAt"
          AND (a."removedAt" IS NULL OR s.submitted_at<a."removedAt"))
      """;
  private final ObjectProvider<JdbcClient> jdbc;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

  JdbcClassSubmissionMetricsRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Instant now() {
    return jdbc.getObject()
        .sql("SELECT clock_timestamp()")
        .query(OffsetDateTime.class)
        .single()
        .toInstant();
  }

  @Override
  public List<ClassMetricSubmission> find(ClassMetricsScope scope, Instant asOf) {
    if (scope.students().isEmpty() || scope.assignments().isEmpty()) {
      return List.of();
    }
    return jdbc.getObject()
        .sql(SQL)
        .param("students", json.writeValueAsString(scope.students()))
        .param("assignments", json.writeValueAsString(scope.assignments()))
        .param("asOf", asOf.atOffset(java.time.ZoneOffset.UTC))
        .query(
            (rs, index) ->
                new ClassMetricSubmission(
                    rs.getObject("id", UUID.class),
                    rs.getObject("user_id", UUID.class),
                    rs.getObject("problem_id", UUID.class),
                    rs.getString("status"),
                    rs.getBigDecimal("score_ratio"),
                    rs.getObject("submitted_at", OffsetDateTime.class).toInstant()))
        .list();
  }
}

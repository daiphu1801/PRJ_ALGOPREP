package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.AssignmentMetricScope;
import com.algoprep.judge.domain.model.AssignmentPeriod;
import com.algoprep.judge.domain.model.AssignmentSolveState;
import com.algoprep.judge.domain.model.ClassProblemMetric;
import com.algoprep.judge.domain.ports.out.AssignmentMetricsRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
class JdbcAssignmentMetricsRepository implements AssignmentMetricsRepository {
  private static final String PROBLEM_METRICS =
      """
      WITH windows AS (
        SELECT * FROM jsonb_to_recordset(CAST(:windows AS jsonb)) AS w(
          "classId" uuid,"studentId" uuid,"problemId" uuid,"joinedAt" timestamptz,
          "leftAt" timestamptz,"assignedAt" timestamptz,"removedAt" timestamptz)),
      candidates AS (
        SELECT s.id,s.problem_id,s.status,w."classId" AS class_id,
          row_number() OVER(PARTITION BY s.id ORDER BY w."assignedAt" DESC,w."classId") AS rn
        FROM judge.submissions s JOIN windows w ON s.user_id=w."studentId"
          AND s.problem_id=w."problemId" AND s.submitted_at>=w."joinedAt"
          AND (w."leftAt" IS NULL OR s.submitted_at<=w."leftAt")
          AND s.submitted_at>=w."assignedAt"
          AND (w."removedAt" IS NULL OR s.submitted_at<w."removedAt")
        WHERE s.submitted_at<=statement_timestamp())
      SELECT problem_id,count(*) AS total,
        count(*) FILTER(WHERE status IN
          ('ACCEPTED','WRONG_ANSWER','TIME_LIMIT_EXCEEDED','RUNTIME_ERROR','COMPILE_ERROR')) AS eligible,
        count(*) FILTER(WHERE status='ACCEPTED') AS accepted
      FROM candidates WHERE rn=1 AND class_id IN (:classes) GROUP BY problem_id
      """;
  private static final String ASSIGNMENT_STATES =
      """
      WITH periods AS (
        SELECT * FROM jsonb_to_recordset(CAST(:periods AS jsonb)) AS a(
          "classId" uuid,"problemId" uuid,"assignedAt" timestamptz,"removedAt" timestamptz))
      SELECT a."classId" AS class_id,a."problemId" AS problem_id,a."assignedAt" AS assigned_at,
        CASE WHEN bool_or(s.status='ACCEPTED') THEN 'SOLVED'
          WHEN count(s.id)>0 THEN 'ATTEMPTED' ELSE 'UNSOLVED' END AS state
      FROM periods a LEFT JOIN judge.submissions s
        ON s.user_id=:student AND s.problem_id=a."problemId"
        AND s.submitted_at>=a."assignedAt"
        AND (a."removedAt" IS NULL OR s.submitted_at<a."removedAt")
        AND s.submitted_at<=statement_timestamp()
      GROUP BY a."classId",a."problemId",a."assignedAt"
      """;
  private final ObjectProvider<JdbcClient> jdbc;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

  JdbcAssignmentMetricsRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Map<UUID, ClassProblemMetric> problemMetrics(AssignmentMetricScope scope) {
    Map<UUID, ClassProblemMetric> result = new LinkedHashMap<>();
    scope.problemIds().forEach(id -> result.put(id, ClassProblemMetric.from(0, 0, 0)));
    if (scope.windows().isEmpty() || scope.classIds().isEmpty() || scope.problemIds().isEmpty()) {
      return Map.copyOf(result);
    }
    jdbc.getObject()
        .sql(PROBLEM_METRICS)
        .param("windows", json.writeValueAsString(scope.windows()))
        .param("classes", scope.classIds())
        .query(
            (rs, n) ->
                Map.entry(
                    rs.getObject("problem_id", UUID.class),
                    ClassProblemMetric.from(
                        rs.getLong("total"), rs.getLong("eligible"), rs.getLong("accepted"))))
        .list()
        .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
    return Map.copyOf(result);
  }

  @Override
  public Map<AssignmentPeriod.Key, AssignmentSolveState> assignmentStates(
      UUID student, List<AssignmentPeriod> assignments) {
    if (assignments.isEmpty()) {
      return Map.of();
    }
    Map<AssignmentPeriod.Key, AssignmentSolveState> result = new LinkedHashMap<>();
    jdbc.getObject()
        .sql(ASSIGNMENT_STATES)
        .param("periods", json.writeValueAsString(assignments))
        .param("student", student)
        .query(
            (rs, n) ->
                Map.entry(
                    new AssignmentPeriod.Key(
                        rs.getObject("class_id", UUID.class),
                        rs.getObject("problem_id", UUID.class),
                        rs.getObject("assigned_at", OffsetDateTime.class).toInstant()),
                    AssignmentSolveState.valueOf(rs.getString("state"))))
        .list()
        .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
    return Map.copyOf(result);
  }
}

package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.SubmissionActivityDay;
import com.algoprep.judge.domain.model.TopicSubmissionCount;
import com.algoprep.judge.domain.ports.out.MySubmissionMetricsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
class JdbcMySubmissionMetricsRepository implements MySubmissionMetricsRepository {
  private static final String ELIGIBLE =
      "s.status IN ('ACCEPTED','WRONG_ANSWER','TIME_LIMIT_EXCEEDED','RUNTIME_ERROR','COMPILE_ERROR')";
  private static final String STREAK =
      """
      WITH RECURSIVE chain(day) AS (
        SELECT (CAST(:asOf AS timestamptz) AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
        WHERE EXISTS (SELECT 1 FROM judge.submissions s WHERE s.user_id=:student
          AND s.submitted_at>=
            (date_trunc('day',CAST(:asOf AS timestamptz) AT TIME ZONE 'Asia/Ho_Chi_Minh')
              AT TIME ZONE 'Asia/Ho_Chi_Minh') AND s.submitted_at<=:asOf)
        UNION ALL
        SELECT c.day-1 FROM chain c WHERE EXISTS (
          SELECT 1 FROM judge.submissions s WHERE s.user_id=:student
            AND s.submitted_at>=((c.day-1)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh')
            AND s.submitted_at<(c.day::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh')))
      SELECT count(*) FROM chain
      """;
  private final ObjectProvider<JdbcClient> jdbc;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

  JdbcMySubmissionMetricsRepository(ObjectProvider<JdbcClient> jdbc) {
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
  public int streak(UUID student, Instant asOf) {
    return Math.toIntExact(query(STREAK, student, asOf).query(Long.class).single());
  }

  @Override
  public Instant last(UUID student, Instant asOf) {
    return query(
            "SELECT max(submitted_at) FROM judge.submissions s WHERE s.user_id=:student AND s.submitted_at<=:asOf",
            student,
            asOf)
        .query(OffsetDateTime.class)
        .optional()
        .map(OffsetDateTime::toInstant)
        .orElse(null);
  }

  @Override
  public List<SubmissionActivityDay> days(UUID student, Instant from, Instant asOf) {
    var statement =
        query(
            """
        SELECT (s.submitted_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS day,count(*) AS n
        FROM judge.submissions s WHERE s.user_id=:student AND s.submitted_at<=:asOf
        """
                + (from == null ? "" : " AND s.submitted_at>=:from")
                + " GROUP BY day ORDER BY day",
            student,
            asOf);
    if (from != null) {
      statement = statement.param("from", from.atOffset(ZoneOffset.UTC));
    }
    return statement
        .query(
            (rs, n) ->
                new SubmissionActivityDay(rs.getObject("day", LocalDate.class), rs.getLong("n")))
        .list();
  }

  @Override
  public Map<UUID, TopicSubmissionCount> topics(
      UUID student, Map<UUID, Set<UUID>> links, Instant from, Instant asOf) {
    Map<UUID, TopicSubmissionCount> result = new LinkedHashMap<>();
    links.keySet().forEach(topic -> result.put(topic, new TopicSubmissionCount(0, 0, null)));
    var pairs = json.createArrayNode();
    links.forEach(
        (topic, problems) ->
            problems.forEach(
                problem ->
                    pairs
                        .addObject()
                        .put("topicId", topic.toString())
                        .put("problemId", problem.toString())));
    if (pairs.isEmpty()) {
      return Map.copyOf(result);
    }
    var statement =
        query(
                """
        WITH links AS (SELECT * FROM jsonb_to_recordset(CAST(:links AS jsonb))
          AS x("topicId" uuid,"problemId" uuid))
        SELECT l."topicId" AS topic_id,
        """
                    + " count(*) FILTER(WHERE "
                    + ELIGIBLE
                    + ") AS eligible,"
                    + " count(*) FILTER(WHERE s.status='ACCEPTED') AS ac,max(s.submitted_at) AS last_at"
                    + " FROM links l JOIN judge.submissions s ON s.problem_id=l.\"problemId\""
                    + " WHERE s.user_id=:student AND s.submitted_at<=:asOf"
                    + (from == null ? "" : " AND s.submitted_at>=:from")
                    + " GROUP BY l.\"topicId\"",
                student,
                asOf)
            .param("links", json.writeValueAsString(pairs));
    if (from != null) {
      statement = statement.param("from", from.atOffset(ZoneOffset.UTC));
    }
    statement
        .query(
            (rs, n) ->
                Map.entry(
                    rs.getObject("topic_id", UUID.class),
                    new TopicSubmissionCount(
                        rs.getLong("eligible"),
                        rs.getLong("ac"),
                        rs.getObject("last_at", OffsetDateTime.class).toInstant())))
        .list()
        .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
    return Map.copyOf(result);
  }

  private JdbcClient.StatementSpec query(String sql, UUID student, Instant asOf) {
    return jdbc.getObject()
        .sql(sql)
        .param("student", student)
        .param("asOf", asOf.atOffset(ZoneOffset.UTC));
  }
}

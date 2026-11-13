package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.IdleSession;
import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.RubricCriterion;
import com.algoprep.aireview.domain.model.RubricRow;
import com.algoprep.aireview.domain.model.RubricScore;
import com.algoprep.aireview.domain.ports.out.InterviewScoringRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcInterviewScoringRepository implements InterviewScoringRepository {
  private static final String RUNNING = "stage IN ('EXPLAIN', 'CHALLENGE', 'SCALE_UP')";
  private static final String IDLE =
      " AND updated_at = :observed AND updated_at < now() - make_interval(mins => :idle)";
  private static final String IDLE_SELECT =
      "SELECT id, turn_count, updated_at FROM ai.interview_sessions WHERE "
          + RUNNING
          + " AND updated_at < now() - make_interval(mins => :idle)";

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcInterviewScoringRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<RubricScore> rubric(UUID sessionId) {
    return jdbc
        .getObject()
        .sql(
            "SELECT criterion_code, score, comment FROM ai.rubric_scores"
                + " WHERE owner_type = 'MOCK_INTERVIEW' AND owner_id = :id")
        .param("id", sessionId)
        .query(
            (rs, row) ->
                new RubricScore(
                    RubricCriterion.valueOf(rs.getString("criterion_code")),
                    rs.getBigDecimal("score"),
                    rs.getString("comment")))
        .list()
        .stream()
        .sorted(Comparator.comparing(RubricScore::criterion))
        .toList();
  }

  @Override
  public boolean markCompleted(UUID id, Instant now, Instant observed, int idleMinutes) {
    return close("COMPLETED", id, now, observed, idleMinutes);
  }

  @Override
  public boolean markAbandoned(UUID id, Instant now, Instant observed, int idleMinutes) {
    return close("ABANDONED", id, now, observed, idleMinutes);
  }

  private boolean close(String stage, UUID id, Instant now, Instant observed, int idleMinutes) {
    var statement =
        jdbc.getObject()
            .sql(
                "UPDATE ai.interview_sessions SET stage = '"
                    + stage
                    + "', ended_at = :now, updated_at = :now WHERE id = :id AND "
                    + RUNNING
                    + (observed == null ? "" : IDLE))
            .param("now", utc(now))
            .param("id", id);
    if (observed != null) {
      statement = statement.param("observed", utc(observed)).param("idle", idleMinutes);
    }
    return statement.update() == 1;
  }

  @Override
  public void insertRubric(
      UUID sessionId, List<RubricScore> scores, Instant now, Supplier<UUID> ids) {
    for (RubricScore s : scores) {
      jdbc.getObject()
          .sql(
              "INSERT INTO ai.rubric_scores (id, owner_type, owner_id, criterion_code, score,"
                  + " comment, created_at) VALUES (:id, 'MOCK_INTERVIEW', :owner, :code, :score,"
                  + " :comment, :now)")
          .param("id", ids.get())
          .param("owner", sessionId)
          .param("code", s.criterion().name())
          .param("score", s.score())
          .param("comment", s.comment())
          .param("now", utc(now))
          .update();
    }
  }

  @Override
  public List<IdleSession> idleSessions(int idleMinutes, int limit) {
    return jdbc.getObject()
        .sql(IDLE_SELECT + " ORDER BY updated_at ASC, id ASC LIMIT :limit")
        .param("idle", idleMinutes)
        .param("limit", limit)
        .query(JdbcInterviewScoringRepository::idle)
        .list();
  }

  @Override
  public Optional<IdleSession> idleSession(UUID sessionId, int idleMinutes) {
    return jdbc.getObject()
        .sql(IDLE_SELECT + " AND id = :id")
        .param("idle", idleMinutes)
        .param("id", sessionId)
        .query(JdbcInterviewScoringRepository::idle)
        .optional();
  }

  @Override
  public List<RubricRow> completedRubrics(UUID userId, Instant from) {
    return jdbc.getObject()
        .sql(
            "SELECT s.id, s.entry_type, s.started_at, s.ended_at, r.criterion_code, r.score"
                + " FROM ai.interview_sessions s JOIN ai.rubric_scores r"
                + " ON r.owner_type = 'MOCK_INTERVIEW' AND r.owner_id = s.id"
                + " WHERE s.user_id = :user AND s.stage = 'COMPLETED'"
                + " AND (CAST(:from AS timestamptz) IS NULL OR s.ended_at >= :from)"
                + " ORDER BY s.ended_at DESC, s.id")
        .param("user", userId)
        .param("from", from == null ? null : utc(from))
        .query(
            (rs, row) ->
                new RubricRow(
                    rs.getObject("id", UUID.class),
                    InterviewEntryType.valueOf(rs.getString("entry_type")),
                    rs.getObject("started_at", OffsetDateTime.class).toInstant(),
                    rs.getObject("ended_at", OffsetDateTime.class).toInstant(),
                    RubricCriterion.valueOf(rs.getString("criterion_code")),
                    rs.getBigDecimal("score")))
        .list();
  }

  private static IdleSession idle(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
    return new IdleSession(
        rs.getObject("id", UUID.class),
        rs.getInt("turn_count"),
        rs.getObject("updated_at", OffsetDateTime.class).toInstant());
  }

  private static OffsetDateTime utc(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}

package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.ports.out.InterviewTurnRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcInterviewTurnRepository implements InterviewTurnRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcInterviewTurnRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void insertTurn(InterviewTurn line) {
    jdbc.getObject()
        .sql(
            "INSERT INTO ai.interview_turns (id, interview_session_id, turn_index, role,"
                + " stage_at_turn, content, created_at)"
                + " VALUES (:id, :session, :idx, :role, :stage, :content, :created)")
        .param("id", line.id())
        .param("session", line.sessionId())
        .param("idx", line.turnIndex())
        .param("role", line.role().name())
        .param("stage", line.stage().name())
        .param("content", line.content())
        .param("created", utc(line.createdAt()))
        .update();
  }

  @Override
  public void updateUserContent(UUID sessionId, int turnIndex, String content) {
    jdbc.getObject()
        .sql(
            "UPDATE ai.interview_turns SET content = :content WHERE interview_session_id = :id"
                + " AND turn_index = :idx AND role = 'USER'")
        .param("content", content)
        .param("id", sessionId)
        .param("idx", turnIndex)
        .update();
  }

  @Override
  public void touch(UUID sessionId, Instant now) {
    jdbc.getObject()
        .sql("UPDATE ai.interview_sessions SET updated_at = :now WHERE id = :id")
        .param("now", utc(now))
        .param("id", sessionId)
        .update();
  }

  @Override
  public boolean advance(UUID sessionId, InterviewSessionStage nextStage, Instant now) {
    return jdbc.getObject()
            .sql(
                "UPDATE ai.interview_sessions SET turn_count = turn_count + 1, stage = :stage,"
                    + " updated_at = :now WHERE id = :id"
                    + " AND stage IN ('EXPLAIN', 'CHALLENGE', 'SCALE_UP')")
            .param("stage", nextStage.name())
            .param("now", utc(now))
            .param("id", sessionId)
            .update()
        == 1;
  }

  private static OffsetDateTime utc(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}

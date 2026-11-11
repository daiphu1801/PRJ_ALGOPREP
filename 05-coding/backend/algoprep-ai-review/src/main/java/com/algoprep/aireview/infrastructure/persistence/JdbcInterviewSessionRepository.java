package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.InterviewStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.model.InterviewerLevel;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
class JdbcInterviewSessionRepository implements InterviewSessionRepository {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String COLUMNS =
      "id, user_id, entry_type, submission_id, question_ref, question_text, topics::text AS topics,"
          + " interviewer_level, max_turns, hint_allowed, hint_count, stage, turn_count,"
          + " started_at, ended_at, updated_at";

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcInterviewSessionRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void insertOpened(InterviewSession s, InterviewTurn opening) {
    jdbc.getObject()
        .sql(
            "INSERT INTO ai.interview_sessions (id, user_id, entry_type, submission_id,"
                + " question_ref, question_text, topics, interviewer_level, max_turns,"
                + " hint_allowed, hint_count, stage, turn_count, started_at, updated_at)"
                + " VALUES (:id, :user, :entry, :submission, :ref, :text, CAST(:topics AS jsonb),"
                + " :level, :maxTurns, :hint, 0, :stage, 0, :started, :updated)")
        .param("id", s.id())
        .param("user", s.userId())
        .param("entry", s.entryType().name())
        .param("submission", s.submissionId())
        .param("ref", s.questionRef())
        .param("text", s.questionText())
        .param("topics", s.entryType() == InterviewEntryType.TOPIC_SELECTION ? topicsJson(s) : null)
        .param("level", s.interviewerLevel() == null ? null : s.interviewerLevel().name())
        .param("maxTurns", s.maxTurns())
        .param("hint", s.hintAllowed())
        .param("stage", s.stage().name())
        .param("started", utc(s.startedAt()))
        .param("updated", utc(s.updatedAt()))
        .update();
    jdbc.getObject()
        .sql(
            "INSERT INTO ai.interview_turns (id, interview_session_id, turn_index, role,"
                + " stage_at_turn, content, created_at)"
                + " VALUES (:id, :session, :idx, :role, :stage, :content, :created)")
        .param("id", opening.id())
        .param("session", opening.sessionId())
        .param("idx", opening.turnIndex())
        .param("role", opening.role().name())
        .param("stage", opening.stage().name())
        .param("content", opening.content())
        .param("created", utc(opening.createdAt()))
        .update();
  }

  @Override
  public Optional<InterviewSession> find(UUID sessionId) {
    return jdbc.getObject()
        .sql("SELECT " + COLUMNS + " FROM ai.interview_sessions WHERE id = :id")
        .param("id", sessionId)
        .query(JdbcInterviewSessionRepository::session)
        .optional();
  }

  @Override
  public List<InterviewTurn> turns(UUID sessionId) {
    return jdbc.getObject()
        .sql(
            "SELECT id, interview_session_id, turn_index, role, stage_at_turn, content, created_at"
                + " FROM ai.interview_turns WHERE interview_session_id = :id ORDER BY turn_index")
        .param("id", sessionId)
        .query(JdbcInterviewSessionRepository::turn)
        .list();
  }

  @Override
  public List<InterviewSession> findAllByUser(UUID userId) {
    return jdbc.getObject()
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM ai.interview_sessions WHERE user_id = :user ORDER BY started_at, id")
        .param("user", userId)
        .query(JdbcInterviewSessionRepository::session)
        .list();
  }

  @Override
  public List<InterviewTurn> turnsOfUser(UUID userId) {
    return jdbc.getObject()
        .sql(
            "SELECT t.id, t.interview_session_id, t.turn_index, t.role, t.stage_at_turn,"
                + " t.content, t.created_at FROM ai.interview_turns t"
                + " JOIN ai.interview_sessions s ON s.id = t.interview_session_id"
                + " WHERE s.user_id = :user ORDER BY t.interview_session_id, t.turn_index")
        .param("user", userId)
        .query(JdbcInterviewSessionRepository::turn)
        .list();
  }

  @Override
  public int countUserTurns(UUID sessionId, InterviewStage stage) {
    return jdbc.getObject()
        .sql(
            "SELECT count(*) FROM ai.interview_turns WHERE interview_session_id = :id"
                + " AND role = 'USER' AND stage_at_turn = :stage")
        .param("id", sessionId)
        .param("stage", stage.name())
        .query(Integer.class)
        .single();
  }

  @Override
  public boolean incrementHintCount(UUID sessionId, int maxHints) {
    return jdbc.getObject()
            .sql(
                "UPDATE ai.interview_sessions SET hint_count = hint_count + 1, updated_at = now()"
                    + " WHERE id = :id AND hint_count < :max"
                    + " AND stage IN ('EXPLAIN', 'CHALLENGE', 'SCALE_UP')")
            .param("id", sessionId)
            .param("max", maxHints)
            .update()
        == 1;
  }

  private static String topicsJson(InterviewSession s) {
    return JSON.writeValueAsString(s.topics());
  }

  private static OffsetDateTime utc(java.time.Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static InterviewTurn turn(ResultSet rs, int row) throws SQLException {
    return new InterviewTurn(
        rs.getObject("id", UUID.class),
        rs.getObject("interview_session_id", UUID.class),
        rs.getInt("turn_index"),
        InterviewTurn.Role.valueOf(rs.getString("role")),
        InterviewStage.valueOf(rs.getString("stage_at_turn")),
        rs.getString("content"),
        rs.getObject("created_at", OffsetDateTime.class).toInstant());
  }

  private static InterviewSession session(ResultSet rs, int row) throws SQLException {
    String topics = rs.getString("topics");
    String level = rs.getString("interviewer_level");
    OffsetDateTime ended = rs.getObject("ended_at", OffsetDateTime.class);
    int maxTurns = rs.getInt("max_turns");
    Integer max = rs.wasNull() ? null : maxTurns;
    boolean hint = rs.getBoolean("hint_allowed");
    Boolean hintAllowed = rs.wasNull() ? null : hint;
    return new InterviewSession(
        rs.getObject("id", UUID.class),
        rs.getObject("user_id", UUID.class),
        InterviewEntryType.valueOf(rs.getString("entry_type")),
        rs.getObject("submission_id", UUID.class),
        rs.getString("question_ref"),
        rs.getString("question_text"),
        topics == null ? List.of() : JSON.readValue(topics, new TypeReference<List<String>>() {}),
        level == null ? null : InterviewerLevel.valueOf(level),
        max,
        hintAllowed,
        rs.getInt("hint_count"),
        InterviewSessionStage.valueOf(rs.getString("stage")),
        rs.getInt("turn_count"),
        rs.getObject("started_at", OffsetDateTime.class).toInstant(),
        ended == null ? null : ended.toInstant(),
        rs.getObject("updated_at", OffsetDateTime.class).toInstant());
  }
}

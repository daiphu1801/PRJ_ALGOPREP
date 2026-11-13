package com.algoprep.interviewbank.infrastructure.persistence;

import com.algoprep.interviewbank.domain.exception.AttemptNumberConflictException;
import com.algoprep.interviewbank.domain.model.AnswerAttempt;
import com.algoprep.interviewbank.domain.model.AttemptPage;
import com.algoprep.interviewbank.domain.model.Feedback;
import com.algoprep.interviewbank.domain.model.FeedbackCriterion;
import com.algoprep.interviewbank.domain.model.FeedbackStatus;
import com.algoprep.interviewbank.domain.model.NewAttempt;
import com.algoprep.interviewbank.domain.model.PracticeQuestion;
import com.algoprep.interviewbank.domain.model.QuestionLock;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import com.algoprep.interviewbank.domain.ports.out.PracticeRepository;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Practice data. user_answers is insert and select only; the unique (user, question, attempt_no) is
 * the last line of defence against two parallel submissions taking the same number.
 */
@Repository
class JdbcPracticeRepository implements PracticeRepository {
  private final ObjectProvider<JdbcClient> jdbc;
  private final JsonMapper json = JsonMapper.builder().build();

  JdbcPracticeRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<PracticeQuestion> findActiveQuestion(UUID questionId) {
    var rows =
        jdbc.getObject()
            .sql(
                "SELECT title, content_markdown FROM interview_bank.interview_questions"
                    + " WHERE id = :id AND status = 'ACTIVE'")
            .param("id", questionId)
            .query((rs, n) -> new String[] {rs.getString(1), rs.getString(2)})
            .list();
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    List<RubricCriterion> rubric =
        jdbc.getObject()
            .sql(
                "SELECT criterion_code, weight_percent, description"
                    + " FROM interview_bank.answer_rubrics WHERE question_id = :id"
                    + " ORDER BY sort_order")
            .param("id", questionId)
            .query(
                (rs, n) ->
                    new RubricCriterion(
                        rs.getString("criterion_code"),
                        rs.getBigDecimal("weight_percent"),
                        rs.getString("description")))
            .list();
    return Optional.of(new PracticeQuestion(questionId, rows.get(0)[0], rows.get(0)[1], rubric));
  }

  @Override
  public Optional<QuestionLock> lockQuestion(UUID questionId) {
    return jdbc.getObject()
        .sql(
            "SELECT topic_id, status FROM interview_bank.interview_questions"
                + " WHERE id = :id FOR SHARE")
        .param("id", questionId)
        .query(
            (rs, n) ->
                new QuestionLock(
                    rs.getObject("topic_id", UUID.class), "ACTIVE".equals(rs.getString("status"))))
        .optional();
  }

  @Override
  public int nextAttemptNo(UUID userId, UUID questionId) {
    return jdbc.getObject()
        .sql(
            "SELECT coalesce(max(attempt_no), 0) + 1 FROM interview_bank.user_answers"
                + " WHERE user_id = :user AND question_id = :question")
        .param("user", userId)
        .param("question", questionId)
        .query(Integer.class)
        .single();
  }

  @Override
  public AnswerAttempt insertAttempt(NewAttempt attempt, int attemptNo) {
    Feedback feedback = attempt.feedback();
    try {
      OffsetDateTime createdAt =
          jdbc.getObject()
              .sql(
                  "INSERT INTO interview_bank.user_answers(id, user_id, question_id, answer_text,"
                      + " attempt_no, feedback_status, feedback_result_json, rubric_snapshot_json)"
                      + " VALUES (:id, :user, :question, :text, :no, :status,"
                      + " CAST(:feedback AS jsonb), CAST(:snapshot AS jsonb))"
                      + " RETURNING created_at")
              .param("id", attempt.id())
              .param("user", attempt.userId())
              .param("question", attempt.questionId())
              .param("text", attempt.answerText())
              .param("no", attemptNo)
              .param("status", attempt.status().name())
              .param(
                  "feedback", feedback == null ? null : write(feedbackMap(feedback)), Types.VARCHAR)
              .param("snapshot", write(snapshotList(attempt.rubricSnapshot())))
              .query(OffsetDateTime.class)
              .single();
      return new AnswerAttempt(
          attempt.id(),
          attemptNo,
          createdAt.toInstant(),
          attempt.answerText(),
          attempt.status(),
          feedback);
    } catch (DuplicateKeyException taken) {
      throw new AttemptNumberConflictException(taken);
    }
  }

  @Override
  public void touchHistory(UUID userId, UUID topicId, boolean firstAttemptOfQuestion) {
    jdbc.getObject()
        .sql(
            "INSERT INTO interview_bank.practice_history(id, user_id, topic_id,"
                + " questions_attempted, last_practiced_at, updated_at)"
                + " VALUES (:id, :user, :topic, :inc, now(), now())"
                + " ON CONFLICT (user_id, topic_id) DO UPDATE SET"
                + " questions_attempted = practice_history.questions_attempted + :inc,"
                + " last_practiced_at = now(), updated_at = now()")
        .param("id", UUID.randomUUID())
        .param("user", userId)
        .param("topic", topicId)
        .param("inc", firstAttemptOfQuestion ? 1 : 0)
        .update();
  }

  @Override
  public AttemptPage attempts(UUID userId, UUID questionId, Integer before, int limit) {
    String cursor = before == null ? "" : " AND attempt_no < :before";
    var query =
        jdbc.getObject()
            .sql(
                "SELECT id, attempt_no, created_at, answer_text, feedback_status,"
                    + " feedback_result_json::text AS feedback FROM interview_bank.user_answers"
                    + " WHERE user_id = :user AND question_id = :question"
                    + cursor
                    + " ORDER BY attempt_no DESC LIMIT :lim")
            .param("user", userId)
            .param("question", questionId)
            .param("lim", limit + 1);
    if (before != null) {
      query = query.param("before", before);
    }
    List<AnswerAttempt> rows = new ArrayList<>(query.query((rs, n) -> row(rs)).list());
    boolean more = rows.size() > limit;
    long total =
        jdbc.getObject()
            .sql(
                "SELECT count(*) FROM interview_bank.user_answers"
                    + " WHERE user_id = :user AND question_id = :question")
            .param("user", userId)
            .param("question", questionId)
            .query(Long.class)
            .single();
    return new AttemptPage(more ? rows.subList(0, limit) : rows, more, total);
  }

  private AnswerAttempt row(java.sql.ResultSet rs) throws java.sql.SQLException {
    String feedback = rs.getString("feedback");
    return new AnswerAttempt(
        rs.getObject("id", UUID.class),
        rs.getInt("attempt_no"),
        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
        rs.getString("answer_text"),
        FeedbackStatus.valueOf(rs.getString("feedback_status")),
        feedback == null ? null : read(feedback));
  }

  private static Map<String, Object> feedbackMap(Feedback f) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("achievedPoints", f.achievedPoints());
    out.put("missingPoints", f.missingPoints());
    out.put("improvementSuggestions", f.improvementSuggestions());
    List<Map<String, Object>> criteria = new ArrayList<>();
    for (FeedbackCriterion c : f.criteria()) {
      Map<String, Object> one = new LinkedHashMap<>();
      one.put("criterionCode", c.criterionCode());
      one.put("achievementPercent", c.achievementPercent());
      one.put("weightPercent", c.weightPercent());
      criteria.add(one);
    }
    out.put("criteria", criteria);
    return out;
  }

  private static List<Map<String, Object>> snapshotList(List<RubricCriterion> rubric) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (RubricCriterion c : rubric) {
      Map<String, Object> one = new LinkedHashMap<>();
      one.put("criterionCode", c.criterionCode());
      one.put("weightPercent", c.weightPercent());
      one.put("description", c.description());
      out.add(one);
    }
    return out;
  }

  private String write(Object value) {
    return json.writeValueAsString(value);
  }

  private Feedback read(String text) {
    JsonNode root = json.readTree(text);
    List<FeedbackCriterion> criteria = new ArrayList<>();
    for (JsonNode c : root.path("criteria")) {
      criteria.add(
          new FeedbackCriterion(
              c.path("criterionCode").asString(),
              c.path("achievementPercent").asInt(),
              c.path("weightPercent").decimalValue()));
    }
    return new Feedback(
        strings(root.path("achievedPoints")),
        strings(root.path("missingPoints")),
        strings(root.path("improvementSuggestions")),
        criteria);
  }

  private static List<String> strings(JsonNode array) {
    List<String> out = new ArrayList<>();
    for (JsonNode one : array) {
      out.add(one.asString());
    }
    return out;
  }
}

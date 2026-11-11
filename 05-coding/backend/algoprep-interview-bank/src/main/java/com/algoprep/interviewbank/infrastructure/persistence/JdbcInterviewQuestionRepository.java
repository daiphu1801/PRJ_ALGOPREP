package com.algoprep.interviewbank.infrastructure.persistence;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.interviewbank.domain.model.LearnerQuestion;
import com.algoprep.interviewbank.domain.model.LearnerQuestionCriteria;
import com.algoprep.interviewbank.domain.model.LearnerQuestionPage;
import com.algoprep.interviewbank.domain.model.LearnerQuestionRow;
import com.algoprep.interviewbank.domain.model.RecallRating;
import com.algoprep.interviewbank.domain.model.RecallState;
import com.algoprep.interviewbank.domain.ports.out.LearnerQuestionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** SQL fragments come from enums and constants only; every value is a named parameter. */
@Repository
class JdbcInterviewQuestionRepository implements LearnerQuestionRepository {
  private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};

  private static final String FROM =
      " FROM interview_bank.interview_questions q"
          + " JOIN interview_bank.question_topics t ON t.id = q.topic_id"
          + " JOIN interview_bank.question_levels l ON l.id = q.level_id"
          + " LEFT JOIN interview_bank.recall_ratings rr"
          + " ON rr.question_id = q.id AND rr.user_id = :user"
          + " LEFT JOIN interview_bank.bookmarks b"
          + " ON b.question_id = q.id AND b.user_id = :user";

  private static final String SELECT =
      "SELECT q.id, t.code AS topic_code, t.display_name AS topic_name,"
          + " t.uses_star_framework, l.code AS level_code, l.display_name AS level_name,"
          + " q.title, q.content_markdown, q.suggested_approach, q.sample_answer_framework,"
          + " q.core_keywords::text AS keywords, q.follow_up_questions::text AS follow_ups,"
          + " rr.rating, rr.interval_days, rr.next_review_at, rr.rated_at,"
          + " (b.id IS NOT NULL) AS bookmarked,"
          + " EXISTS (SELECT 1 FROM interview_bank.answer_rubrics r WHERE r.question_id = q.id)"
          + " AS practice_available";

  /** Due cards first (oldest due first), then never-rated by display number, then the rest. */
  private static final String DRILL_ORDER =
      " ORDER BY CASE WHEN rr.next_review_at <= now() THEN 0 WHEN rr.id IS NULL THEN 1 ELSE 2 END,"
          + " rr.next_review_at ASC NULLS LAST, q.display_no ASC";

  private final ObjectProvider<JdbcClient> jdbc;
  private final IdGeneratorPort ids;
  private final JsonMapper json = JsonMapper.builder().build();

  JdbcInterviewQuestionRepository(ObjectProvider<JdbcClient> jdbc, IdGeneratorPort ids) {
    this.jdbc = jdbc;
    this.ids = ids;
  }

  private static String filters(LearnerQuestionCriteria criteria, Map<String, Object> params) {
    var where = new StringBuilder(" WHERE q.status = 'ACTIVE'");
    if (criteria.topicId() != null) {
      where.append(" AND q.topic_id = :topic");
      params.put("topic", criteria.topicId());
    }
    if (criteria.levelId() != null) {
      where.append(" AND q.level_id = :level");
      params.put("level", criteria.levelId());
    }
    if (criteria.tsQuery() != null) {
      where
          .append(" AND (")
          .append(QuestionSearchSql.FTS)
          .append(" OR lower(interview_bank.f_unaccent(q.core_keywords::text))")
          .append(" LIKE '%' || lower(interview_bank.f_unaccent(CAST(:like AS text))) || '%'")
          .append(" ESCAPE '\\')");
      params.put("tsq", criteria.tsQuery());
      params.put("like", criteria.likeText());
    }
    where.append(
        switch (criteria.status()) {
          case ALL -> "";
          case UNSEEN -> " AND rr.id IS NULL";
          case REVIEWING -> " AND rr.rating IN ('VAGUE', 'FORGOTTEN')";
          case KNOWN -> " AND rr.rating = 'KNOWN'";
        });
    if (criteria.bookmarkedOnly()) {
      where.append(" AND b.id IS NOT NULL");
    }
    return where.toString();
  }

  @Override
  public LearnerQuestionPage page(LearnerQuestionCriteria criteria) {
    var params = new HashMap<String, Object>();
    params.put("user", criteria.userId());
    String where = filters(criteria, params);
    long shown =
        jdbc.getObject()
            .sql("SELECT count(*)" + FROM + where)
            .params(params)
            .query(Long.class)
            .single();
    long total =
        jdbc.getObject()
            .sql("SELECT count(*) FROM interview_bank.interview_questions WHERE status = 'ACTIVE'")
            .query(Long.class)
            .single();
    long pages = Math.max(1, (shown + criteria.pageSize() - 1) / criteria.pageSize());
    int page = criteria.drill() ? 1 : (int) Math.min(criteria.page(), pages);
    params.put("size", criteria.pageSize());
    params.put("offset", (long) (page - 1) * criteria.pageSize());
    var items =
        jdbc.getObject()
            .sql(
                SELECT
                    + FROM
                    + where
                    + (criteria.drill() ? DRILL_ORDER : " ORDER BY q.display_no ASC")
                    + " LIMIT :size OFFSET :offset")
            .params(params)
            .query((rs, row) -> listRow(rs, criteria.drill()))
            .list();
    if (criteria.drill()) {
      shown = items.size();
    }
    return new LearnerQuestionPage(items, page, criteria.pageSize(), shown, total);
  }

  private LearnerQuestionRow listRow(ResultSet rs, boolean drill) throws SQLException {
    String rating = rs.getString("rating");
    return new LearnerQuestionRow(
        rs.getObject("id", UUID.class),
        rs.getString("topic_code"),
        rs.getString("topic_name"),
        rs.getString("level_code"),
        rs.getString("level_name"),
        rs.getString("title"),
        rating == null ? null : RecallRating.valueOf(rating),
        rs.getBoolean("bookmarked"),
        drill ? rs.getString("suggested_approach") : null,
        drill ? strings(rs.getString("keywords")) : List.of());
  }

  @Override
  public Optional<LearnerQuestion> find(UUID userId, UUID questionId) {
    return jdbc.getObject()
        .sql(SELECT + FROM + " WHERE q.id = :id AND q.status = 'ACTIVE'")
        .param("user", userId)
        .param("id", questionId)
        .query(
            (rs, row) -> {
              String rating = rs.getString("rating");
              RecallState recall =
                  rating == null
                      ? null
                      : new RecallState(
                          RecallRating.valueOf(rating),
                          rs.getInt("interval_days"),
                          rs.getObject("next_review_at", OffsetDateTime.class).toInstant(),
                          rs.getObject("rated_at", OffsetDateTime.class).toInstant());
              return new LearnerQuestion(
                  rs.getObject("id", UUID.class),
                  rs.getString("topic_code"),
                  rs.getString("topic_name"),
                  rs.getBoolean("uses_star_framework"),
                  rs.getString("level_code"),
                  rs.getString("level_name"),
                  rs.getString("title"),
                  rs.getString("content_markdown"),
                  rs.getString("suggested_approach"),
                  rs.getString("sample_answer_framework"),
                  strings(rs.getString("keywords")),
                  strings(rs.getString("follow_ups")),
                  rs.getBoolean("bookmarked"),
                  rs.getBoolean("practice_available"),
                  recall);
            })
        .optional();
  }

  /** The ACTIVE check and the upsert are one statement, so a retired question cannot be rated. */
  @Override
  public Optional<RecallState> rate(UUID userId, UUID questionId, RecallRating rating) {
    return jdbc.getObject()
        .sql(
            "INSERT INTO interview_bank.recall_ratings (id, user_id, question_id, rating,"
                + " interval_days, next_review_at, rated_at)"
                + " SELECT :id, :user, q.id, :rating, :days,"
                + " c.ts + make_interval(hours => :hours), c.ts"
                + " FROM interview_bank.interview_questions q"
                + " CROSS JOIN (SELECT clock_timestamp() AS ts) c"
                + " WHERE q.id = :question AND q.status = 'ACTIVE'"
                + " ON CONFLICT (user_id, question_id) DO UPDATE SET rating = EXCLUDED.rating,"
                + " interval_days = EXCLUDED.interval_days,"
                + " next_review_at = EXCLUDED.next_review_at, rated_at = EXCLUDED.rated_at"
                + " RETURNING rating, interval_days, next_review_at, rated_at")
        .param("id", ids.newId())
        .param("user", userId)
        .param("question", questionId)
        .param("rating", rating.name())
        .param("days", rating.intervalDays())
        .param("hours", rating.intervalDays() * 24)
        .query(
            (rs, row) ->
                new RecallState(
                    RecallRating.valueOf(rs.getString("rating")),
                    rs.getInt("interval_days"),
                    rs.getObject("next_review_at", OffsetDateTime.class).toInstant(),
                    rs.getObject("rated_at", OffsetDateTime.class).toInstant()))
        .optional();
  }

  @Override
  public boolean isActive(UUID questionId) {
    return !jdbc.getObject()
        .sql(
            "SELECT id FROM interview_bank.interview_questions"
                + " WHERE id = :id AND status = 'ACTIVE'")
        .param("id", questionId)
        .query(UUID.class)
        .list()
        .isEmpty();
  }

  @Override
  public void setBookmark(UUID userId, UUID questionId, boolean bookmarked) {
    if (bookmarked) {
      jdbc.getObject()
          .sql(
              "INSERT INTO interview_bank.bookmarks (id, user_id, question_id)"
                  + " VALUES (:id, :user, :question) ON CONFLICT (user_id, question_id) DO NOTHING")
          .param("id", ids.newId())
          .param("user", userId)
          .param("question", questionId)
          .update();
    } else {
      jdbc.getObject()
          .sql(
              "DELETE FROM interview_bank.bookmarks"
                  + " WHERE user_id = :user AND question_id = :question")
          .param("user", userId)
          .param("question", questionId)
          .update();
    }
  }

  private List<String> strings(String text) {
    return text == null ? List.of() : json.readValue(text, STRINGS);
  }
}

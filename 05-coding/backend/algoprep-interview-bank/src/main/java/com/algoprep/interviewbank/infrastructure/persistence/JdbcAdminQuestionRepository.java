package com.algoprep.interviewbank.infrastructure.persistence;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.interviewbank.domain.model.AdminQuestion;
import com.algoprep.interviewbank.domain.model.AdminQuestionPage;
import com.algoprep.interviewbank.domain.model.AdminQuestionRow;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.QuestionDraft;
import com.algoprep.interviewbank.domain.model.QuestionListCriteria;
import com.algoprep.interviewbank.domain.model.QuestionRow;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import com.algoprep.interviewbank.domain.ports.out.AdminQuestionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** SQL fragments come from enums and constants only, never from client input. */
@Repository
class JdbcAdminQuestionRepository implements AdminQuestionRepository {
  private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};

  /**
   * Per question: every attempt counts as usage; the score of a COMPLETED attempt is the sum of
   * achievement times weight over its criteria divided by 100 (BR-IB-10). PostgreSQL rounds a
   * numeric half away from zero, which is half-up for these non-negative scores.
   */
  private static final String USAGE =
      "SELECT ua.question_id, count(*) AS usage_total, round(avg(s.score), 1) AS average_score"
          + " FROM interview_bank.user_answers ua"
          + " LEFT JOIN LATERAL (SELECT sum((c->>'achievementPercent')::numeric"
          + " * (c->>'weightPercent')::numeric) / 100 AS score"
          + " FROM jsonb_array_elements(ua.feedback_result_json->'criteria') c) s"
          + " ON ua.feedback_status = 'COMPLETED'";

  private static final String DETAIL =
      "WITH usage AS ("
          + USAGE
          + " WHERE ua.question_id = :id GROUP BY ua.question_id)"
          + " SELECT q.id, q.display_no, q.topic_id, q.level_id, q.title, q.content_markdown,"
          + " q.follow_up_questions::text AS follow_ups, q.created_at, q.updated_at,"
          + " coalesce(u.usage_total, 0) AS usage_total, u.average_score"
          + " FROM interview_bank.interview_questions q"
          + " LEFT JOIN usage u ON u.question_id = q.id"
          + " WHERE q.id = :id AND q.status = 'ACTIVE'";

  private final ObjectProvider<JdbcClient> jdbc;
  private final IdGeneratorPort ids;
  private final JsonMapper json = JsonMapper.builder().build();

  JdbcAdminQuestionRepository(ObjectProvider<JdbcClient> jdbc, IdGeneratorPort ids) {
    this.jdbc = jdbc;
    this.ids = ids;
  }

  private static String orderBy(QuestionListCriteria criteria) {
    String column =
        switch (criteria.sort()) {
          case DISPLAY_NO -> "q.display_no";
          case LEVEL -> "l.sort_order";
          case TOPIC -> "t.sort_order";
          case TITLE -> "lower(q.title)";
          case FOLLOW_UP_COUNT -> "follow_up_count";
          case RUBRIC_COUNT -> "rubric_count";
          case USAGE_COUNT -> "usage_total";
          case AVERAGE_SCORE_PERCENT -> "average_score";
        };
    return column + (criteria.descending() ? " DESC" : " ASC") + " NULLS LAST, q.display_no ASC";
  }

  private static final String FROM =
      " FROM interview_bank.interview_questions q"
          + " JOIN interview_bank.question_topics t ON t.id = q.topic_id"
          + " JOIN interview_bank.question_levels l ON l.id = q.level_id";

  private static final String LIST_SELECT =
      "WITH usage AS ("
          + USAGE
          + " GROUP BY ua.question_id)"
          + " SELECT q.id, q.display_no, l.code AS level_code, l.display_name AS level_name,"
          + " t.code AS topic_code, t.display_name AS topic_name, q.title,"
          + " jsonb_array_length(q.follow_up_questions) AS follow_up_count,"
          + " (SELECT count(*) FROM interview_bank.answer_rubrics r"
          + " WHERE r.question_id = q.id) AS rubric_count,"
          + " coalesce(u.usage_total, 0) AS usage_total, u.average_score";

  /** Builds the WHERE clause; every value goes through a named parameter. */
  private static String filters(QuestionListCriteria criteria, Map<String, Object> params) {
    var where = new StringBuilder(" WHERE q.status = 'ACTIVE'");
    if (criteria.topicId() != null) {
      where.append(" AND q.topic_id = :topic");
      params.put("topic", criteria.topicId());
    }
    if (criteria.levelId() != null) {
      where.append(" AND q.level_id = :level");
      params.put("level", criteria.levelId());
    }
    if (criteria.searchQuery() != null) {
      where.append(" AND ").append(QuestionSearchSql.FTS);
      params.put("tsq", criteria.searchQuery());
    }
    return where.toString();
  }

  @Override
  public AdminQuestionPage page(QuestionListCriteria criteria) {
    var params = new HashMap<String, Object>();
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
    int page = (int) Math.min(criteria.page(), pages);
    params.put("size", criteria.pageSize());
    params.put("offset", (long) (page - 1) * criteria.pageSize());
    var items =
        jdbc.getObject()
            .sql(
                LIST_SELECT
                    + FROM
                    + " LEFT JOIN usage u ON u.question_id = q.id"
                    + where
                    + " ORDER BY "
                    + orderBy(criteria)
                    + " LIMIT :size OFFSET :offset")
            .params(params)
            .query((rs, row) -> listRow(rs))
            .list();
    return new AdminQuestionPage(items, page, criteria.pageSize(), shown, total);
  }

  private static AdminQuestionRow listRow(ResultSet rs) throws SQLException {
    return new AdminQuestionRow(
        rs.getObject("id", UUID.class),
        rs.getInt("display_no"),
        rs.getString("level_code"),
        rs.getString("level_name"),
        rs.getString("topic_code"),
        rs.getString("topic_name"),
        rs.getString("title"),
        rs.getInt("follow_up_count"),
        rs.getInt("rubric_count"),
        rs.getLong("usage_total"),
        rs.getBigDecimal("average_score"));
  }

  @Override
  public Optional<AdminQuestion> findActive(UUID id) {
    var rubric =
        jdbc.getObject()
            .sql(
                "SELECT criterion_code, weight_percent, description"
                    + " FROM interview_bank.answer_rubrics WHERE question_id = :id"
                    + " ORDER BY sort_order")
            .param("id", id)
            .query(
                (rs, row) ->
                    new RubricCriterion(
                        rs.getString("criterion_code"),
                        rs.getBigDecimal("weight_percent"),
                        rs.getString("description")))
            .list();
    return jdbc.getObject()
        .sql(DETAIL)
        .param("id", id)
        .query(
            (rs, row) ->
                new AdminQuestion(
                    rs.getObject("id", UUID.class),
                    rs.getInt("display_no"),
                    rs.getObject("topic_id", UUID.class),
                    rs.getObject("level_id", UUID.class),
                    rs.getString("title"),
                    rs.getString("content_markdown"),
                    strings(rs.getString("follow_ups")),
                    rubric,
                    rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                    rs.getObject("updated_at", OffsetDateTime.class).toInstant(),
                    rs.getLong("usage_total"),
                    rs.getBigDecimal("average_score")))
        .optional();
  }

  private List<String> strings(String text) {
    return json.readValue(text, STRINGS);
  }

  @Override
  public boolean lockReference(CatalogKind kind, UUID id) {
    String table =
        kind == CatalogKind.LEVEL
            ? "interview_bank.question_levels"
            : "interview_bank.question_topics";
    return !jdbc.getObject()
        .sql("SELECT id FROM " + table + " WHERE id = :id FOR KEY SHARE")
        .param("id", id)
        .query(UUID.class)
        .list()
        .isEmpty();
  }

  @Override
  public Optional<QuestionRow> lockActive(UUID id) {
    return jdbc.getObject()
        .sql(
            "SELECT id, topic_id, status, title FROM interview_bank.interview_questions"
                + " WHERE id = :id AND status = 'ACTIVE' FOR UPDATE")
        .param("id", id)
        .query((rs, row) -> questionRow(rs))
        .optional();
  }

  @Override
  public List<QuestionRow> lockAll(Collection<UUID> idList) {
    return jdbc.getObject()
        .sql(
            "SELECT id, topic_id, status, title FROM interview_bank.interview_questions"
                + " WHERE id IN (:ids) ORDER BY id FOR UPDATE")
        .param("ids", idList)
        .query((rs, row) -> questionRow(rs))
        .list();
  }

  private static QuestionRow questionRow(ResultSet rs) throws SQLException {
    return new QuestionRow(
        rs.getObject("id", UUID.class),
        rs.getObject("topic_id", UUID.class),
        rs.getString("status"),
        rs.getString("title"));
  }

  @Override
  public AdminQuestion insert(QuestionDraft draft, UUID actor) {
    UUID id = ids.newId();
    jdbc.getObject()
        .sql(
            "INSERT INTO interview_bank.interview_questions (id, topic_id, level_id, title,"
                + " content_markdown, suggested_approach, sample_answer_framework, core_keywords,"
                + " follow_up_questions, created_by) VALUES (:id, :topic, :level, :title, :content,"
                + " :approach, :sample, CAST(CAST(:keywords AS text) AS jsonb),"
                + " CAST(:followUps AS jsonb), :actor)")
        .param("id", id)
        .param("topic", draft.topicId())
        .param("level", draft.levelId())
        .param("title", draft.title())
        .param("content", draft.contentMarkdown())
        .param("approach", draft.suggestedApproach())
        .param("sample", draft.sampleAnswerFramework())
        .param(
            "keywords",
            draft.coreKeywords() == null ? null : json.writeValueAsString(draft.coreKeywords()))
        .param("followUps", json.writeValueAsString(draft.followUps()))
        .param("actor", actor)
        .update();
    insertRubric(id, draft.rubric());
    return findActive(id).orElseThrow();
  }

  @Override
  public void update(UUID id, QuestionDraft draft) {
    jdbc.getObject()
        .sql(
            "UPDATE interview_bank.interview_questions SET topic_id = :topic, level_id = :level,"
                + " title = :title, content_markdown = :content,"
                + " follow_up_questions = CAST(:followUps AS jsonb), updated_at = now()"
                + " WHERE id = :id")
        .param("id", id)
        .param("topic", draft.topicId())
        .param("level", draft.levelId())
        .param("title", draft.title())
        .param("content", draft.contentMarkdown())
        .param("followUps", json.writeValueAsString(draft.followUps()))
        .update();
  }

  @Override
  public void replaceRubric(UUID id, List<RubricCriterion> rubric) {
    jdbc.getObject()
        .sql("DELETE FROM interview_bank.answer_rubrics WHERE question_id = :id")
        .param("id", id)
        .update();
    insertRubric(id, rubric);
  }

  private void insertRubric(UUID questionId, List<RubricCriterion> rubric) {
    for (int index = 0; index < rubric.size(); index++) {
      var item = rubric.get(index);
      jdbc.getObject()
          .sql(
              "INSERT INTO interview_bank.answer_rubrics (id, question_id, criterion_code,"
                  + " description, weight_percent, sort_order)"
                  + " VALUES (:id, :question, :code, :description, :weight, :order)")
          .param("id", ids.newId())
          .param("question", questionId)
          .param("code", item.criterionCode())
          .param("description", item.description())
          .param("weight", item.weightPercent())
          .param("order", index + 1)
          .update();
    }
  }

  @Override
  public void retire(UUID id) {
    jdbc.getObject()
        .sql(
            "UPDATE interview_bank.interview_questions SET status = 'RETIRED', updated_at = now()"
                + " WHERE id = :id")
        .param("id", id)
        .update();
  }

  @Override
  public void recalculateProgress(UUID questionId, Collection<UUID> topicIds) {
    var users =
        jdbc.getObject()
            .sql(
                "SELECT DISTINCT user_id FROM interview_bank.user_answers"
                    + " WHERE question_id = :id")
            .param("id", questionId)
            .query(UUID.class)
            .list();
    for (UUID user : users) {
      for (UUID topic : new HashSet<>(topicIds)) {
        recalculate(user, topic);
      }
    }
  }

  /** Counts distinct ACTIVE questions of the topic the person answered; never incremental. */
  private void recalculate(UUID user, UUID topic) {
    Map<String, Object> aggregate =
        jdbc.getObject()
            .sql(
                "SELECT count(DISTINCT ua.question_id) AS attempted, max(ua.created_at) AS last"
                    + " FROM interview_bank.user_answers ua"
                    + " JOIN interview_bank.interview_questions q ON q.id = ua.question_id"
                    + " WHERE ua.user_id = :user AND q.topic_id = :topic AND q.status = 'ACTIVE'")
            .param("user", user)
            .param("topic", topic)
            .query(
                (rs, row) -> {
                  var map = new HashMap<String, Object>();
                  map.put("attempted", rs.getInt("attempted"));
                  map.put("last", rs.getObject("last", OffsetDateTime.class));
                  return map;
                })
            .single();
    int attempted = (Integer) aggregate.get("attempted");
    if (attempted > 0) {
      jdbc.getObject()
          .sql(
              "INSERT INTO interview_bank.practice_history (id, user_id, topic_id,"
                  + " questions_attempted, last_practiced_at, updated_at)"
                  + " VALUES (:id, :user, :topic, :attempted, :last, now())"
                  + " ON CONFLICT (user_id, topic_id) DO UPDATE SET"
                  + " questions_attempted = EXCLUDED.questions_attempted,"
                  + " last_practiced_at = EXCLUDED.last_practiced_at, updated_at = now()")
          .param("id", ids.newId())
          .param("user", user)
          .param("topic", topic)
          .param("attempted", attempted)
          .param("last", aggregate.get("last"))
          .update();
    } else {
      jdbc.getObject()
          .sql(
              "UPDATE interview_bank.practice_history SET questions_attempted = 0,"
                  + " last_practiced_at = NULL, updated_at = now()"
                  + " WHERE user_id = :user AND topic_id = :topic")
          .param("user", user)
          .param("topic", topic)
          .update();
    }
  }

  @Override
  public void lockImport() {
    jdbc.getObject()
        .sql("SELECT pg_advisory_xact_lock(hashtextextended('interview_bank:import', 0))")
        .query((rs, row) -> 1)
        .single();
  }

  @Override
  public Set<String> activeTitleKeys() {
    return new HashSet<>(
        jdbc.getObject()
            .sql(
                "SELECT lower(btrim(title)) FROM interview_bank.interview_questions"
                    + " WHERE status = 'ACTIVE'")
            .query(String.class)
            .list());
  }

  @Override
  public String titleKey(String title) {
    return jdbc.getObject()
        .sql("SELECT lower(btrim(CAST(:title AS text)))")
        .param("title", title)
        .query(String.class)
        .single();
  }
}

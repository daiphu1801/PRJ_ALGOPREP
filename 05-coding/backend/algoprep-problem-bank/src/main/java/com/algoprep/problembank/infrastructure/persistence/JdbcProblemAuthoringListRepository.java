package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.AttentionCount;
import com.algoprep.problembank.domain.model.AuthoringListFilter;
import com.algoprep.problembank.domain.model.AuthoringListPage;
import com.algoprep.problembank.domain.model.TopicDistribution;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringListRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Grouped queries only, no N+1: topic names, counts and statistics ride on the page query. The sort
 * column comes from a closed switch, never from the request text.
 */
@Repository
class JdbcProblemAuthoringListRepository implements ProblemAuthoringListRepository {
  // Leading space on purpose: a text block drops the blank before its closing delimiter.
  private static final String SCOPE =
      " NOT p.deleted AND (CAST(:author AS uuid) IS NULL OR p.author_id = CAST(:author AS uuid))";
  private static final String FILTERS =
      SCOPE
          + " AND (CAST(:level AS uuid) IS NULL OR p.level_id = CAST(:level AS uuid))"
          + " AND (CAST(:status AS text) IS NULL OR p.status = CAST(:status AS text))"
          + " AND (CAST(:q AS text) IS NULL OR p.code ILIKE CAST(:q AS text) ESCAPE '\\'"
          + " OR p.title ILIKE CAST(:q AS text) ESCAPE '\\')";

  /** The page row: names, counts and statistics ride on one grouped query, no N+1. */
  private static final String ITEMS =
      """
      SELECT p.id, p.code, p.title, p.level_id, l.code AS level_code,
             l.display_name AS level_name, p.status, p.updated_at,
             COALESCE(s.submission_count, 0) AS submission_count, s.ac_rate,
             (SELECT count(*) FROM problem.testcases t WHERE t.problem_id = p.id)
               AS testcase_count,
             (SELECT count(*) FROM problem.class_assignments ca
               WHERE ca.problem_id = p.id AND ca.removed_at IS NULL)
               AS assigned_class_count,
             ARRAY(SELECT tp.name FROM problem.problem_topics pt
                    JOIN problem.topics tp ON tp.id = pt.topic_id
                    WHERE pt.problem_id = p.id ORDER BY tp.sort_order, tp.name)
               AS topic_names
      FROM problem.problems p
      JOIN problem.problem_levels l ON l.id = p.level_id
      LEFT JOIN problem.problem_stats s ON s.problem_id = p.id
      WHERE""";

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcProblemAuthoringListRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public AuthoringListPage list(AuthoringListFilter f) {
    JdbcClient client = jdbc.getObject();
    var totals =
        client
            .sql(
                "SELECT count(*) AS total, count(*) FILTER (WHERE p.status = 'PUBLISHED') AS"
                    + " published FROM problem.problems p WHERE "
                    + SCOPE)
            .param("author", f.authorScope())
            .query((rs, row) -> new long[] {rs.getLong("total"), rs.getLong("published")})
            .single();
    long matching =
        client
            .sql("SELECT count(*) FROM problem.problems p WHERE " + FILTERS)
            .param("author", f.authorScope())
            .param("level", f.levelId())
            .param("status", f.status())
            .param("q", pattern(f.q()))
            .query(Long.class)
            .single();
    int totalPages = (int) ((matching + f.pageSize() - 1) / f.pageSize());
    int page = Math.min(f.page(), Math.max(totalPages, 1));
    String order = column(f.sort()) + (f.ascending() ? " ASC" : " DESC") + " NULLS LAST, p.id";
    List<AuthoringListPage.Item> items =
        client
            .sql(ITEMS + FILTERS + " ORDER BY " + order + " LIMIT :size OFFSET :offset")
            .param("author", f.authorScope())
            .param("level", f.levelId())
            .param("status", f.status())
            .param("q", pattern(f.q()))
            .param("size", f.pageSize())
            .param("offset", (long) (page - 1) * f.pageSize())
            .query(JdbcProblemAuthoringListRepository::item)
            .list();
    return new AuthoringListPage(
        items, page, f.pageSize(), matching, totalPages, totals[0], totals[1]);
  }

  @Override
  public List<AuthoringListPage.Item> export(
      UUID authorScope, List<UUID> ids, String q, UUID levelId, String status) {
    String byIds = ids == null ? "" : " AND p.id = ANY(:ids)";
    var statement =
        jdbc.getObject()
            .sql(ITEMS + FILTERS + byIds + " ORDER BY p.updated_at DESC, p.id")
            .param("author", authorScope)
            .param("level", levelId)
            .param("status", status)
            .param("q", pattern(q));
    if (ids != null) {
      statement = statement.param("ids", ids.toArray(new UUID[0]));
    }
    return statement.query(JdbcProblemAuthoringListRepository::item).list();
  }

  @Override
  public TopicDistribution topicDistribution(UUID authorScope) {
    JdbcClient client = jdbc.getObject();
    int withTopic =
        client
            .sql(
                "SELECT count(DISTINCT pt.problem_id) FROM problem.problem_topics pt JOIN"
                    + " problem.problems p ON p.id = pt.problem_id WHERE "
                    + SCOPE)
            .param("author", authorScope)
            .query(Integer.class)
            .single();
    List<TopicDistribution.Entry> entries =
        client
            .sql(
                """
                SELECT t.id, t.name, count(*) AS n
                FROM problem.problem_topics pt
                JOIN problem.problems p ON p.id = pt.problem_id
                JOIN problem.topics t ON t.id = pt.topic_id
                WHERE """
                    + SCOPE
                    + " GROUP BY t.id, t.name, t.sort_order ORDER BY n DESC, t.sort_order, t.name")
            .param("author", authorScope)
            .query(
                (rs, row) ->
                    new TopicDistribution.Entry(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getInt("n"),
                        percent(rs.getInt("n"), withTopic)))
            .list();
    return new TopicDistribution(withTopic, entries.size(), entries);
  }

  @Override
  public List<AttentionCount> attention(UUID authorScope, int lowAcPercent, int staleDraftDays) {
    return jdbc.getObject()
        .sql(
            """
            SELECT count(*) FILTER (WHERE NOT EXISTS
                     (SELECT 1 FROM problem.testcases t WHERE t.problem_id = p.id)) AS no_testcase,
                   count(*) FILTER (WHERE s.ac_rate IS NOT NULL AND s.ac_rate < :low) AS low_ac,
                   count(*) FILTER (WHERE p.status = 'UNPUBLISHED'
                                      AND p.published_at IS NOT NULL) AS hidden,
                   count(*) FILTER (WHERE p.status = 'UNPUBLISHED' AND p.published_at IS NULL
                                      AND p.updated_at < clock_timestamp()
                                          - make_interval(days => :days)) AS stale
            FROM problem.problems p
            LEFT JOIN problem.problem_stats s ON s.problem_id = p.id
            WHERE """
                + SCOPE)
        .param("author", authorScope)
        .param("low", lowAcPercent)
        .param("days", staleDraftDays)
        .query(
            (rs, row) ->
                List.of(
                    new AttentionCount("NO_TESTCASE", rs.getInt("no_testcase")),
                    new AttentionCount("LOW_AC", rs.getInt("low_ac")),
                    new AttentionCount("HIDDEN_FROM_LEARNERS", rs.getInt("hidden")),
                    new AttentionCount("STALE_DRAFT", rs.getInt("stale"))))
        .single();
  }

  private static AuthoringListPage.Item item(java.sql.ResultSet rs, int row)
      throws java.sql.SQLException {
    return new AuthoringListPage.Item(
        rs.getObject("id", UUID.class),
        rs.getString("code"),
        rs.getString("title"),
        names(rs.getArray("topic_names")),
        rs.getObject("level_id", UUID.class),
        rs.getString("level_code"),
        rs.getString("level_name"),
        rs.getString("status"),
        rs.getInt("submission_count"),
        rs.getInt("assigned_class_count"),
        rs.getBigDecimal("ac_rate"),
        rs.getInt("testcase_count"),
        rs.getTimestamp("updated_at").toInstant());
  }

  private static String column(String sort) {
    return switch (sort) {
      case "code" -> "p.code";
      case "title" -> "p.title";
      case "level" -> "l.sort_order";
      case "status" -> "p.status";
      case "submissionCount" -> "COALESCE(s.submission_count, 0)";
      case "acRate" -> "s.ac_rate";
      default -> "p.updated_at";
    };
  }

  /** {@code %text%} with LIKE metacharacters escaped; null for no search. */
  private static String pattern(String q) {
    if (q == null) {
      return null;
    }
    return "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
  }

  private static BigDecimal percent(int count, int total) {
    if (total == 0) {
      return BigDecimal.ZERO.setScale(1);
    }
    return BigDecimal.valueOf(count)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
  }

  private static List<String> names(Array array) throws java.sql.SQLException {
    return new ArrayList<>(Arrays.asList((String[]) array.getArray()));
  }
}

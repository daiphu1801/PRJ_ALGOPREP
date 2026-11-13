package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.PublishedProblemModels.Example;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Filter;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Overview;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Page;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Paging;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Row;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Sample;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Samples;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Workspace;
import com.algoprep.problembank.domain.model.StoredGradingTestcase.Content;
import com.algoprep.problembank.domain.ports.out.PublishedProblemReadRepository;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * One scope for every query: {@code PUBLISHED AND NOT deleted}. The solve-state sets arrive as
 * {@code uuid[]} literals built from {@link UUID#toString()} only, so nothing user-typed reaches
 * them.
 */
@Repository
class JdbcPublishedProblemReadRepository implements PublishedProblemReadRepository {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String VISIBLE = " p.status = 'PUBLISHED' AND NOT p.deleted ";

  private static final String FROM =
      """
       FROM problem.problems p
       JOIN problem.problem_levels pl ON pl.id = p.level_id
       LEFT JOIN problem.problem_stats s ON s.problem_id = p.id
      """;

  private static final String FILTERS =
      """
       AND (CAST(:q AS text) IS NULL
            OR p.code ILIKE :pattern ESCAPE '\\' OR p.title ILIKE :pattern ESCAPE '\\')
       AND (CAST(:difficulty AS text) IS NULL
            OR NOT EXISTS (SELECT 1 FROM problem.problem_levels x WHERE x.code = :difficulty)
            OR pl.code = :difficulty)
       AND (CAST(:topic AS uuid) IS NULL OR EXISTS (
            SELECT 1 FROM problem.problem_topics pt WHERE pt.problem_id = p.id AND pt.topic_id = CAST(:topic AS uuid)))
       AND (CAST(:tag AS text) IS NULL OR EXISTS (
            SELECT 1 FROM problem.problem_tags ptg JOIN problem.tags tg ON tg.id = ptg.tag_id
            WHERE ptg.problem_id = p.id AND lower(tg.name) = lower(:tag)))
       AND (CAST(:status AS text) IS NULL
            OR (:status = 'SOLVED' AND p.id = ANY(CAST(:solved AS uuid[])))
            OR (:status = 'ATTEMPTED' AND p.id = ANY(CAST(:attempted AS uuid[])))
            OR (:status = 'UNSOLVED' AND NOT p.id = ANY(CAST(:solved AS uuid[]))
                AND NOT p.id = ANY(CAST(:attempted AS uuid[])))
            OR (:status = 'NOT_SOLVED' AND NOT p.id = ANY(CAST(:solved AS uuid[]))))
      """;

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcPublishedProblemReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Page list(Filter filter, SolveStates states, Paging paging) {
    long total =
        bind(
                jdbc.getObject().sql("SELECT count(*)" + FROM + " WHERE" + VISIBLE + FILTERS),
                filter,
                states)
            .query(Long.class)
            .single();
    int pages = Math.max(1, (int) Math.ceil(total / (double) paging.pageSize()));
    int page = Math.min(paging.page(), pages);
    String dir = paging.ascending() ? "ASC" : "DESC";
    String order =
        switch (paging.sort()) {
          case "level" -> "pl.sort_order " + dir;
          case "acRate" -> "s.ac_rate " + dir + " NULLS LAST";
          default -> "p.code " + dir;
        };
    var items =
        bind(
                jdbc.getObject()
                    .sql(
                        """
                        SELECT p.id, p.code, p.title, pl.code AS level_code, pl.display_name,
                               s.ac_rate, p.function_wrapper_supported,
                               ARRAY(SELECT t.name FROM problem.problem_topics pt
                                     JOIN problem.topics t ON t.id = pt.topic_id
                                     WHERE pt.problem_id = p.id ORDER BY t.sort_order, t.name) AS topic_names
                        """
                            + FROM
                            + " WHERE"
                            + VISIBLE
                            + FILTERS
                            + " ORDER BY "
                            + order
                            + ", p.code, p.id LIMIT :limit OFFSET :offset"),
                filter,
                states)
            .param("limit", paging.pageSize())
            .param("offset", (long) (page - 1) * paging.pageSize())
            .query(JdbcPublishedProblemReadRepository::row)
            .list();
    return new Page(items, total, page);
  }

  @Override
  public Optional<UUID> random(Filter filter, SolveStates states) {
    return bind(
            jdbc.getObject()
                .sql(
                    "SELECT p.id"
                        + FROM
                        + " WHERE"
                        + VISIBLE
                        + FILTERS
                        + " ORDER BY random() LIMIT 1"),
            filter,
            states)
        .query(UUID.class)
        .optional();
  }

  @Override
  public Optional<Workspace> workspace(UUID problemId, UUID userId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT p.id, p.code, p.title, pl.code AS level_code, pl.display_name,
                   COALESCE(p.statement_md, '') AS statement_md, p.constraints_md,
                   p.function_wrapper_supported, sp.stdin_format_md, sp.stdout_format_md, s.ac_rate,
                   EXISTS (SELECT 1 FROM problem.bookmarks b
                           WHERE b.problem_id = p.id AND b.user_id = :user) AS bookmarked,
                   ARRAY(SELECT t.name FROM problem.problem_topics pt
                         JOIN problem.topics t ON t.id = pt.topic_id
                         WHERE pt.problem_id = p.id ORDER BY t.sort_order, t.name) AS topic_names
            """
                + FROM
                + " LEFT JOIN problem.problem_specs sp ON sp.problem_id = p.id WHERE"
                + VISIBLE
                + " AND p.id = :id")
        .param("id", problemId)
        .param("user", userId)
        .query(
            (rs, row) ->
                new Workspace(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    rs.getString("title"),
                    rs.getString("level_code"),
                    rs.getString("display_name"),
                    names(rs.getArray("topic_names")),
                    rs.getString("statement_md"),
                    rs.getString("constraints_md"),
                    rs.getBoolean("function_wrapper_supported"),
                    rs.getString("stdin_format_md"),
                    rs.getString("stdout_format_md"),
                    rs.getBigDecimal("ac_rate"),
                    rs.getBoolean("bookmarked"),
                    examples(problemId)))
        .optional();
  }

  @Override
  public Optional<Overview> overview(UUID problemId, UUID userId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT p.id, p.code, p.title, pl.code AS level_code, pl.display_name,
                   COALESCE(p.statement_md, '') AS statement_md, p.constraints_md, s.ac_rate,
                   (SELECT count(*) FROM problem.testcases t
                    WHERE t.problem_id = p.id AND t.visibility = 'HIDDEN' AND t.approved) AS hidden_count,
                   EXISTS (SELECT 1 FROM problem.bookmarks b
                           WHERE b.problem_id = p.id AND b.user_id = :user) AS bookmarked,
                   ARRAY(SELECT t.name FROM problem.problem_topics pt
                         JOIN problem.topics t ON t.id = pt.topic_id
                         WHERE pt.problem_id = p.id ORDER BY t.sort_order, t.name) AS topic_names
            """
                + FROM
                + " WHERE"
                + VISIBLE
                + " AND p.id = :id")
        .param("id", problemId)
        .param("user", userId)
        .query(
            (rs, row) ->
                new Overview(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    rs.getString("title"),
                    rs.getString("level_code"),
                    rs.getString("display_name"),
                    rs.getString("statement_md"),
                    rs.getString("constraints_md"),
                    names(rs.getArray("topic_names")),
                    rs.getBigDecimal("ac_rate"),
                    rs.getInt("hidden_count"),
                    rs.getBoolean("bookmarked"),
                    examples(problemId)))
        .optional();
  }

  @Override
  public Optional<Samples> samples(UUID problemId) {
    boolean visible =
        jdbc.getObject()
            .sql(
                "SELECT EXISTS (SELECT 1 FROM problem.problems p WHERE"
                    + VISIBLE
                    + " AND p.id = :id)")
            .param("id", problemId)
            .query(Boolean.class)
            .single();
    if (!visible) {
      return Optional.empty();
    }
    List<Sample> items =
        jdbc.getObject()
            .sql(
                """
                SELECT id, position, input_storage, input_inline, input_object_key,
                       expected_output_storage, expected_output_inline, expected_output_object_key
                FROM problem.testcases
                WHERE problem_id = :id AND visibility = 'SAMPLE' AND approved = true
                ORDER BY position, id
                """)
            .param("id", problemId)
            .query(
                (rs, row) ->
                    new Sample(
                        rs.getObject("id", UUID.class),
                        rs.getInt("position"),
                        new Content(
                            rs.getString("input_storage"),
                            rs.getString("input_inline"),
                            rs.getString("input_object_key")),
                        new Content(
                            rs.getString("expected_output_storage"),
                            rs.getString("expected_output_inline"),
                            rs.getString("expected_output_object_key"))))
            .list();
    return Optional.of(new Samples(parameterNames(problemId), items));
  }

  private List<Example> examples(UUID problemId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT position, input, output, explanation_md FROM problem.worked_examples
            WHERE problem_id = :id ORDER BY position, id
            """)
        .param("id", problemId)
        .query(
            (rs, row) ->
                new Example(
                    rs.getInt("position"),
                    rs.getString("input"),
                    rs.getString("output"),
                    rs.getString("explanation_md")))
        .list();
  }

  private List<String> parameterNames(UUID problemId) {
    Optional<String> raw =
        jdbc.getObject()
            .sql(
                "SELECT CAST(parameters AS text) FROM problem.problem_specs WHERE problem_id = :id")
            .param("id", problemId)
            .query(String.class)
            .optional();
    var names = new ArrayList<String>();
    if (raw.isPresent()) {
      JsonNode array = JSON.readTree(raw.get());
      array.forEach(node -> names.add(node.path("name").asString()));
    }
    return names;
  }

  private static Row row(ResultSet rs, int index) throws SQLException {
    return new Row(
        rs.getObject("id", UUID.class),
        rs.getString("code"),
        rs.getString("title"),
        names(rs.getArray("topic_names")),
        rs.getString("level_code"),
        rs.getString("display_name"),
        rs.getBigDecimal("ac_rate"),
        rs.getBoolean("function_wrapper_supported"));
  }

  private static JdbcClient.StatementSpec bind(
      JdbcClient.StatementSpec spec, Filter filter, SolveStates states) {
    String q = filter.q();
    return spec.param("q", q)
        .param("pattern", q == null ? null : "%" + escape(q) + "%")
        .param("difficulty", filter.difficulty())
        .param("topic", filter.topic() == null ? null : filter.topic().toString())
        .param("tag", filter.tag())
        .param("status", filter.status())
        .param("solved", states == null ? "{}" : literal(states.solved()))
        .param("attempted", states == null ? "{}" : literal(states.attempted()));
  }

  private static String escape(String raw) {
    return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static String literal(Collection<UUID> ids) {
    return ids.stream().map(UUID::toString).collect(Collectors.joining(",", "{", "}"));
  }

  private static List<String> names(Array array) throws SQLException {
    var names = new ArrayList<String>();
    for (Object name : (Object[]) array.getArray()) {
      names.add((String) name);
    }
    return names;
  }
}

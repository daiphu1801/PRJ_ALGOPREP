package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.ClassAssignmentModels.ActiveRow;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.Assignable;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.AssignablePage;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ProblemState;
import com.algoprep.problembank.domain.ports.out.ClassAssignmentRepository;
import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcClassAssignmentRepository implements ClassAssignmentRepository {
  private static final String TOPICS =
      """
      ARRAY(SELECT t.name FROM problem.problem_topics pt JOIN problem.topics t ON t.id = pt.topic_id
            WHERE pt.problem_id = p.id ORDER BY t.sort_order, t.name) AS topic_names
      """;

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcClassAssignmentRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<ActiveRow> active(Set<UUID> classIds, String q) {
    return jdbc.getObject()
        .sql(
            """
            SELECT ca.problem_id, ca.class_id, ca.assigned_at, p.title, pl.code AS level_code,
                   pl.display_name, (p.status <> 'PUBLISHED' OR p.deleted) AS withdrawn,
            """
                + TOPICS
                + """
                  FROM problem.class_assignments ca
                  JOIN problem.problems p ON p.id = ca.problem_id
                  JOIN problem.problem_levels pl ON pl.id = p.level_id
                  WHERE ca.class_id = ANY(CAST(:classes AS uuid[])) AND ca.removed_at IS NULL
                    AND (CAST(:q AS text) IS NULL OR p.title ILIKE :pattern ESCAPE '\\')
                  """)
        .param("classes", literal(classIds))
        .param("q", q)
        .param("pattern", q == null ? null : "%" + escape(q) + "%")
        .query(
            (rs, row) ->
                new ActiveRow(
                    rs.getObject("problem_id", UUID.class),
                    rs.getObject("class_id", UUID.class),
                    rs.getTimestamp("assigned_at").toInstant(),
                    rs.getString("title"),
                    names(rs.getArray("topic_names")),
                    rs.getString("level_code"),
                    rs.getString("display_name"),
                    rs.getBoolean("withdrawn")))
        .list();
  }

  @Override
  public AssignablePage assignable(UUID classId, String q, int page, int pageSize) {
    String where =
        """
        FROM problem.problems p JOIN problem.problem_levels pl ON pl.id = p.level_id
        WHERE p.status = 'PUBLISHED' AND NOT p.deleted
          AND NOT EXISTS (SELECT 1 FROM problem.class_assignments ca
                          WHERE ca.problem_id = p.id AND ca.class_id = :class AND ca.removed_at IS NULL)
          AND (CAST(:q AS text) IS NULL OR p.code ILIKE :pattern ESCAPE '\\'
               OR p.title ILIKE :pattern ESCAPE '\\')
        """;
    String pattern = q == null ? null : "%" + escape(q) + "%";
    long total =
        jdbc.getObject()
            .sql("SELECT count(*) " + where)
            .param("class", classId)
            .param("q", q)
            .param("pattern", pattern)
            .query(Long.class)
            .single();
    int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
    int shown = Math.min(page, pages);
    var items =
        jdbc.getObject()
            .sql(
                "SELECT p.id, p.code, p.title, pl.code AS level_code, pl.display_name, "
                    + TOPICS
                    + where
                    + " ORDER BY p.code, p.id LIMIT :limit OFFSET :offset")
            .param("class", classId)
            .param("q", q)
            .param("pattern", pattern)
            .param("limit", pageSize)
            .param("offset", (long) (shown - 1) * pageSize)
            .query(
                (rs, row) ->
                    new Assignable(
                        rs.getObject("id", UUID.class),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("level_code"),
                        rs.getString("display_name"),
                        names(rs.getArray("topic_names"))))
            .list();
    return new AssignablePage(items, total, shown);
  }

  @Override
  public Optional<ProblemState> problemState(UUID problemId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT title, (status = 'PUBLISHED' AND NOT deleted) AS published
            FROM problem.problems WHERE id = :id
            """)
        .param("id", problemId)
        .query((rs, row) -> new ProblemState(rs.getString("title"), rs.getBoolean("published")))
        .optional();
  }

  @Override
  public boolean insertActive(UUID problemId, UUID classId, UUID actorId) {
    return jdbc.getObject()
            .sql(
                """
                INSERT INTO problem.class_assignments(id, problem_id, class_id, assigned_by)
                VALUES (gen_random_uuid(), :problem, :class, :actor)
                ON CONFLICT (problem_id, class_id) WHERE removed_at IS NULL DO NOTHING
                """)
            .param("problem", problemId)
            .param("class", classId)
            .param("actor", actorId)
            .update()
        > 0;
  }

  @Override
  public Optional<java.time.Instant> deactivate(UUID classId, UUID problemId) {
    return jdbc.getObject()
        .sql(
            """
            UPDATE problem.class_assignments SET removed_at = clock_timestamp()
            WHERE problem_id = :problem AND class_id = :class AND removed_at IS NULL
            RETURNING removed_at
            """)
        .param("problem", problemId)
        .param("class", classId)
        .query((rs, row) -> rs.getTimestamp("removed_at").toInstant())
        .optional();
  }

  private static String escape(String raw) {
    return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static String literal(Set<UUID> ids) {
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

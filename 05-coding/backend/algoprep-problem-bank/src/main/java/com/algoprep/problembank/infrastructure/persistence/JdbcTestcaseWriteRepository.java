package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.ApprovalCounts;
import com.algoprep.problembank.domain.model.ProblemTouch;
import com.algoprep.problembank.domain.model.StoredGradingTestcase.Content;
import com.algoprep.problembank.domain.model.StoredTestcase;
import com.algoprep.problembank.domain.ports.out.TestcaseWriteRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcTestcaseWriteRepository implements TestcaseWriteRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcTestcaseWriteRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ApprovalCounts counts(UUID problemId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT count(*) AS total,
                   count(*) FILTER (WHERE approved) AS approved,
                   count(*) FILTER (WHERE approved AND visibility = 'SAMPLE') AS samples
            FROM problem.testcases WHERE problem_id = :id
            """)
        .param("id", problemId)
        .query(
            (rs, row) ->
                new ApprovalCounts(rs.getInt("total"), rs.getInt("approved"), rs.getInt("samples")))
        .single();
  }

  @Override
  public int nextPosition(UUID problemId) {
    return jdbc.getObject()
        .sql("SELECT COALESCE(max(position), 0) + 1 FROM problem.testcases WHERE problem_id = :id")
        .param("id", problemId)
        .query(Integer.class)
        .single();
  }

  @Override
  public Optional<StoredTestcase> lock(UUID problemId, UUID testcaseId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT id, visibility, origin, approved, category, position,
                   input_storage, input_inline, input_object_key,
                   expected_output_storage, expected_output_inline, expected_output_object_key
            FROM problem.testcases WHERE id = :id AND problem_id = :problem FOR UPDATE
            """)
        .param("id", testcaseId)
        .param("problem", problemId)
        .query(
            (rs, row) ->
                new StoredTestcase(
                    rs.getObject("id", UUID.class),
                    rs.getString("visibility"),
                    rs.getString("origin"),
                    rs.getBoolean("approved"),
                    rs.getString("category"),
                    rs.getInt("position"),
                    new Content(
                        rs.getString("input_storage"),
                        rs.getString("input_inline"),
                        rs.getString("input_object_key")),
                    new Content(
                        rs.getString("expected_output_storage"),
                        rs.getString("expected_output_inline"),
                        rs.getString("expected_output_object_key"))))
        .optional();
  }

  @Override
  public void insert(UUID problemId, StoredTestcase t) {
    jdbc.getObject()
        .sql(
            """
            INSERT INTO problem.testcases(id, problem_id, visibility, origin, approved, category,
              position, input_storage, input_inline, input_object_key,
              expected_output_storage, expected_output_inline, expected_output_object_key)
            VALUES (:id, :problem, :visibility, :origin, :approved, :category, :position,
              :inStorage, :inInline, :inKey, :outStorage, :outInline, :outKey)
            """)
        .params(parameters(t))
        .param("problem", problemId)
        .update();
  }

  @Override
  public void update(StoredTestcase t) {
    jdbc.getObject()
        .sql(
            """
            UPDATE problem.testcases SET visibility = :visibility, approved = :approved,
              category = :category, position = :position,
              input_storage = :inStorage, input_inline = :inInline, input_object_key = :inKey,
              expected_output_storage = :outStorage, expected_output_inline = :outInline,
              expected_output_object_key = :outKey, updated_at = now()
            WHERE id = :id
            """)
        .params(parameters(t))
        .update();
  }

  @Override
  public void delete(UUID testcaseId) {
    jdbc.getObject()
        .sql("DELETE FROM problem.testcases WHERE id = :id")
        .param("id", testcaseId)
        .update();
  }

  @Override
  public List<UUID> lockOrder(UUID problemId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT id FROM problem.testcases WHERE problem_id = :id
            ORDER BY position, id FOR UPDATE
            """)
        .param("id", problemId)
        .query(UUID.class)
        .list();
  }

  @Override
  public void setPositions(List<UUID> orderedIds) {
    for (int i = 0; i < orderedIds.size(); i++) {
      jdbc.getObject()
          .sql(
              "UPDATE problem.testcases SET position = :position, updated_at = now() WHERE id = :id")
          .param("position", i + 1)
          .param("id", orderedIds.get(i))
          .update();
    }
  }

  @Override
  public ProblemTouch touch(UUID problemId, UUID actorId) {
    return jdbc.getObject()
        .sql(
            """
            UPDATE problem.problems SET updated_by = :actor,
              updated_at = GREATEST(clock_timestamp(), updated_at + interval '1 microsecond')
            WHERE id = :id AND NOT deleted
            RETURNING updated_at, current_testcase_set_version
            """)
        .param("actor", actorId)
        .param("id", problemId)
        .query(JdbcTestcaseWriteRepository::touchRow)
        .single();
  }

  @Override
  public ProblemTouch state(UUID problemId) {
    return jdbc.getObject()
        .sql("SELECT updated_at, current_testcase_set_version FROM problem.problems WHERE id = :id")
        .param("id", problemId)
        .query(JdbcTestcaseWriteRepository::touchRow)
        .single();
  }

  private static ProblemTouch touchRow(ResultSet rs, int row) throws SQLException {
    return new ProblemTouch(
        rs.getTimestamp("updated_at").toInstant(), rs.getInt("current_testcase_set_version"));
  }

  private static Map<String, Object> parameters(StoredTestcase t) {
    var map = new HashMap<String, Object>();
    map.put("id", t.id());
    map.put("visibility", t.visibility());
    map.put("origin", t.origin());
    map.put("approved", t.approved());
    map.put("category", t.category());
    map.put("position", t.position());
    map.put("inStorage", t.input().storage());
    map.put("inInline", t.input().inline());
    map.put("inKey", t.input().objectKey());
    map.put("outStorage", t.expectedOutput().storage());
    map.put("outInline", t.expectedOutput().inline());
    map.put("outKey", t.expectedOutput().objectKey());
    return map;
  }
}

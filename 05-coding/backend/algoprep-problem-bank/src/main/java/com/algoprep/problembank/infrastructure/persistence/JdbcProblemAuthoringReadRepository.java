package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.Tag;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemSpecData;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Read-only joins for DD F2 section 34; testcase bodies are selected only on request. */
@Repository
class JdbcProblemAuthoringReadRepository implements ProblemAuthoringReadRepository {
  private final ObjectProvider<JdbcClient> jdbc;
  private final JdbcProblemContentRelations relations;

  JdbcProblemAuthoringReadRepository(
      ObjectProvider<JdbcClient> jdbc, JdbcProblemContentRelations relations) {
    this.jdbc = jdbc;
    this.relations = relations;
  }

  @Override
  public Optional<AuthoringProblemRecord> find(UUID problemId, boolean withTestcaseContent) {
    return jdbc.getObject()
        .sql("SELECT * FROM problem.problems WHERE id = :id AND deleted = false")
        .param("id", problemId)
        .query(
            (rs, row) ->
                new AuthoringProblemRecord(
                    problemId,
                    rs.getString("code"),
                    rs.getString("title"),
                    rs.getString("statement_md"),
                    rs.getString("constraints_md"),
                    rs.getObject("level_id", UUID.class),
                    rs.getString("status"),
                    rs.getBoolean("function_wrapper_supported"),
                    new ProblemContentData.Limits(
                        rs.getInt("time_limit_ms"),
                        rs.getInt("memory_limit_mb"),
                        rs.getInt("max_output_size_kb"),
                        (Integer) rs.getObject("max_submissions_per_hour")),
                    rs.getInt("current_testcase_set_version"),
                    rs.getInt("ai_testcase_generations_used"),
                    rs.getString("ai_brief"),
                    rs.getBoolean("no_full_code"),
                    rs.getBoolean("socratic_only"),
                    rs.getObject("author_id", UUID.class),
                    rs.getObject("updated_by", UUID.class),
                    rs.getObject("duplicated_from_problem_id", UUID.class),
                    rs.getTimestamp("updated_at").toInstant(),
                    published(rs.getTimestamp("published_at")),
                    relations.topics(problemId),
                    tags(problemId),
                    relations.solution(problemId),
                    relations.examples(problemId),
                    testcases(problemId, withTestcaseContent),
                    spec(problemId)))
        .optional();
  }

  private static java.time.Instant published(java.sql.Timestamp value) {
    return value == null ? null : value.toInstant();
  }

  @Override
  public int aiGenerationLimit() {
    return jdbc.getObject()
        .sql(
            "SELECT max_ai_generations_per_problem FROM problem.problem_bank_settings WHERE id = 1")
        .query(Integer.class)
        .single();
  }

  private List<Tag> tags(UUID problem) {
    return jdbc.getObject()
        .sql(
            """
            SELECT t.id, t.name FROM problem.tags t
            JOIN problem.problem_tags pt ON pt.tag_id = t.id
            WHERE pt.problem_id = :id ORDER BY lower(t.name), t.id
            """)
        .param("id", problem)
        .query((rs, row) -> new Tag(rs.getObject("id", UUID.class), rs.getString("name")))
        .list();
  }

  private List<AuthoringTestcase> testcases(UUID problem, boolean withContent) {
    return jdbc.getObject()
        .sql(
            """
            SELECT id, position, visibility, origin, approved, category,
                   input_storage, expected_output_storage,
                   CASE WHEN :content THEN input_inline END AS input_inline,
                   CASE WHEN :content THEN input_object_key END AS input_object_key,
                   CASE WHEN :content THEN expected_output_inline END AS expected_inline,
                   CASE WHEN :content THEN expected_output_object_key END AS expected_object_key
            FROM problem.testcases WHERE problem_id = :id ORDER BY position, id
            """)
        .param("id", problem)
        .param("content", withContent)
        .query(
            (rs, row) ->
                new AuthoringTestcase(
                    rs.getObject("id", UUID.class),
                    rs.getInt("position"),
                    rs.getString("visibility"),
                    rs.getString("origin"),
                    rs.getBoolean("approved"),
                    rs.getString("category"),
                    rs.getString("input_storage"),
                    rs.getString("input_inline"),
                    rs.getString("input_object_key"),
                    rs.getString("expected_output_storage"),
                    rs.getString("expected_inline"),
                    rs.getString("expected_object_key")))
        .list();
  }

  private StoredProblemSpec spec(UUID problem) {
    return jdbc.getObject()
        .sql("SELECT * FROM problem.problem_specs WHERE problem_id = :id")
        .param("id", problem)
        .query(
            (rs, row) ->
                new StoredProblemSpec(
                    new ProblemSpecData(
                        rs.getString("matching_strategy"),
                        rs.getBigDecimal("epsilon_value"),
                        rs.getString("stdin_format_md"),
                        rs.getString("stdout_format_md"),
                        rs.getString("function_name"),
                        rs.getString("return_type"),
                        rs.getString("parameters"),
                        rs.getString("name_overrides")),
                    rs.getInt("spec_version")))
        .optional()
        .orElse(null);
  }
}

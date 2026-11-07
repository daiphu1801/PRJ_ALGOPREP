package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.problembank.domain.model.ProblemCode;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProblemContentRepository implements ProblemContentRepository {
  private final ObjectProvider<JdbcClient> jdbc;
  private final IdGeneratorPort ids;
  private final JdbcProblemContentRelations relations;

  JdbcProblemContentRepository(
      ObjectProvider<JdbcClient> jdbc, IdGeneratorPort ids, JdbcProblemContentRelations relations) {
    this.jdbc = jdbc;
    this.ids = ids;
    this.relations = relations;
  }

  @Override
  public Optional<ProblemContentSnapshot> lock(UUID problemId) {
    return jdbc.getObject()
        .sql("SELECT * FROM problem.problems WHERE id=:id AND NOT deleted FOR UPDATE")
        .param("id", problemId)
        .query(
            (rs, row) ->
                new ProblemContentSnapshot(
                    problemId,
                    rs.getString("code"),
                    rs.getObject("author_id", UUID.class),
                    rs.getString("status"),
                    new ProblemContentData(
                        rs.getString("title"),
                        rs.getString("statement_md"),
                        rs.getString("constraints_md"),
                        rs.getObject("level_id", UUID.class),
                        new ProblemContentData.Limits(
                            rs.getInt("time_limit_ms"),
                            rs.getInt("memory_limit_mb"),
                            rs.getInt("max_output_size_kb"),
                            rs.getObject("max_submissions_per_hour", Integer.class)),
                        relations.topics(problemId),
                        relations.tags(problemId),
                        relations.examples(problemId),
                        relations.solution(problemId)),
                    rs.getTimestamp("updated_at").toInstant()))
        .optional();
  }

  @Override
  public void lockCatalogues() {
    jdbc.getObject().sql("SELECT pg_advisory_xact_lock(2201001)").query((rs, row) -> true).single();
    jdbc.getObject().sql("SELECT pg_advisory_xact_lock(2201002)").query((rs, row) -> true).single();
  }

  @Override
  public Optional<UUID> firstLevel() {
    return jdbc.getObject()
        .sql("SELECT id FROM problem.problem_levels ORDER BY sort_order,code LIMIT 1")
        .query(UUID.class)
        .optional();
  }

  @Override
  public boolean cataloguesExist(UUID levelId, List<UUID> topicIds) {
    boolean level =
        jdbc.getObject()
            .sql("SELECT EXISTS(SELECT 1 FROM problem.problem_levels WHERE id=:id)")
            .param("id", levelId)
            .query(Boolean.class)
            .single();
    if (!level) {
      return false;
    }
    for (UUID topic : topicIds) {
      if (!jdbc.getObject()
          .sql("SELECT EXISTS(SELECT 1 FROM problem.topics WHERE id=:id)")
          .param("id", topic)
          .query(Boolean.class)
          .single()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public UUID create(UUID actorId, ProblemContentData content) {
    UUID id = ids.newId();
    for (int attempt = 1; attempt <= 20; attempt++) {
      int inserted =
          jdbc.getObject()
              .sql(
                  """
          INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb)
          VALUES(:id,:code,:title,:actor,:actor,:level,:time,:memory,:output) ON CONFLICT(code) DO NOTHING
          """)
              .param("id", id)
              .param("code", ProblemCode.from(content.title(), attempt))
              .param("title", content.title())
              .param("actor", actorId)
              .param("level", content.levelId())
              .param("time", content.limits().timeLimitMs())
              .param("memory", content.limits().memoryLimitMb())
              .param("output", content.limits().maxOutputSizeKb())
              .update();
      if (inserted == 1) {
        return id;
      }
    }
    throw new IllegalStateException("Problem code allocation exhausted");
  }

  @Override
  public Instant writeAiContext(
      UUID problemId, UUID actorId, String brief, boolean noFullCode, boolean questionOnly) {
    return jdbc.getObject()
        .sql(
            """
        UPDATE problem.problems SET ai_brief=:brief,no_full_code=:noFull,socratic_only=:questionOnly,updated_by=:actor,
          updated_at=GREATEST(clock_timestamp(),updated_at + interval '1 microsecond') WHERE id=:id
        RETURNING updated_at
        """)
        .param("brief", brief, Types.VARCHAR)
        .param("noFull", noFullCode)
        .param("questionOnly", questionOnly)
        .param("actor", actorId)
        .param("id", problemId)
        .query((rs, row) -> rs.getTimestamp("updated_at").toInstant())
        .single();
  }

  @Override
  public void markDuplicatedFrom(UUID problemId, UUID sourceId) {
    jdbc.getObject()
        .sql("UPDATE problem.problems SET duplicated_from_problem_id=:source WHERE id=:id")
        .param("source", sourceId)
        .param("id", problemId)
        .update();
  }

  @Override
  public ProblemContentSnapshot write(UUID problemId, UUID actorId, ProblemContentData content) {
    jdbc.getObject()
        .sql(
            """
        UPDATE problem.problems SET title=:title,statement_md=:statement,constraints_md=:constraints,level_id=:level,
          time_limit_ms=:time,memory_limit_mb=:memory,max_output_size_kb=:output,max_submissions_per_hour=:quota,
          updated_by=:actor,updated_at=GREATEST(clock_timestamp(),updated_at + interval '1 microsecond') WHERE id=:id
        """)
        .param("title", content.title())
        .param("statement", content.statementMd())
        .param("constraints", content.constraintsMd())
        .param("level", content.levelId())
        .param("time", content.limits().timeLimitMs())
        .param("memory", content.limits().memoryLimitMb())
        .param("output", content.limits().maxOutputSizeKb())
        .param("quota", content.limits().maxSubmissionsPerHour(), Types.INTEGER)
        .param("actor", actorId)
        .param("id", problemId)
        .update();
    relations.replace(problemId, content);
    return lock(problemId)
        .orElseThrow(() -> new IllegalStateException("Written problem unavailable"));
  }
}

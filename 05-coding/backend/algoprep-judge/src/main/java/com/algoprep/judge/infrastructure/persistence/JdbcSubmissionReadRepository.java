package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.HistoryWindow;
import com.algoprep.judge.domain.model.SubmissionReadState;
import com.algoprep.judge.domain.ports.out.SubmissionReadRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSubmissionReadRepository implements SubmissionReadRepository {
  private static final String COLUMNS =
      """
      SELECT id,user_id,problem_id,language,submission_model,status,submitted_at,finished_at,
      source_code,compile_error_message,passed_testcase_count,total_testcase_count,score_ratio,
      runtime_ms,memory_kb,submitted_at < now()-interval '90 days' AS expired FROM judge.submissions
      """;
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcSubmissionReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<SubmissionReadState> owned(UUID id, UUID owner) {
    return jdbc.getObject()
        .sql(COLUMNS + " WHERE id=:id AND user_id=:owner")
        .param("id", id)
        .param("owner", owner)
        .query(JdbcSubmissionReadRepository::state)
        .optional();
  }

  @Override
  public Optional<SubmissionReadState> find(UUID id) {
    return jdbc.getObject()
        .sql(COLUMNS + " WHERE id=:id")
        .param("id", id)
        .query(JdbcSubmissionReadRepository::state)
        .optional();
  }

  @Override
  public List<SubmissionReadState.Case> cases(UUID id) {
    return jdbc.getObject()
        .sql(
            """
        SELECT testcase_id,testcase_order,verdict,runtime_ms,memory_kb,is_sample,
        CASE WHEN is_sample THEN actual_output_snippet END AS actual,
        CASE WHEN is_sample AND verdict<>'AC' THEN stderr_snippet END AS stderr
        FROM judge.submission_testcase_results WHERE submission_id=:id ORDER BY testcase_order
        """)
        .param("id", id)
        .query(
            (rs, n) ->
                new SubmissionReadState.Case(
                    rs.getObject("testcase_id", UUID.class),
                    rs.getInt("testcase_order"),
                    rs.getString("verdict"),
                    rs.getInt("runtime_ms"),
                    rs.getInt("memory_kb"),
                    rs.getBoolean("is_sample"),
                    rs.getString("actual"),
                    rs.getString("stderr")))
        .list();
  }

  @Override
  public SubmissionReadState.Beats beats(SubmissionReadState s) {
    return jdbc.getObject()
        .sql(
            """
        SELECT count(*) AS total,count(*) FILTER (WHERE id<>:id AND runtime_ms>=:runtime) AS slower
        FROM judge.submissions WHERE problem_id=:problem AND language=:language AND status='ACCEPTED'
        """)
        .param("id", s.id())
        .param("runtime", s.runtime())
        .param("problem", s.problemId())
        .param("language", s.language())
        .query((rs, n) -> new SubmissionReadState.Beats(rs.getLong("total"), rs.getLong("slower")))
        .single();
  }

  @Override
  public boolean stderrEnabled() {
    return jdbc.getObject()
        .sql("SELECT return_stderr_to_student FROM judge.sandbox_settings WHERE id=1")
        .query(Boolean.class)
        .optional()
        .orElse(false);
  }

  @Override
  public List<SubmissionReadState> recent(UUID owner, UUID problem, int limit) {
    return jdbc.getObject()
        .sql(
            COLUMNS
                + " WHERE user_id=:u AND problem_id=:p ORDER BY submitted_at DESC,id DESC LIMIT :l")
        .param("u", owner)
        .param("p", problem)
        .param("l", limit)
        .query(JdbcSubmissionReadRepository::state)
        .list();
  }

  @Override
  public long count(UUID owner, String verdict, String language, Set<UUID> problems) {
    return filters("SELECT count(*) FROM judge.submissions", owner, verdict, language, problems)
        .query(Long.class)
        .single();
  }

  @Override
  public List<SubmissionReadState> history(
      UUID owner, String verdict, String language, HistoryWindow w) {
    return filters(COLUMNS, owner, verdict, language, w.problems())
        .param("offset", w.offset())
        .param("limit", w.limit())
        .query(JdbcSubmissionReadRepository::state)
        .list();
  }

  private JdbcClient.StatementSpec filters(
      String select, UUID owner, String verdict, String language, Set<UUID> problems) {
    String where = " WHERE user_id=:u";
    if (verdict != null) {
      where += " AND status=:v";
    }
    if (language != null) {
      where += " AND language=:l";
    }
    if (problems != null) {
      where += problems.isEmpty() ? " AND false" : " AND problem_id IN (:p)";
    }
    var query =
        jdbc.getObject()
            .sql(
                select
                    + where
                    + (select.equals(COLUMNS)
                        ? " ORDER BY submitted_at DESC,id DESC OFFSET :offset LIMIT :limit"
                        : ""))
            .param("u", owner);
    if (verdict != null) {
      query = query.param("v", verdict);
    }
    if (language != null) {
      query = query.param("l", language);
    }
    if (problems != null && !problems.isEmpty()) {
      query = query.param("p", problems);
    }
    return query;
  }

  private static SubmissionReadState state(ResultSet rs, int n) throws SQLException {
    var finished = rs.getObject("finished_at", OffsetDateTime.class);
    return new SubmissionReadState(
        rs.getObject("id", UUID.class),
        rs.getObject("user_id", UUID.class),
        rs.getObject("problem_id", UUID.class),
        rs.getString("language"),
        rs.getString("submission_model"),
        rs.getString("status"),
        rs.getObject("submitted_at", OffsetDateTime.class).toInstant(),
        finished == null ? null : finished.toInstant(),
        rs.getString("source_code"),
        rs.getString("compile_error_message"),
        rs.getInt("passed_testcase_count"),
        rs.getObject("total_testcase_count", Integer.class),
        rs.getBigDecimal("score_ratio"),
        rs.getObject("runtime_ms", Integer.class),
        rs.getObject("memory_kb", Integer.class),
        rs.getBoolean("expired"));
  }
}

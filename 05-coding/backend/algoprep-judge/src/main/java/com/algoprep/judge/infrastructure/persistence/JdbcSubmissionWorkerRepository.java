package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.WorkerRecords.Outcome;
import com.algoprep.judge.domain.model.WorkerRecords.Result;
import com.algoprep.judge.domain.model.WorkerRecords.State;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.model.WorkerRecords.Token;
import com.algoprep.judge.domain.ports.out.SubmissionWorkerRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Row locks are transaction-scoped; the future worker owns its separate job lease. */
@Repository
class JdbcSubmissionWorkerRepository implements SubmissionWorkerRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcSubmissionWorkerRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<State> compiling(UUID id, int specVersion) {
    return jdbc.getObject()
        .sql(
            """
      UPDATE judge.submissions s SET status='COMPILING',spec_version=:version,
        started_grading_at=COALESCE(started_grading_at,clock_timestamp()),updated_at=clock_timestamp(),
        total_testcase_count=NULL,passed_testcase_count=0,score_ratio=NULL,runtime_ms=NULL,memory_kb=NULL
      WHERE s.id=:id AND s.status IN ('PENDING','COMPILING','JUDGING')
        AND NOT EXISTS(SELECT 1 FROM judge.submission_testcase_results r WHERE r.submission_id=s.id)
      RETURNING s.*
      """)
        .param("id", id)
        .param("version", specVersion)
        .query(JdbcSubmissionWorkerRepository::state)
        .optional();
  }

  @Override
  public Optional<State> judging(UUID id, int total) {
    return jdbc.getObject()
        .sql(
            """
      UPDATE judge.submissions SET status='JUDGING',total_testcase_count=:total,updated_at=clock_timestamp()
      WHERE id=:id AND status='COMPILING' AND spec_version IS NOT NULL RETURNING *
      """)
        .param("id", id)
        .param("total", total)
        .query(JdbcSubmissionWorkerRepository::state)
        .optional();
  }

  @Override
  public Optional<State> heartbeat(UUID id) {
    return jdbc.getObject()
        .sql(
            """
      UPDATE judge.submissions SET updated_at=clock_timestamp()
      WHERE id=:id AND status='JUDGING' RETURNING *
      """)
        .param("id", id)
        .query(JdbcSubmissionWorkerRepository::state)
        .optional();
  }

  @Override
  public Optional<State> lock(UUID id) {
    return jdbc.getObject()
        .sql("SELECT * FROM judge.submissions WHERE id=:id FOR UPDATE")
        .param("id", id)
        .query(JdbcSubmissionWorkerRepository::state)
        .optional();
  }

  @Override
  public boolean resultExists(UUID id, UUID testcaseId) {
    return jdbc.getObject()
        .sql(
            "SELECT EXISTS(SELECT 1 FROM judge.submission_testcase_results WHERE submission_id=:id AND testcase_id=:testcase)")
        .param("id", id)
        .param("testcase", testcaseId)
        .query(Boolean.class)
        .single();
  }

  @Override
  public boolean issue(UUID id, UUID testcaseId, Token token) {
    return jdbc.getObject()
            .sql(
                """
      INSERT INTO judge.judge_run_tokens(id,submission_id,testcase_id,token,issued_at)
      SELECT :tokenId,:id,:testcase,:value,clock_timestamp()
      WHERE NOT EXISTS(SELECT 1 FROM judge.judge_run_tokens WHERE submission_id=:id
        AND testcase_id=:testcase AND consumed_at IS NOT NULL)
      """)
            .param("tokenId", token.id())
            .param("id", id)
            .param("testcase", testcaseId)
            .param("value", token.value())
            .update()
        == 1;
  }

  @Override
  public void save(TestcaseWrite write, State state, UUID resultId) {
    String actual = write.sample() ? snippet(write.actualOutput()) : null;
    String stderr =
        write.sample()
                && write.returnStderr()
                && state.model().equals("STANDARD_IO")
                && !write.verdict().equals("AC")
            ? snippet(write.stderr())
            : null;
    int inserted =
        jdbc.getObject()
            .sql(
                """
      INSERT INTO judge.submission_testcase_results(id,submission_id,testcase_id,testcase_order,is_sample,
        verdict,runtime_ms,memory_kb,actual_output_snippet,stderr_snippet,judge_run_token_id,created_at)
      SELECT :resultId,:id,:testcase,:position,:sample,:verdict,:runtime,:memory,:actual,:stderr,t.id,clock_timestamp()
      FROM judge.judge_run_tokens t WHERE t.id=:tokenId AND t.submission_id=:id
        AND t.testcase_id=:testcase AND t.consumed_at IS NULL
      """)
            .param("resultId", resultId)
            .param("id", write.submissionId())
            .param("testcase", write.testcaseId())
            .param("position", write.order())
            .param("sample", write.sample())
            .param("verdict", write.verdict())
            .param("runtime", write.runtime(), Types.INTEGER)
            .param("memory", write.memory(), Types.INTEGER)
            .param("actual", actual, Types.VARCHAR)
            .param("stderr", stderr, Types.VARCHAR)
            .param("tokenId", write.tokenId())
            .update();
    int consumed =
        jdbc.getObject()
            .sql(
                "UPDATE judge.judge_run_tokens SET consumed_at=clock_timestamp() WHERE id=:tokenId AND consumed_at IS NULL")
            .param("tokenId", write.tokenId())
            .update();
    if (inserted != 1 || consumed != 1) {
      throw new IllegalStateException("Invalid run token");
    }
  }

  @Override
  public List<Result> results(UUID id) {
    return jdbc.getObject()
        .sql(
            "SELECT testcase_order,verdict,runtime_ms,memory_kb FROM judge.submission_testcase_results WHERE submission_id=:id ORDER BY testcase_order")
        .param("id", id)
        .query(
            (r, i) ->
                new Result(
                    r.getInt("testcase_order"),
                    r.getString("verdict"),
                    r.getObject("runtime_ms", Integer.class),
                    r.getObject("memory_kb", Integer.class)))
        .list();
  }

  @Override
  public State finish(State before, Outcome outcome) {
    return jdbc.getObject()
        .sql(
            """
      WITH tick AS MATERIALIZED(SELECT clock_timestamp() AS at)
      UPDATE judge.submissions s SET status=:status,passed_testcase_count=:passed,total_testcase_count=:total,
        score_ratio=:score,runtime_ms=:runtime,memory_kb=:memory,failure_reason=:reason,
        compile_error_message=:compile,finished_at=tick.at,updated_at=tick.at FROM tick
      WHERE s.id=:id AND s.status IN ('PENDING','COMPILING','JUDGING') RETURNING s.*
      """)
        .param("id", before.id())
        .param("status", outcome.status())
        .param("passed", outcome.passed())
        .param("total", outcome.total(), Types.INTEGER)
        .param("score", outcome.score(), Types.NUMERIC)
        .param("runtime", outcome.runtime(), Types.INTEGER)
        .param("memory", outcome.memory(), Types.INTEGER)
        .param("reason", outcome.failureReason(), Types.VARCHAR)
        .param("compile", outcome.compileMessage(), Types.VARCHAR)
        .query(JdbcSubmissionWorkerRepository::state)
        .single();
  }

  @Override
  public boolean firstAttempt(State state) {
    return jdbc.getObject()
        .sql(
            """
      SELECT NOT EXISTS(SELECT 1 FROM judge.submissions WHERE user_id=:userId AND problem_id=:problemId
        AND (submitted_at,id)<(:submittedAt,:id))
      """)
        .param("id", state.id())
        .param("userId", state.owner().userId())
        .param("problemId", state.owner().problemId())
        .param(
            "submittedAt", OffsetDateTime.ofInstant(state.submittedAt(), java.time.ZoneOffset.UTC))
        .query(Boolean.class)
        .single();
  }

  @Override
  public void append(UUID eventId, State state, String type, String envelope) {
    jdbc.getObject()
        .sql(
            """
      INSERT INTO judge.outbox_events(id,aggregate_type,aggregate_id,event_type,payload,created_at)
      VALUES(:eventId,'Submission',:id,:type,CAST(:payload AS jsonb),:at)
      """)
        .param("eventId", eventId)
        .param("id", state.id())
        .param("type", type)
        .param("payload", envelope)
        .param("at", OffsetDateTime.ofInstant(state.updatedAt(), java.time.ZoneOffset.UTC))
        .update();
  }

  private static State state(ResultSet r, int index) throws SQLException {
    return new State(
        r.getObject("id", UUID.class),
        new Owner(r.getObject("user_id", UUID.class), r.getObject("problem_id", UUID.class)),
        r.getString("language"),
        r.getString("submission_model"),
        r.getString("status"),
        r.getObject("total_testcase_count", Integer.class),
        r.getInt("passed_testcase_count"),
        r.getObject("runtime_ms", Integer.class),
        r.getObject("memory_kb", Integer.class),
        r.getObject("submitted_at", OffsetDateTime.class).toInstant(),
        r.getObject("updated_at", OffsetDateTime.class).toInstant());
  }

  private static String snippet(String value) {
    if (value == null) {
      return null;
    }
    return value.substring(
        0, value.offsetByCodePoints(0, Math.min(4096, value.codePointCount(0, value.length()))));
  }
}

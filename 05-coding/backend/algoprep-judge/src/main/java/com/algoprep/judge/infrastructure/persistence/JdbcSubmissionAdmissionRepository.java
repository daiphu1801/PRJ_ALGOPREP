package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.PendingSubmission;
import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.ports.out.SubmissionAdmissionRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Count uses a fresh statement snapshot after the advisory lock and a single DB clock instant. */
@Repository
class JdbcSubmissionAdmissionRepository implements SubmissionAdmissionRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcSubmissionAdmissionRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean languageEnabled(String language) {
    return jdbc.getObject()
        .sql("SELECT enabled FROM judge.language_configs WHERE language=:language")
        .param("language", language)
        .query(Boolean.class)
        .single();
  }

  @Override
  public void lockPair(Owner owner) {
    jdbc.getObject()
        .sql("SELECT pg_advisory_xact_lock(hashtextextended(:pair, 0))")
        .param("pair", owner.userId() + ":" + owner.problemId())
        .query((rs, row) -> true)
        .single();
  }

  @Override
  public Instant databaseNow() {
    return jdbc.getObject()
        .sql("SELECT clock_timestamp()")
        .query(OffsetDateTime.class)
        .single()
        .toInstant();
  }

  @Override
  public long countRecent(Owner owner, Instant at) {
    return jdbc.getObject()
        .sql(
            """
        SELECT count(*) FROM judge.submissions
        WHERE user_id=:userId AND problem_id=:problemId
          AND submitted_at > :at - interval '1 hour'
        """)
        .param("userId", owner.userId())
        .param("problemId", owner.problemId())
        .param("at", OffsetDateTime.ofInstant(at, java.time.ZoneOffset.UTC))
        .query(Long.class)
        .single();
  }

  @Override
  public void insert(PendingSubmission submission) {
    jdbc.getObject()
        .sql(
            """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,
          source_code,source_input_method,status,testcase_set_version_at_grading,
          submitted_at,updated_at,passed_testcase_count,total_testcase_count)
        VALUES (:id,:userId,:problemId,:language,:model,:source,:method,'PENDING',:version,
          :at,:at,0,NULL)
        """)
        .param("id", submission.submissionId())
        .param("userId", submission.owner().userId())
        .param("problemId", submission.owner().problemId())
        .param("language", submission.program().language())
        .param("model", submission.program().submissionModel())
        .param("source", submission.program().sourceCode())
        .param("method", submission.program().sourceInputMethod())
        .param("version", submission.testcaseSetVersion())
        .param("at", OffsetDateTime.ofInstant(submission.submittedAt(), java.time.ZoneOffset.UTC))
        .update();
  }
}

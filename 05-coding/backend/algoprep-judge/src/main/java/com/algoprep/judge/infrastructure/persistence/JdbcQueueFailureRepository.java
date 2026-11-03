package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.SubmissionLifecycleView;
import com.algoprep.judge.domain.model.SubmissionLifecycleView.Timing;
import com.algoprep.judge.domain.ports.out.QueueFailureRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcQueueFailureRepository implements QueueFailureRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcQueueFailureRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<SubmissionLifecycleView> finishPending(UUID submissionId) {
    return jdbc.getObject()
        .sql(
            """
        WITH tick AS MATERIALIZED (SELECT clock_timestamp() AS at)
        UPDATE judge.submissions s SET status='SYSTEM_ERROR',failure_reason='QUEUE_UNAVAILABLE',
          score_ratio=NULL,finished_at=tick.at,updated_at=tick.at FROM tick
        WHERE s.id=:id AND s.status='PENDING'
        RETURNING s.id,s.user_id,s.problem_id,s.language,s.status,s.submitted_at,s.finished_at
        """)
        .param("id", submissionId)
        .query(JdbcQueueFailureRepository::view)
        .optional();
  }

  @Override
  public Optional<SubmissionLifecycleView> find(UUID submissionId) {
    return jdbc.getObject()
        .sql(
            "SELECT id,user_id,problem_id,language,status,submitted_at,finished_at FROM judge.submissions WHERE id=:id")
        .param("id", submissionId)
        .query(JdbcQueueFailureRepository::view)
        .optional();
  }

  @Override
  public boolean firstAttempt(SubmissionLifecycleView submission) {
    return jdbc.getObject()
        .sql(
            """
        SELECT NOT EXISTS (SELECT 1 FROM judge.submissions
          WHERE user_id=:userId AND problem_id=:problemId
            AND (submitted_at,id) < (:submittedAt,:id))
        """)
        .param("userId", submission.owner().userId())
        .param("problemId", submission.owner().problemId())
        .param("submittedAt", utc(submission.timing().submittedAt()))
        .param("id", submission.submissionId())
        .query(Boolean.class)
        .single();
  }

  @Override
  public void appendGraded(UUID eventId, UUID submissionId, String envelope, Instant finishedAt) {
    jdbc.getObject()
        .sql(
            """
        INSERT INTO judge.outbox_events(id,aggregate_type,aggregate_id,event_type,payload,created_at)
        VALUES (:eventId,'Submission',:id,'SubmissionGraded',CAST(:payload AS jsonb),:at)
        """)
        .param("eventId", eventId)
        .param("id", submissionId)
        .param("payload", envelope)
        .param("at", utc(finishedAt))
        .update();
  }

  private static SubmissionLifecycleView view(ResultSet row, int index) throws SQLException {
    var finished = row.getObject("finished_at", OffsetDateTime.class);
    return new SubmissionLifecycleView(
        row.getObject("id", UUID.class),
        new Owner(row.getObject("user_id", UUID.class), row.getObject("problem_id", UUID.class)),
        row.getString("language"),
        row.getString("status"),
        new Timing(
            row.getObject("submitted_at", OffsetDateTime.class).toInstant(),
            finished == null ? null : finished.toInstant()));
  }

  private static OffsetDateTime utc(Instant value) {
    return OffsetDateTime.ofInstant(value, java.time.ZoneOffset.UTC);
  }
}

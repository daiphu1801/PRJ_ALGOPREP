package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.ports.out.JudgeMaintenanceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcJudgeMaintenanceRepository implements JudgeMaintenanceRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcJudgeMaintenanceRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<UUID> staleCandidates(int limit, int timeoutSeconds) {
    return jdbc.getObject()
        .sql(
            """
        SELECT id FROM judge.submissions
        WHERE status IN ('PENDING','COMPILING','JUDGING')
          AND updated_at < now() - make_interval(secs => :timeout)
          AND (status <> 'PENDING' OR NOT
            (SELECT queue_paused FROM judge.cluster_settings WHERE id=1))
        ORDER BY updated_at, id LIMIT :limit
        """)
        .param("timeout", timeoutSeconds)
        .param("limit", limit)
        .query(UUID.class)
        .list();
  }

  @Override
  public boolean claimStale(UUID id, int timeoutSeconds) {
    if (!tryJobLock("JOB-JD-01")
        || !jdbc.getObject()
            .sql("SELECT pg_try_advisory_xact_lock(hashtextextended(:key,0))")
            .param("key", "judge:sub:" + id)
            .query(Boolean.class)
            .single()) {
      return false;
    }
    // Serialize pause/resume before locking submissions, matching the writer's lock order.
    boolean paused =
        jdbc.getObject()
            .sql("SELECT queue_paused FROM judge.cluster_settings WHERE id=1 FOR SHARE")
            .query(Boolean.class)
            .single();
    return jdbc.getObject()
        .sql(
            """
        SELECT id FROM judge.submissions WHERE id=:id
          AND status IN ('PENDING','COMPILING','JUDGING')
          AND updated_at < now() - make_interval(secs => :timeout)
          AND (status <> 'PENDING' OR NOT :paused)
        FOR UPDATE
        """)
        .param("id", id)
        .param("timeout", timeoutSeconds)
        .param("paused", paused)
        .query(UUID.class)
        .optional()
        .isPresent();
  }

  @Override
  public boolean tryJobLock(String job) {
    return jdbc.getObject()
        .sql("SELECT pg_try_advisory_xact_lock(hashtext(:job))")
        .param("job", job)
        .query(Boolean.class)
        .single();
  }

  @Override
  public int deletePublished(int days, int batchSize) {
    return jdbc.getObject()
        .sql(
            """
        WITH expired AS (
          SELECT id FROM judge.outbox_events
          WHERE published_at IS NOT NULL AND published_at < now() - make_interval(days => :days)
          ORDER BY published_at,id LIMIT :batch FOR UPDATE SKIP LOCKED
        )
        DELETE FROM judge.outbox_events o USING expired e WHERE o.id=e.id
          AND o.published_at IS NOT NULL AND o.published_at < now() - make_interval(days => :days)
        """)
        .param("days", days)
        .param("batch", batchSize)
        .update();
  }

  @Override
  public int deleteTestcaseDetails(int days, int batchSize) {
    return jdbc.getObject()
        .sql(
            """
        WITH expired AS (
          SELECT id FROM judge.submission_testcase_results
          WHERE created_at < now() - make_interval(days => :days)
          ORDER BY created_at,id LIMIT :batch FOR UPDATE SKIP LOCKED
        )
        DELETE FROM judge.submission_testcase_results r USING expired e WHERE r.id=e.id
          AND r.created_at < now() - make_interval(days => :days)
        """)
        .param("days", days)
        .param("batch", batchSize)
        .update();
  }

  @Override
  public boolean lockQueuePaused() {
    return jdbc.getObject()
        .sql("SELECT queue_paused FROM judge.cluster_settings WHERE id=1 FOR UPDATE")
        .query(Boolean.class)
        .single();
  }

  @Override
  public void changeQueuePaused(boolean paused) {
    jdbc.getObject()
        .sql(
            "UPDATE judge.cluster_settings SET queue_paused=:paused,updated_at=clock_timestamp() WHERE id=1")
        .param("paused", paused)
        .update();
  }

  @Override
  public void refreshPendingHeartbeat() {
    jdbc.getObject()
        .sql("UPDATE judge.submissions SET updated_at=clock_timestamp() WHERE status='PENDING'")
        .update();
  }
}

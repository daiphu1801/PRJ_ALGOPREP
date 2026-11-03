package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.exception.WorkerPersistenceException;
import com.algoprep.judge.domain.model.WorkerControl;
import com.algoprep.judge.domain.model.WorkerJob;
import com.algoprep.judge.domain.model.WorkerSettings;
import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcWorkerJobRepository implements WorkerJobRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcWorkerJobRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<WorkerJob> find(UUID id) {
    try {
      return jdbc.getObject()
          .sql(
              "SELECT s.*,EXISTS(SELECT 1 FROM judge.submission_testcase_results r WHERE r.submission_id=s.id) AS has_results FROM judge.submissions s WHERE s.id=:id")
          .param("id", id)
          .query(
              (r, n) ->
                  new WorkerJob(
                      r.getObject("id", UUID.class),
                      r.getObject("problem_id", UUID.class),
                      r.getString("language"),
                      r.getString("submission_model"),
                      r.getString("source_code"),
                      r.getInt("testcase_set_version_at_grading"),
                      r.getString("status"),
                      r.getBoolean("has_results")))
          .optional();
    } catch (RuntimeException e) {
      throw new WorkerPersistenceException();
    }
  }

  @Override
  public WorkerSettings settings(String language) {
    try {
      return jdbc.getObject()
          .sql(
              "SELECT l.time_limit_multiplier,l.memory_limit_multiplier,d.time_limit_ms,d.memory_limit_mb,d.output_limit_kb,d.compile_timeout_sec,s.network_access_enabled,s.child_process_limit_enabled,s.return_stderr_to_student FROM judge.language_configs l CROSS JOIN judge.grading_defaults d CROSS JOIN judge.sandbox_settings s WHERE l.language=:language AND d.id=1 AND s.id=1")
          .param("language", language)
          .query(
              (r, n) ->
                  new WorkerSettings(
                      r.getBigDecimal(1),
                      r.getBigDecimal(2),
                      r.getInt(3),
                      r.getInt(4),
                      r.getInt(5),
                      r.getInt(6),
                      r.getBoolean(7),
                      r.getBoolean(8),
                      r.getBoolean(9)))
          .single();
    } catch (RuntimeException e) {
      throw new WorkerPersistenceException();
    }
  }

  @Override
  public WorkerControl control() {
    try {
      return jdbc.getObject()
          .sql(
              "SELECT queue_paused,autoscale_enabled,autoscale_job_threshold,(SELECT count(*) FROM judge.submissions WHERE status='PENDING') AS pending FROM judge.cluster_settings WHERE id=1")
          .query(
              (r, n) ->
                  new WorkerControl(r.getBoolean(1), r.getBoolean(2), r.getInt(3), r.getLong(4)))
          .single();
    } catch (RuntimeException e) {
      throw new WorkerPersistenceException();
    }
  }
}

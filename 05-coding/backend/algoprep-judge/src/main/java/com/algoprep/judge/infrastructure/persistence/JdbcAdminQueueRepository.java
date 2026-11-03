package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.AdminQueueSettings;
import com.algoprep.judge.domain.model.AdminQueueState;
import com.algoprep.judge.domain.ports.out.AdminQueueRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAdminQueueRepository implements AdminQueueRepository {
  private static final Map<String, String> COLUMNS =
      Map.of(
          "autoscaleJobThreshold",
          "autoscale_job_threshold",
          "jobsPerPage",
          "jobs_per_page",
          "errorRetentionHours",
          "error_retention_hours",
          "latencyCapSeconds",
          "latency_cap_seconds");
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAdminQueueRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public AdminQueueState read() {
    return load(false);
  }

  @Override
  public AdminQueueState lock() {
    return load(true);
  }

  private AdminQueueState load(boolean locking) {
    return jdbc.getObject()
        .sql(
            "SELECT queue_paused,autoscale_enabled,autoscale_job_threshold,"
                + "jobs_per_page,error_retention_hours,latency_cap_seconds,updated_at FROM judge.cluster_settings WHERE id=1"
                + (locking ? " FOR UPDATE" : ""))
        .query(
            (rs, n) ->
                new AdminQueueState(
                    rs.getBoolean("queue_paused"),
                    rs.getBoolean("autoscale_enabled"),
                    new AdminQueueSettings(
                        rs.getInt("autoscale_job_threshold"),
                        rs.getInt("jobs_per_page"),
                        rs.getInt("error_retention_hours"),
                        rs.getInt("latency_cap_seconds"),
                        rs.getObject("updated_at", OffsetDateTime.class).toInstant())))
        .single();
  }

  @Override
  public void markPauseActor(UUID actor) {
    jdbc.getObject()
        .sql("UPDATE judge.cluster_settings SET updated_by=:actor WHERE id=1")
        .param("actor", actor)
        .update();
  }

  @Override
  public void setAutoscale(boolean enabled, UUID actor) {
    jdbc.getObject()
        .sql(
            "UPDATE judge.cluster_settings SET autoscale_enabled=:enabled,"
                + "updated_by=:actor,updated_at=clock_timestamp() WHERE id=1")
        .param("enabled", enabled)
        .param("actor", actor)
        .update();
  }

  @Override
  public void saveSettings(Map<String, Integer> changed, UUID actor) {
    var assignments = new ArrayList<String>();
    for (String field : changed.keySet()) {
      String column = COLUMNS.get(field);
      if (column == null) {
        throw new IllegalArgumentException("Unknown queue setting");
      }
      assignments.add(column + "=:" + field);
    }
    jdbc.getObject()
        .sql(
            "UPDATE judge.cluster_settings SET "
                + String.join(",", assignments)
                + ",updated_by=:actor,updated_at=clock_timestamp() WHERE id=1")
        .params(changed)
        .param("actor", actor)
        .update();
  }
}

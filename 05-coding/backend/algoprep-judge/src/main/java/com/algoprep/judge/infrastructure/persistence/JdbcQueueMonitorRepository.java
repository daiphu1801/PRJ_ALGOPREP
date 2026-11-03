package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.QueueMonitoring.Counts;
import com.algoprep.judge.domain.model.QueueMonitoring.Facts;
import com.algoprep.judge.domain.model.QueueMonitoring.Job;
import com.algoprep.judge.domain.model.QueueMonitoring.JobSettings;
import com.algoprep.judge.domain.ports.out.QueueMonitorRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcQueueMonitorRepository implements QueueMonitorRepository {
  private static final String ACTIVE = "status IN ('PENDING','COMPILING','JUDGING')";
  private static final String FAILED =
      """
      (status='SYSTEM_ERROR' AND finished_at IS NOT NULL
       AND EXTRACT(epoch FROM (CAST(:asOf AS timestamptz)-finished_at))
           <= CAST(:retention AS bigint)*3600)
      """;
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcQueueMonitorRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Facts snapshot() {
    return jdbc.getObject()
        .sql(
            """
        WITH clock AS (SELECT transaction_timestamp() AS at), stats AS (
          SELECT COUNT(*) FILTER(WHERE status='PENDING') AS pending,
            ROUND(AVG(EXTRACT(epoch FROM (started_grading_at-submitted_at))*1000)
              FILTER(WHERE started_grading_at >= clock.at-interval '1 hour'
                AND started_grading_at <= clock.at))::bigint AS avg_wait
          FROM judge.submissions CROSS JOIN clock)
        SELECT clock.at,c.queue_paused,c.autoscale_enabled,c.autoscale_job_threshold,
          c.latency_cap_seconds,stats.pending,stats.avg_wait
        FROM judge.cluster_settings c CROSS JOIN clock CROSS JOIN stats WHERE c.id=1
        """)
        .query(
            (rs, n) ->
                new Facts(
                    rs.getObject("at", OffsetDateTime.class).toInstant(),
                    rs.getBoolean("queue_paused"),
                    rs.getBoolean("autoscale_enabled"),
                    rs.getInt("autoscale_job_threshold"),
                    rs.getLong("pending"),
                    rs.getObject("avg_wait", Long.class),
                    rs.getInt("latency_cap_seconds")))
        .single();
  }

  @Override
  public JobSettings jobSettings() {
    return jdbc.getObject()
        .sql(
            """
        SELECT jobs_per_page,error_retention_hours,transaction_timestamp() AS at
        FROM judge.cluster_settings WHERE id=1
        """)
        .query(
            (rs, n) ->
                new JobSettings(
                    rs.getInt("jobs_per_page"),
                    rs.getInt("error_retention_hours"),
                    rs.getObject("at", OffsetDateTime.class).toInstant()))
        .single();
  }

  private JdbcClient.StatementSpec scoped(String sql, JobSettings settings) {
    return jdbc.getObject()
        .sql(sql)
        .param("asOf", settings.asOf().atOffset(ZoneOffset.UTC))
        .param("retention", settings.errorRetentionHours());
  }

  @Override
  public Counts counts(JobSettings settings) {
    return scoped(
            "SELECT COUNT(*) FILTER(WHERE status='PENDING') AS waiting,"
                + "COUNT(*) FILTER(WHERE status='SYSTEM_ERROR') AS failed,COUNT(*) AS total "
                + "FROM judge.submissions WHERE "
                + ACTIVE
                + " OR "
                + FAILED,
            settings)
        .query(
            (rs, n) -> new Counts(rs.getLong("waiting"), rs.getLong("failed"), rs.getLong("total")))
        .single();
  }

  @Override
  public List<Job> jobs(String tab, JobSettings settings, long offset) {
    String filter =
        switch (tab) {
          case "all" -> ACTIVE + " OR " + FAILED;
          case "failed" -> FAILED;
          default -> "status='PENDING'";
        };
    return scoped(
            """
        SELECT id,language,status,CASE WHEN status='SYSTEM_ERROR' THEN NULL
          ELSE TRUNC(EXTRACT(epoch FROM(CAST(:asOf AS timestamptz)
            -COALESCE(started_grading_at,submitted_at)))*1000)::bigint END AS waiting_ms
        FROM judge.submissions WHERE
        """
                + filter
                + " ORDER BY (status='SYSTEM_ERROR'),COALESCE(started_grading_at,submitted_at),id"
                + " LIMIT :size OFFSET :offset",
            settings)
        .param("size", settings.pageSize())
        .param("offset", offset)
        .query(
            (rs, n) ->
                new Job(
                    rs.getObject("id", UUID.class),
                    rs.getString("language"),
                    rs.getString("status"),
                    rs.getObject("waiting_ms", Long.class)))
        .list();
  }
}

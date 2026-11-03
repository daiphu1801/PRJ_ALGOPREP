package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.InfraEvents.Filter;
import com.algoprep.judge.domain.model.InfraEvents.Position;
import com.algoprep.judge.domain.model.InfraEvents.Row;
import com.algoprep.judge.domain.model.InfraEvents.Summary;
import com.algoprep.judge.domain.ports.out.InfraEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcInfraEventRepository implements InfraEventRepository {
  private static final String SOURCE =
      """
      WITH events AS (
        SELECT created_at AS occurred_at, 'o:' || id::text AS source_key,
               CASE WHEN publish_attempts >= :errorAttempts THEN 'ERROR' ELSE 'WARN' END AS level,
               'outbox' AS service, aggregate_id AS submission_id, event_type AS kind,
               publish_attempts AS attempts
        FROM judge.outbox_events WHERE publish_attempts >= :warnAttempts
        UNION ALL
        SELECT finished_at, 's:' || id::text, 'ERROR', 'go-judge', id, failure_reason, 0
        FROM judge.submissions WHERE status = 'SYSTEM_ERROR'
      ), retained AS (
        SELECT * FROM events
        WHERE occurred_at >= transaction_timestamp() - :retention * interval '1 day'
          AND occurred_at <= transaction_timestamp()
      )
      """;

  // Search the exact application template without loading payload, source or testcase columns.
  private static final String SEARCH =
      """
      replace(replace(replace(
        CASE WHEN service='outbox' THEN :OUTBOX
             WHEN kind='WORKER_TIMEOUT' THEN :WORKER_TIMEOUT
             WHEN kind='SANDBOX_FAULT' THEN :SANDBOX_FAULT
             WHEN kind='QUEUE_UNAVAILABLE' THEN :QUEUE_UNAVAILABLE
             WHEN kind='SPEC_UNAVAILABLE' THEN :SPEC_UNAVAILABLE END,
        '{id}',submission_id::text),'{kind}',kind),'{attempts}',attempts::text)
      """;

  private final ObjectProvider<JdbcClient> jdbc;
  private final Map<String, Object> policy;

  JdbcInfraEventRepository(
      ObjectProvider<JdbcClient> jdbc,
      @Value("${judge.infra.outbox-warn-attempts:3}") int warn,
      @Value("${judge.infra.outbox-error-attempts:5}") int error,
      @Value("${judge.infra.event-retention-days:90}") int retention) {
    this.jdbc = jdbc;
    this.policy = Map.of("warnAttempts", warn, "errorAttempts", error, "retention", retention);
  }

  @Override
  public Summary summary() {
    return jdbc.getObject()
        .sql(
            SOURCE
                + """
        SELECT count(*) FILTER (WHERE level='WARN') AS warns,
               count(*) FILTER (WHERE level='ERROR') AS errors
        FROM retained WHERE occurred_at >= transaction_timestamp() - interval '24 hours'
        """)
        .params(policy)
        .query((rs, n) -> new Summary(rs.getLong("warns"), rs.getLong("errors"), 24))
        .single();
  }

  @Override
  public List<Row> list(Filter filter, Map<String, String> templates) {
    var params = new HashMap<String, Object>(policy);
    var where = new StringBuilder(" SELECT * FROM retained WHERE true");
    addFilters(filter, where, params);
    if (!filter.q().isEmpty()) {
      where.append(" AND strpos(lower(").append(SEARCH).append("),lower(:q)) > 0");
      params.putAll(templates);
      params.put("q", filter.q());
    }
    String sql =
        SOURCE + where + " ORDER BY occurred_at DESC,source_key COLLATE \"C\" DESC LIMIT 21";
    return jdbc.getObject()
        .sql(sql)
        .params(params)
        .query(
            (rs, n) ->
                new Row(
                    new Position(
                        rs.getObject("occurred_at", OffsetDateTime.class).toInstant(),
                        rs.getString("source_key")),
                    rs.getString("level"),
                    rs.getString("service"),
                    rs.getObject("submission_id", UUID.class),
                    rs.getString("kind"),
                    rs.getInt("attempts")))
        .list();
  }

  private static void addFilters(Filter filter, StringBuilder where, Map<String, Object> params) {
    if (!filter.level().isEmpty()) {
      where.append(" AND lower(level)=:level");
      params.put("level", filter.level());
    }
    if (!filter.service().isEmpty()) {
      where.append(" AND service=:service");
      params.put("service", filter.service());
    }
    if (filter.from() != null) {
      where.append(" AND occurred_at >= CAST(:from AS timestamptz)");
      params.put("from", filter.from().atOffset(ZoneOffset.UTC));
    }
    if (filter.until() != null) {
      where.append(" AND occurred_at < CAST(:until AS timestamptz)");
      params.put("until", filter.until().atOffset(ZoneOffset.UTC));
    }
    if (filter.cursor() != null) {
      where.append(
          " AND (occurred_at,source_key COLLATE \"C\") < (CAST(:cursorTime AS timestamptz),:cursorKey COLLATE \"C\")");
      params.put("cursorTime", filter.cursor().occurredAt().toString());
      params.put("cursorKey", filter.cursor().sourceKey());
    }
  }
}

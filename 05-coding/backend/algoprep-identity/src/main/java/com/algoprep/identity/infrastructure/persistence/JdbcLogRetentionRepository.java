package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.ExpiredLogPolicy;
import com.algoprep.identity.domain.model.LogRetentionSettings;
import com.algoprep.identity.domain.ports.out.LogRetentionRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Timestamps come from the database clock so the cut-off never depends on app time. */
@Repository
class JdbcLogRetentionRepository implements LogRetentionRepository {
  private static final String DAYS_KEY = "audit_log.retention_days";
  private static final String POLICY_KEY = "audit_log.expired_policy";
  private static final String SELECT =
      "SELECT key, value_json #>> '{}' AS value FROM identity.system_settings"
          + " WHERE key IN ('audit_log.retention_days', 'audit_log.expired_policy')";
  private static final String UPSERT =
      """
      INSERT INTO identity.system_settings (key, value_json, updated_at, updated_by)
      VALUES (:key, CAST(:value AS jsonb), clock_timestamp(), :actor)
      ON CONFLICT (key) DO UPDATE SET value_json = EXCLUDED.value_json,
        updated_at = EXCLUDED.updated_at, updated_by = EXCLUDED.updated_by
      """;

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcLogRetentionRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public LogRetentionSettings read() {
    return load(SELECT);
  }

  @Override
  public LogRetentionSettings readForUpdate() {
    return load(SELECT + " FOR UPDATE");
  }

  private LogRetentionSettings load(String sql) {
    Map<String, String> values = new HashMap<>();
    jdbc.getObject()
        .sql(sql)
        .query((rs, n) -> values.put(rs.getString("key"), rs.getString("value")))
        .list();
    return new LogRetentionSettings(
        Integer.parseInt(values.getOrDefault(DAYS_KEY, "90")),
        ExpiredLogPolicy.valueOf(values.getOrDefault(POLICY_KEY, "DELETE")));
  }

  @Override
  public void save(LogRetentionSettings settings, UUID actorUserId) {
    upsert(DAYS_KEY, Integer.toString(settings.retentionDays()), actorUserId);
    upsert(POLICY_KEY, "\"" + settings.expiredPolicy().name() + "\"", actorUserId);
  }

  private void upsert(String key, String valueJson, UUID actor) {
    jdbc.getObject()
        .sql(UPSERT)
        .param("key", key)
        .param("value", valueJson)
        .param("actor", actor)
        .update();
  }

  @Override
  public int purgeExpiredBatch(int retentionDays, int limit) {
    return jdbc.getObject()
        .sql(
            """
        WITH due AS (
          SELECT id FROM identity.system_audit_logs
          WHERE created_at < clock_timestamp() - make_interval(days => :days)
          ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED),
        gone AS (
          DELETE FROM identity.system_audit_logs l USING due WHERE l.id = due.id RETURNING l.id)
        SELECT count(*) FROM gone
        """)
        .param("days", retentionDays)
        .param("limit", limit)
        .query(Integer.class)
        .single();
  }
}

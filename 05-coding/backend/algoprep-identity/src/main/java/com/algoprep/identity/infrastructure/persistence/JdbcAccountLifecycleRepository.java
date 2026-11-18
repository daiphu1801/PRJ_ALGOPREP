package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.ports.out.AccountLifecycleRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Timestamps come from the database clock so grace-period checks never depend on app time. */
@Repository
class JdbcAccountLifecycleRepository implements AccountLifecycleRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAccountLifecycleRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean isLastActiveAdmin(UUID userId) {
    // FOR UPDATE cannot sit on COUNT(*), so the rows are locked and counted in the application.
    boolean isActiveAdmin =
        Boolean.TRUE.equals(
            jdbc.getObject()
                .sql(
                    "SELECT EXISTS (SELECT 1 FROM identity.users u JOIN identity.roles r"
                        + " ON r.id = u.role_id WHERE u.id = :id AND u.status = 'ACTIVE'"
                        + " AND r.base_category = 'ADMIN')")
                .param("id", userId)
                .query(Boolean.class)
                .single());
    if (!isActiveAdmin) {
      return false; // nothing to protect, and the admin rows are not locked for nobody
    }
    // "Last" means the last active admin, or the last active admin whose role can edit the matrix:
    // losing either leaves nobody able to repair access from the application (IDT-420, IDT-432).
    List<Map<String, Object>> admins =
        jdbc.getObject()
            .sql(
                """
        SELECT u.id,
          EXISTS (SELECT 1 FROM identity.permissions p
                  JOIN identity.functions f ON f.id = p.function_id
                  JOIN identity.actions a ON a.id = p.action_id
                  WHERE p.role_id = r.id AND p.granted
                    AND f.code = 'PERMISSION_MATRIX' AND a.code = 'UPDATE') AS editor
        FROM identity.users u JOIN identity.roles r ON r.id = u.role_id
        WHERE r.base_category = 'ADMIN' AND u.status = 'ACTIVE'
        ORDER BY u.id FOR UPDATE OF u
        """)
            .query()
            .listOfRows();
    if (admins.size() == 1 && admins.get(0).get("id").equals(userId)) {
      return true;
    }
    List<Object> editors =
        admins.stream()
            .filter(row -> Boolean.TRUE.equals(row.get("editor")))
            .map(r -> r.get("id"))
            .toList();
    return editors.size() == 1 && editors.get(0).equals(userId);
  }

  @Override
  public boolean deactivateSelf(UUID userId) {
    JdbcClient client = jdbc.getObject();
    int changed =
        client
            .sql(
                """
        UPDATE identity.users SET status = 'DEACTIVATED', deactivation_source = 'SELF',
          deactivated_at = clock_timestamp(), updated_at = clock_timestamp()
        WHERE id = :id AND status = 'ACTIVE'
        """)
            .param("id", userId)
            .update();
    if (changed == 0) {
      return false;
    }
    client
        .sql(
            "UPDATE identity.refresh_tokens SET revoked_at = clock_timestamp()"
                + " WHERE user_id = :id AND revoked_at IS NULL")
        .param("id", userId)
        .update();
    return true;
  }

  @Override
  public int deleteStalePendingBatch(Duration ttl, int limit) {
    return jdbc.getObject()
        .sql(
            """
        WITH due AS (
          SELECT u.id FROM identity.users u
          WHERE u.status = 'PENDING_VERIFICATION' AND NOT u.must_change_password
            AND u.created_at <= clock_timestamp() - make_interval(secs => :seconds)
            AND NOT EXISTS (SELECT 1 FROM identity.system_audit_logs a WHERE a.actor_user_id = u.id)
            AND NOT EXISTS (SELECT 1 FROM identity.identity_recent_activity r
                            WHERE r.actor_user_id = u.id)
          ORDER BY u.created_at LIMIT :limit FOR UPDATE SKIP LOCKED),
        prefs AS (DELETE FROM identity.user_preferences WHERE user_id IN (SELECT id FROM due)),
        links AS (DELETE FROM identity.oauth_identities WHERE user_id IN (SELECT id FROM due)),
        tokens AS (DELETE FROM identity.refresh_tokens WHERE user_id IN (SELECT id FROM due)),
        gone AS (DELETE FROM identity.users WHERE id IN (SELECT id FROM due) RETURNING id)
        SELECT count(*) FROM gone
        """)
        .param("seconds", ttl.toSeconds())
        .param("limit", limit)
        .query(Integer.class)
        .single();
  }

  @Override
  public int anonymizeDueBatch(int limit) {
    // One statement: the data-modifying CTEs all run, so the users and their OAuth links change
    // together or not at all.
    return jdbc.getObject()
        .sql(
            """
        WITH due AS (
          SELECT id FROM identity.users
          WHERE status = 'DEACTIVATED' AND deactivation_source = 'SELF'
            AND deactivated_at <= clock_timestamp() - interval '30 days'
            AND anonymized_at IS NULL
          ORDER BY deactivated_at LIMIT :limit FOR UPDATE SKIP LOCKED),
        anonymized AS (
          UPDATE identity.users u SET email = 'deleted-user-' || u.id || '@anon.local',
            display_name = 'Người dùng đã xoá', phone = NULL, school_or_company = NULL,
            current_position = NULL, target_position = NULL, password_hash = NULL,
            anonymized_at = clock_timestamp(), updated_at = clock_timestamp()
          FROM due WHERE u.id = due.id RETURNING u.id),
        unlinked AS (
          DELETE FROM identity.oauth_identities WHERE user_id IN (SELECT id FROM anonymized))
        SELECT count(*) FROM anonymized
        """)
        .param("limit", limit)
        .query(Integer.class)
        .single();
  }
}

package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.identity.domain.model.AuditCategory;
import com.algoprep.identity.domain.model.AuditLogCursor;
import com.algoprep.identity.domain.model.AuditLogFilter;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F1 admin user management and audit-log read persistence (search filters, paging, counters). */
class JdbcAdminRepositoriesTest {
  private final FakeDb db = new FakeDb();
  private final JdbcAdminUserRepository users = new JdbcAdminUserRepository(db.provider());
  private final JdbcAuditLogReadRepository audit = new JdbcAuditLogReadRepository(db.provider());
  private final UUID id = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-05-01T00:00:00Z");

  @Test
  void searchEscapesLikeWildcardsAndComputesOffset() {
    db.rows("SELECT count(*) FROM identity.users u JOIN", Map.of("count", 42L));
    db.generic("ORDER BY u.created_at DESC", 2);
    var page = users.search("ACTIVE", "STUDENT", "50%_a\\", 3, 10);
    assertThat(page.total()).isEqualTo(42);
    assertThat(page.items()).hasSize(2);
    var items = db.calls().get(1).values();
    String bs = String.valueOf((char) 92);
    assertThat(items).contains("%50" + bs + "%" + bs + "_a" + bs + bs + "%", 10, 20L);
  }

  @Test
  void searchWithoutFiltersBindsNulls() {
    db.rows("SELECT count(*) FROM identity.users u JOIN", Map.of("count", 0L));
    var page = users.search(null, null, null, 1, 5);
    assertThat(page.items()).isEmpty();
    assertThat(db.calls().get(0).values()).containsOnlyNulls();
  }

  @Test
  void findAndLockMapRowsIncludingNullLastActive() {
    java.util.HashMap<String, Object> noActivity = new java.util.HashMap<>();
    noActivity.put("id", id);
    noActivity.put("last_active_at", null);
    db.rows("WHERE u.id = ?", noActivity);
    db.rows(
        "FOR UPDATE OF u",
        Map.of("id", id, "status", "ACTIVE", "solved_count", 4L, "last_active_at", Instant.EPOCH));
    assertThat(users.find(id)).get().extracting("lastActiveAt").isNull();
    var locked = users.lock(id).orElseThrow();
    assertThat(locked.lastActiveAt()).isEqualTo(Instant.EPOCH);
    assertThat(locked.solvedCount()).isEqualTo(4);
  }

  @Test
  void countersAndGroupCountsDefaultToZero() {
    db.rows("PENDING_VERIFICATION'", Map.of("count", 3L));
    assertThat(users.countPendingVerification()).isEqualTo(3);
    assertThat(users.groupCounts())
        .containsEntry("STUDENT", 0L)
        .containsEntry("ADMIN", 0L)
        .containsEntry("DEACTIVATED", 0L);
    db.rows("GROUP BY 1", Map.of("grp", "STUDENT", "n", 9L));
    assertThat(users.groupCounts()).containsEntry("STUDENT", 9L);
  }

  @Test
  void insertCreatedByAdminReportsConflict() {
    assertThat(users.insertCreatedByAdmin(id, "a@b.c", "h", "A", id)).isTrue();
    db.updateCount("ON CONFLICT (lower(email))", 0);
    assertThat(users.insertCreatedByAdmin(id, "a@b.c", "h", "A", id)).isFalse();
  }

  @Test
  void adminActionsIssueTargetedUpdates() {
    users.changeRole(id, id);
    assertThat(db.last().sql()).contains("SET role_id = ?");
    users.deactivateByAdmin(id);
    assertThat(db.last().sql()).contains("status = 'DEACTIVATED'", "deactivation_source = 'ADMIN'");
    users.reactivate(id);
    assertThat(db.last().sql()).contains("status = 'ACTIVE'");
    users.setTemporaryPassword(id, "hash");
    assertThat(db.last().sql()).contains("must_change_password = true");
    assertThat(db.last().values()).contains("hash", id);
    users.revokeAllSessions(id);
    assertThat(db.last().sql()).contains("refresh_tokens SET revoked_at");
  }

  @Test
  void auditSearchAppliesEveryFilterAndCursor() {
    db.generic("FROM identity.system_audit_logs a", 2);
    var filter = new AuditLogFilter("auth", AuditCategory.AUTH, now, now.plusSeconds(60));
    var rows = audit.search(filter, new AuditLogCursor(now, id), 20);
    assertThat(rows).hasSize(2);
    String sql = db.last().sql();
    assertThat(sql)
        .contains("a.created_at >=", "a.created_at <", "(a.created_at, a.id) <", "LIMIT");
    assertThat(db.last().values()).contains(20, id);
  }

  @Test
  void auditSearchWithoutFilterHasNoPredicates() {
    audit.search(AuditLogFilter.none(), null, 5);
    assertThat(db.last().sql()).doesNotContain("a.created_at >=", "starts_with");
  }

  @Test
  void auditNewerSupportsOptionalUpperBound() {
    db.generic("ASC LIMIT", 1);
    var cur = new AuditLogCursor(now, id);
    assertThat(audit.newer(cur, null, 10)).hasSize(1);
    assertThat(db.last().sql()).doesNotContain("<=");
    audit.newer(cur, new AuditLogCursor(now.plusSeconds(5), id), 10);
    assertThat(db.last().sql()).contains("<=");
  }

  @Test
  void retentionDaysParsesStoredValueOrFallsBackTo90() {
    assertThat(audit.retentionDays()).isEqualTo(90);
    FakeDb d = new FakeDb().rows("audit_log.retention_days", Map.of("v", " 30 "));
    assertThat(new JdbcAuditLogReadRepository(d.provider()).retentionDays()).isEqualTo(30);
    FakeDb bad = new FakeDb().rows("audit_log.retention_days", Map.of("v", "abc"));
    assertThat(new JdbcAuditLogReadRepository(bad.provider()).retentionDays()).isEqualTo(90);
  }

  @Test
  void activeAdminsAndActionCountsAndLatest() {
    db.rows("count(*) AS cnt,", Map.of("id", id, "cnt", 6L, "last_at", Timestamp.from(now)));
    db.rows("GROUP BY action_type", Map.of("action_type", "auth.login", "cnt", 2L));
    db.rows(
        "ORDER BY created_at DESC, id DESC LIMIT 1",
        Map.of("id", id, "created_at", Timestamp.from(now)));
    assertThat(audit.activeAdmins()).hasSize(1);
    assertThat(audit.actionCountsLastSevenDays()).containsEntry("auth.login", 2L);
    assertThat(audit.latest()).get().extracting("createdAt").isEqualTo(now);
  }

  @Test
  void findWithinRetentionBindsRetentionDays() {
    assertThat(audit.findWithinRetention(id)).isEmpty();
    assertThat(db.last().values()).contains(id, 90);
  }
}

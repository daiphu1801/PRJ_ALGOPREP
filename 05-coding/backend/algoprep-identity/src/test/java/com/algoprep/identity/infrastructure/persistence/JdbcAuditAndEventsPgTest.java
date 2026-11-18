package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.model.audit.AuditCategory;
import com.algoprep.identity.domain.model.audit.AuditEntry;
import com.algoprep.identity.domain.model.audit.AuditLogCursor;
import com.algoprep.identity.domain.model.audit.AuditLogFilter;
import com.algoprep.identity.domain.model.audit.AuditLogRow;
import com.algoprep.identity.domain.model.audit.ExpiredLogPolicy;
import com.algoprep.identity.domain.model.audit.LogRetentionSettings;
import com.algoprep.identity.domain.model.outbox.OutboxEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/** Audit log, retention settings, outbox and processed-events idempotency on PostgreSQL. */
class JdbcAuditAndEventsPgTest extends PostgresTestBase {
  private final JdbcAuditLogReadRepository audit = new JdbcAuditLogReadRepository(provider());
  private final JdbcLogRetentionRepository retention = new JdbcLogRetentionRepository(provider());
  private final JdbcIdentityOutboxRepository outbox = new JdbcIdentityOutboxRepository(provider());
  private final JdbcProcessedEventRepository processed =
      new JdbcProcessedEventRepository(provider());

  private UUID log(UUID actor, String action, Instant at) {
    UUID id = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO identity.system_audit_logs(id,actor_user_id,action_type,target_type,created_at)"
                + " VALUES (:id,:a,:t,'X',:at)")
        .param("id", id)
        .param("a", actor)
        .param("t", action)
        .param("at", utc(at))
        .update();
    return id;
  }

  private static Instant ago(long hours) {
    return Instant.now().minus(hours, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
  }

  // ---- audit writer --------------------------------------------------------------------------

  @Test
  void auditAdapterWritesJsonbAndRejectsUnknownActor() {
    UUID actor = user("a@x.io", "ADMIN");
    Instant at = Instant.parse("2026-01-02T03:04:05Z");
    var adapter = new JdbcAuditLogAdapter(provider(), UUID::randomUUID, () -> at);
    adapter.record(new AuditEntry(actor, "ROLE_CREATED", "ROLE", "r-1", "{\"a\":1}", null));
    var row =
        jdbc.sql(
                "SELECT target_id, before_json->>'a', after_json IS NULL, created_at"
                    + " FROM identity.system_audit_logs")
            .query(
                (rs, n) ->
                    rs.getString(1)
                        + "|"
                        + rs.getString(2)
                        + "|"
                        + rs.getBoolean(3)
                        + "|"
                        + rs.getTimestamp(4).toInstant())
            .single();
    assertThat(row).isEqualTo("r-1|1|true|" + at);

    var stranger = new AuditEntry(UUID.randomUUID(), "ROLE_CREATED", "ROLE", null, null, null);
    assertThatThrownBy(() -> adapter.record(stranger))
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23503"));
    assertThat(count("system_audit_logs")).isEqualTo(1);
  }

  // ---- audit search --------------------------------------------------------------------------

  @Test
  void searchFiltersByTimeWindowCategoryAndUnmappedActionsFallIntoConfig() {
    UUID actor = user("a@x.io", "ADMIN");
    log(actor, "LOGIN_FAILED", ago(5));
    log(actor, "ROLE_UPDATED", ago(4));
    log(actor, "PROBLEM_DELETED", ago(3));
    log(actor, "MYSTERY_ACTION", ago(2));
    log(actor, "AI_CONFIG_UPDATED", ago(1));

    assertThat(types(audit.search(AuditLogFilter.none(), null, 50))).hasSize(5);
    assertThat(types(audit.search(cat(AuditCategory.AUTH), null, 50)))
        .containsExactly("LOGIN_FAILED");
    assertThat(types(audit.search(cat(AuditCategory.PERMISSION), null, 50)))
        .containsExactly("ROLE_UPDATED");
    assertThat(types(audit.search(cat(AuditCategory.CONTENT), null, 50)))
        .containsExactly("PROBLEM_DELETED");
    assertThat(types(audit.search(cat(AuditCategory.CONFIG), null, 50)))
        .containsExactly("AI_CONFIG_UPDATED", "MYSTERY_ACTION");

    var window = new AuditLogFilter(null, null, ago(4).minusSeconds(600), ago(2).minusSeconds(600));
    assertThat(types(audit.search(window, null, 50)))
        .containsExactly("PROBLEM_DELETED", "ROLE_UPDATED");
  }

  @Test
  void searchKeywordMatchesEmailServiceAndEventCodePrefix() {
    UUID alice = user("alice@x.io", "ADMIN");
    UUID bob = user("bob@x.io", "ADMIN");
    UUID first = log(alice, "ROLE_UPDATED", ago(3));
    log(bob, "LOGIN_FAILED", ago(2));
    log(bob, "AI_MODEL_CHANGED", ago(1));

    assertThat(types(audit.search(kw("alice"), null, 50))).containsExactly("ROLE_UPDATED");
    assertThat(types(audit.search(kw("auth"), null, 50))).containsExactly("LOGIN_FAILED");
    assertThat(types(audit.search(kw("ai-config"), null, 50))).containsExactly("AI_MODEL_CHANGED");
    String code = first.toString().substring(0, 8);
    assertThat(types(audit.search(kw(code), null, 50))).containsExactly("ROLE_UPDATED");
    assertThat(types(audit.search(kw("evt_" + code), null, 50))).containsExactly("ROLE_UPDATED");
    assertThat(audit.search(kw("nobody-matches-this"), null, 50)).isEmpty();
  }

  @Test
  void searchCursorPagesNewestFirstWithoutGapsOrRepeatsOnEqualTimestamps() {
    UUID actor = user("a@x.io", "ADMIN");
    Instant same = ago(2);
    for (int i = 0; i < 4; i++) {
      log(actor, "ROLE_UPDATED", same);
    }
    log(actor, "ROLE_UPDATED", ago(1));
    log(actor, "ROLE_UPDATED", ago(3));

    List<UUID> seen = new java.util.ArrayList<>();
    AuditLogCursor cursor = null;
    for (int guard = 0; guard < 10; guard++) {
      List<AuditLogRow> page = audit.search(AuditLogFilter.none(), cursor, 2);
      if (page.isEmpty()) {
        break;
      }
      page.forEach(r -> seen.add(r.id()));
      AuditLogRow last = page.get(page.size() - 1);
      cursor = new AuditLogCursor(last.createdAt(), last.id());
    }
    assertThat(seen).hasSize(6).doesNotHaveDuplicates();
    assertThat(seen)
        .isEqualTo(
            jdbc.sql("SELECT id FROM identity.system_audit_logs ORDER BY created_at DESC, id DESC")
                .query(UUID.class)
                .list());
  }

  @Test
  void newerReturnsRowsAfterCursorAscendingUpToBound() {
    UUID actor = user("a@x.io", "ADMIN");
    UUID a = log(actor, "ROLE_UPDATED", ago(4));
    UUID b = log(actor, "ROLE_UPDATED", ago(3));
    UUID c = log(actor, "ROLE_UPDATED", ago(2));
    UUID d = log(actor, "ROLE_UPDATED", ago(1));
    var from = audit.findWithinRetention(a).orElseThrow();
    var upTo = audit.findWithinRetention(c).orElseThrow();

    assertThat(audit.newer(from, null, 10)).extracting(AuditLogRow::id).containsExactly(b, c, d);
    assertThat(audit.newer(from, upTo, 10)).extracting(AuditLogRow::id).containsExactly(b, c);
    assertThat(audit.newer(from, upTo, 1)).extracting(AuditLogRow::id).containsExactly(b);
    assertThat(audit.latest().orElseThrow().id()).isEqualTo(d);
  }

  @Test
  void latestIsEmptyAndRetentionWindowExcludesOldRows() {
    UUID actor = user("a@x.io", "ADMIN");
    assertThat(audit.latest()).isEmpty();
    UUID old = log(actor, "ROLE_UPDATED", Instant.now().minus(120, ChronoUnit.DAYS));
    UUID fresh = log(actor, "ROLE_UPDATED", ago(1));
    assertThat(audit.retentionDays()).isEqualTo(90);
    assertThat(audit.findWithinRetention(old)).isEmpty();
    assertThat(audit.findWithinRetention(fresh)).isPresent();
    assertThat(audit.findWithinRetention(UUID.randomUUID())).isEmpty();
  }

  @Test
  void retentionDaysReadsSettingAndFallsBackOnGarbage() {
    jdbc.sql(
            "UPDATE identity.system_settings SET value_json = '30'::jsonb WHERE key='audit_log.retention_days'")
        .update();
    assertThat(audit.retentionDays()).isEqualTo(30);
    jdbc.sql(
            "UPDATE identity.system_settings SET value_json = '\"abc\"'::jsonb WHERE key='audit_log.retention_days'")
        .update();
    assertThat(audit.retentionDays()).isEqualTo(90);
    jdbc.sql("DELETE FROM identity.system_settings WHERE key='audit_log.retention_days'").update();
    assertThat(audit.retentionDays()).isEqualTo(90);
  }

  @Test
  void dashboardAggregatesCountOnlyAdminActorsInsideTheirWindows() {
    UUID busy = user("busy@x.io", "ADMIN");
    UUID quiet = user("quiet@x.io", "ADMIN");
    UUID teacher = user("t@x.io", "INSTRUCTOR");
    log(busy, "ROLE_UPDATED", ago(1));
    log(busy, "ROLE_UPDATED", ago(2));
    log(quiet, "LOGIN_FAILED", ago(3));
    log(quiet, "LOGIN_FAILED", ago(30));
    log(teacher, "ROLE_UPDATED", ago(1));
    log(busy, "ROLE_UPDATED", Instant.now().minus(10, ChronoUnit.DAYS));

    var admins = audit.activeAdmins();
    assertThat(admins)
        .extracting(a -> a.displayName())
        .containsExactly("Name of busy@x.io", "Name of quiet@x.io");
    assertThat(admins.get(0).actionCount()).isEqualTo(2);
    assertThat(admins.get(1).actionCount()).isEqualTo(1);

    var counts = audit.actionCountsLastSevenDays();
    assertThat(counts).containsEntry("ROLE_UPDATED", 3L).containsEntry("LOGIN_FAILED", 2L);
  }

  // ---- retention settings --------------------------------------------------------------------

  @Test
  void retentionSettingsUpsertKeepsOneRowPerKeyAndRecordsActor() {
    UUID actor = user("a@x.io", "ADMIN");
    assertThat(retention.read()).isEqualTo(new LogRetentionSettings(90, ExpiredLogPolicy.DELETE));
    retention.save(new LogRetentionSettings(45, ExpiredLogPolicy.ARCHIVE), actor);
    retention.save(new LogRetentionSettings(60, ExpiredLogPolicy.ARCHIVE), actor);
    assertThat(count("system_settings")).isEqualTo(2);
    assertThat(retention.read()).isEqualTo(new LogRetentionSettings(60, ExpiredLogPolicy.ARCHIVE));
    assertThat(
            jdbc.sql(
                    "SELECT updated_by FROM identity.system_settings WHERE key='audit_log.retention_days'")
                .query(UUID.class)
                .single())
        .isEqualTo(actor);
  }

  @Test
  void readForUpdateLocksSettingRowsAgainstOtherWriters() {
    tx.executeWithoutResult(
        s -> {
          assertThat(retention.readForUpdate().retentionDays()).isEqualTo(90);
          assertThat(settingLocked()).isTrue();
        });
    assertThat(settingLocked()).isFalse();
  }

  private boolean settingLocked() {
    try (Connection other = data.getConnection();
        var st =
            other.prepareStatement(
                "SELECT 1 FROM identity.system_settings WHERE key = 'audit_log.retention_days'"
                    + " FOR UPDATE NOWAIT")) {
      other.setAutoCommit(false);
      st.execute();
      return false;
    } catch (SQLException e) {
      return "55P03".equals(e.getSQLState());
    }
  }

  @Test
  void purgeExpiredBatchDeletesOldestFirstWithinLimit() {
    UUID actor = user("a@x.io", "ADMIN");
    UUID oldest = log(actor, "ROLE_UPDATED", Instant.now().minus(200, ChronoUnit.DAYS));
    UUID older = log(actor, "ROLE_UPDATED", Instant.now().minus(150, ChronoUnit.DAYS));
    UUID oldish = log(actor, "ROLE_UPDATED", Instant.now().minus(100, ChronoUnit.DAYS));
    UUID fresh = log(actor, "ROLE_UPDATED", ago(1));

    assertThat(retention.purgeExpiredBatch(90, 2)).isEqualTo(2);
    assertThat(ids("system_audit_logs")).containsExactlyInAnyOrder(oldish, fresh);
    assertThat(retention.purgeExpiredBatch(90, 2)).isEqualTo(1);
    assertThat(ids("system_audit_logs")).containsExactly(fresh);
    assertThat(retention.purgeExpiredBatch(90, 2)).isZero();
    assertThat(oldest).isNotEqualTo(older);
  }

  private List<UUID> ids(String table) {
    return jdbc.sql("SELECT id FROM identity." + table).query(UUID.class).list();
  }

  // ---- processed events ----------------------------------------------------------------------

  @Test
  void markProcessedIsIdempotentByEventId() {
    UUID event = UUID.randomUUID();
    assertThat(processed.markProcessed(event, "SubmissionJudged")).isTrue();
    assertThat(processed.markProcessed(event, "SubmissionJudged")).isFalse();
    assertThat(processed.markProcessed(event, "OtherType")).isFalse();
    assertThat(processed.markProcessed(UUID.randomUUID(), "SubmissionJudged")).isTrue();
    assertThat(count("processed_domain_events")).isEqualTo(2);
    assertThat(
            jdbc.sql("SELECT event_type FROM identity.processed_domain_events WHERE event_id=:e")
                .param("e", event)
                .query(String.class)
                .single())
        .isEqualTo("SubmissionJudged");
  }

  // ---- outbox --------------------------------------------------------------------------------

  private UUID outboxRow(Instant createdAt, Instant publishedAt) {
    UUID id = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO identity.outbox_events(id,aggregate_type,aggregate_id,event_type,"
                + "routing_key,payload,created_at,published_at)"
                + " VALUES (:id,'CLASS',:agg,'ClassCreated','class.created','{\"k\":1}'::jsonb,:c,:p)")
        .param("id", id)
        .param("agg", UUID.randomUUID())
        .param("c", utc(createdAt))
        .param(
            "p",
            publishedAt == null ? null : utc(publishedAt),
            java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
        .update();
    return id;
  }

  @Test
  void lockPendingReturnsUnpublishedOldestFirstAndSkipsRowsLockedByAnotherWorker()
      throws SQLException {
    UUID first = outboxRow(ago(5), null);
    UUID second = outboxRow(ago(4), null);
    outboxRow(ago(3), ago(2));
    UUID third = outboxRow(ago(1), null);

    List<OutboxEvent> locked = outbox.lockPending(2);
    assertThat(locked).extracting(OutboxEvent::id).containsExactly(first, second);
    assertThat(locked.get(0).envelope()).contains("\"k\"");
    assertThat(locked.get(0).routingKey()).isEqualTo("class.created");

    tx.executeWithoutResult(
        s -> {
          assertThat(outbox.lockPending(1)).extracting(OutboxEvent::id).containsExactly(first);
          // A second worker on another connection skips the row held here (SKIP LOCKED).
          assertThat(otherWorkerSees(3)).containsExactly(second, third);
        });
  }

  private List<UUID> otherWorkerSees(int limit) {
    try (Connection other = data.getConnection()) {
      other.setAutoCommit(false);
      try (var st =
          other.prepareStatement(
              "SELECT id FROM identity.outbox_events WHERE published_at IS NULL"
                  + " ORDER BY created_at, id LIMIT ? FOR UPDATE SKIP LOCKED")) {
        st.setInt(1, limit);
        var rs = st.executeQuery();
        List<UUID> out = new java.util.ArrayList<>();
        while (rs.next()) {
          out.add(rs.getObject(1, UUID.class));
        }
        return out;
      } finally {
        other.rollback();
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void attemptedCountsEveryTryAndPublishesOnlyWhenConfirmedAndOnlyOnce() {
    UUID id = outboxRow(ago(1), null);
    outbox.attempted(id, false);
    assertThat(attempts(id)).isEqualTo(1);
    assertThat(publishedAt(id)).isNull();
    outbox.attempted(id, true);
    assertThat(attempts(id)).isEqualTo(2);
    assertThat(publishedAt(id)).isNotNull();
    Instant stamped = publishedAt(id);
    // A late duplicate confirmation changes nothing: published rows are frozen.
    outbox.attempted(id, true);
    assertThat(attempts(id)).isEqualTo(2);
    assertThat(publishedAt(id)).isEqualTo(stamped);
    assertThat(outbox.lockPending(10)).isEmpty();
  }

  @Test
  void attemptedSaturatesAtIntegerMaxAndRejectsNegativeAttempts() {
    UUID id = outboxRow(ago(1), null);
    jdbc.sql("UPDATE identity.outbox_events SET publish_attempts = 2147483647 WHERE id=:id")
        .param("id", id)
        .update();
    outbox.attempted(id, false);
    assertThat(attempts(id)).isEqualTo(Integer.MAX_VALUE);
    assertThatThrownBy(
            () ->
                jdbc.sql("UPDATE identity.outbox_events SET publish_attempts = -1 WHERE id=:id")
                    .param("id", id)
                    .update())
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23514"));
  }

  @Test
  void deletePublishedOlderThanDaysKeepsPendingAndRecentRowsAndHonoursLimit() {
    Instant longAgo = Instant.now().minus(30, ChronoUnit.DAYS);
    UUID p1 = outboxRow(longAgo, longAgo);
    UUID p2 = outboxRow(longAgo, longAgo.plusSeconds(1));
    UUID recent = outboxRow(ago(2), ago(1));
    UUID pending = outboxRow(longAgo, null);

    assertThat(outbox.deletePublishedOlderThanDays(7, 1)).isEqualTo(1);
    assertThat(ids("outbox_events")).containsExactlyInAnyOrder(p2, recent, pending);
    assertThat(outbox.deletePublishedOlderThanDays(7, 10)).isEqualTo(1);
    assertThat(ids("outbox_events")).containsExactlyInAnyOrder(recent, pending);
    assertThat(p1).isNotNull();
  }

  private int attempts(UUID id) {
    return jdbc.sql("SELECT publish_attempts FROM identity.outbox_events WHERE id=:id")
        .param("id", id)
        .query(Integer.class)
        .single();
  }

  private Instant publishedAt(UUID id) {
    return jdbc.sql("SELECT published_at FROM identity.outbox_events WHERE id=:id")
        .param("id", id)
        .query((rs, n) -> rs.getTimestamp(1) == null ? null : rs.getTimestamp(1).toInstant())
        .list()
        .get(0);
  }

  private static AuditLogFilter cat(AuditCategory category) {
    return new AuditLogFilter(null, category, null, null);
  }

  private static AuditLogFilter kw(String keyword) {
    return new AuditLogFilter(keyword, null, null, null);
  }

  private static List<String> types(List<AuditLogRow> rows) {
    return rows.stream().map(AuditLogRow::actionType).toList();
  }
}

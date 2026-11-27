package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.model.account.RestorationGrant;
import com.algoprep.identity.domain.model.audit.AuditEntry;
import com.algoprep.identity.domain.model.registration.RegistrationAccount;
import com.algoprep.identity.domain.ports.out.UserAccountRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F1 supporting persistence: password reset/change, restoration, outbox, registration, lookups. */
class JdbcSupportRepositoriesTest {
  private final FakeDb db = new FakeDb();
  private final UUID id = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-05-01T00:00:00Z");
  private final UserAccountRepository accounts = mock(UserAccountRepository.class);
  private final UserAccount account =
      new UserAccount(id, "h", "A", UserStatus.ACTIVE, false, "STUDENT", BaseCategory.STUDENT);

  @Test
  void passwordResetLockReadsDatabaseClockAndAccount() {
    when(accounts.findById(id)).thenReturn(Optional.of(account));
    db.rows("SELECT clock_timestamp()", Map.of("t", Timestamp.from(now)));
    db.rows("FROM identity.users WHERE lower(email)", Map.of("id", id, "email", "a@b.c"));
    var repo = new JdbcPasswordResetRepository(db.provider(), accounts);
    var locked = repo.lockByEmail("a@b.c").orElseThrow();
    assertThat(locked.user()).isEqualTo(account);
    assertThat(locked.databaseTime()).isEqualTo(now);
    assertThat(locked.passwordChangedAt()).isNotNull();
  }

  @Test
  void passwordResetLockMapsNullPasswordChangeAndMissingEmail() {
    when(accounts.findById(id)).thenReturn(Optional.of(account));
    var repo = new JdbcPasswordResetRepository(db.provider(), accounts);
    assertThat(repo.lockByEmail("none@b.c")).isEmpty();
    Map<String, Object> row = new HashMap<>();
    row.put("id", id);
    row.put("password_changed_at", null);
    db.rows("FROM identity.users WHERE lower(email)", row);
    db.rows("SELECT clock_timestamp()", Map.of("t", Timestamp.from(now)));
    assertThat(repo.lockByEmail("a@b.c").orElseThrow().passwordChangedAt()).isNull();
  }

  @Test
  void passwordResetUpdatesPasswordThenRevokesSessions() {
    new JdbcPasswordResetRepository(db.provider(), accounts)
        .updatePasswordAndRevokeSessions(id, "newhash");
    assertThat(db.calls()).hasSize(2);
    assertThat(db.calls().get(0).values()).containsExactly("newhash", id);
    assertThat(db.calls().get(1).sql()).contains("refresh_tokens SET revoked_at");
  }

  @Test
  void passwordChangeKeepsCurrentFamily() {
    UUID family = UUID.randomUUID();
    var repo = new JdbcPasswordChangeRepository(db.provider(), accounts);
    assertThat(repo.lockById(id)).isEmpty();
    when(accounts.findById(id)).thenReturn(Optional.of(account));
    db.rows("SELECT email FROM identity.users", Map.of("email", "a@b.c"));
    var locked = repo.lockById(id).orElseThrow();
    assertThat(locked.email()).isEqualTo("a@b.c");
    repo.updatePasswordAndRevokeOtherSessions(id, "hash", family);
    assertThat(db.last().sql()).contains("family_id <> ?");
    assertThat(db.last().values()).containsExactly(id, family);
  }

  @Test
  void restorationLocksAccountWithNullableTimestamps() {
    var repo = new JdbcAccountRestorationRepository(db.provider());
    assertThat(repo.lockById(id)).isEmpty();
    Map<String, Object> row = new HashMap<>();
    row.put("id", id);
    row.put("status", "DEACTIVATED");
    row.put("deactivation_source", "SELF");
    row.put("deactivated_at", Timestamp.from(now));
    row.put("anonymized_at", null);
    db.rows("FROM identity.users WHERE id = ? FOR UPDATE", row);
    db.rows("SELECT clock_timestamp()", Map.of("t", Timestamp.from(now.plusSeconds(5))));
    var found = repo.lockById(id).orElseThrow();
    assertThat(found.status()).isEqualTo(UserStatus.DEACTIVATED);
    assertThat(found.deactivatedAt()).isEqualTo(now);
    assertThat(found.anonymizedAt()).isNull();
    assertThat(found.databaseTime()).isEqualTo(now.plusSeconds(5));
  }

  @Test
  void restoreRequiresExactlyOneRowChanged() {
    var repo = new JdbcAccountRestorationRepository(db.provider());
    var grant = new RestorationGrant(id, now, now.plusSeconds(600));
    assertThat(repo.restore(grant)).isTrue();
    assertThat(db.last().values())
        .contains(id, Timestamp.from(now), Timestamp.from(now.plusSeconds(600)));
    db.updateCount("SET status = 'ACTIVE', deactivation_source = NULL", 0);
    assertThat(repo.restore(grant)).isFalse();
  }

  @Test
  void outboxLockPendingAttemptedAndCleanup() {
    var repo = new JdbcIdentityOutboxRepository(db.provider());
    db.rows(
        "FOR UPDATE SKIP LOCKED",
        Map.of(
            "id",
            id,
            "event_type",
            "ClassDeleted",
            "routing_key",
            "rk",
            "envelope",
            "{}",
            "publish_attempts",
            2));
    var pending = repo.lockPending(10);
    assertThat(pending).singleElement().extracting("attempts").isEqualTo(2);
    assertThat(db.last().values()).containsExactly(10);
    repo.attempted(id, true);
    assertThat(db.last().values()).containsExactly(true, id);
    repo.attempted(id, false);
    assertThat(db.last().values()).containsExactly(false, id);
    db.updateCount("DELETE FROM identity.outbox_events", 4);
    assertThat(repo.deletePublishedOlderThanDays(7, 100)).isEqualTo(4);
  }

  @Test
  void registrationCreateLockAndActivate() {
    when(accounts.findById(id)).thenReturn(Optional.of(account));
    var repo = new JdbcRegistrationRepository(db.provider(), accounts);
    var reg = new RegistrationAccount(id, "a@b.c", "h", "A");
    assertThat(repo.create(reg)).isTrue();
    assertThat(db.last().values()).containsExactly(id, "a@b.c", "h", "A");
    db.updateCount("ON CONFLICT (lower(email))", 0);
    assertThat(repo.create(reg)).isFalse();
    assertThat(repo.lockByEmail("a@b.c")).isEmpty();
    db.rows("SELECT id FROM identity.users WHERE lower(email)", Map.of("id", id));
    assertThat(repo.lockByEmail("a@b.c")).contains(account);
    repo.activate(id);
    assertThat(db.last().sql()).contains("must_change_password = false");
  }

  @Test
  void classActivityBindsOptionalClassFilter() {
    var repo = new JdbcClassActivityReadRepository(db.provider());
    assertThat(repo.recent(id, null, now, 5)).isEmpty();
    assertThat(db.last().values()).contains(id, 5);
    db.generic("FROM identity.identity_recent_activity a", 2);
    assertThat(repo.recent(id, id, now, 5)).hasSize(2);
  }

  @Test
  void auditLogAdapterInsertsWithClockAndNullableJson() {
    UUID generated = UUID.randomUUID();
    var adapter = new JdbcAuditLogAdapter(db.provider(), () -> generated, () -> now);
    adapter.record(new AuditEntry(id, "LOGIN", "USER", null, null, "{\"a\":1}"));
    assertThat(db.last().sql()).contains("INSERT INTO identity.system_audit_logs");
    assertThat(db.last().values())
        .contains(generated, id, "LOGIN", "USER", "{\"a\":1}", Timestamp.from(now));
    assertThat(db.last().values()).containsNull();
  }

  @Test
  void submissionClassScopeMapsMissingEnrollment() {
    var repo = new JdbcSubmissionClassScopeRepository(db.provider());
    Map<String, Object> noStudent = new HashMap<>();
    noStudent.put("id", id);
    noStudent.put("joined_at", null);
    noStudent.put("left_at", null);
    noStudent.put("student_id", null);
    db.rows("FROM identity.classes c", noStudent);
    var scope = repo.read(id).get(0);
    assertThat(scope.studentId()).isNull();
    assertThat(scope.joinedAt()).isNull();
    Map<String, Object> full = new HashMap<>();
    full.put("joined_at", OffsetDateTime.parse("2026-01-01T00:00:00Z"));
    full.put("left_at", OffsetDateTime.parse("2026-02-01T00:00:00Z"));
    db.rows("FROM identity.classes c", full);
    assertThat(repo.read(id).get(0).leftAt()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
  }

  @Test
  void userLookupsShortCircuitOnEmptyInput() {
    var cats = new JdbcUserBaseCategoriesRepository(db.provider());
    var names = new JdbcUserDisplayNamesRepository(db.provider());
    assertThat(cats.findBaseCategories(Set.of())).isEmpty();
    assertThat(names.findDisplayNames(Set.of())).isEmpty();
    assertThat(db.calls()).isEmpty();
  }

  @Test
  void userLookupsMapRows() {
    var cats = new JdbcUserBaseCategoriesRepository(db.provider());
    var names = new JdbcUserDisplayNamesRepository(db.provider());
    db.rows("SELECT u.id, r.base_category", Map.of("id", id, "base_category", "INSTRUCTOR"));
    db.rows("AS safe_name", Map.of("id", id, "safe_name", "An"));
    assertThat(cats.findBaseCategories(Set.of(id))).containsEntry(id, BaseCategory.INSTRUCTOR);
    assertThat(names.findDisplayNames(Set.of(id))).containsEntry(id, "An");
  }

  @Test
  void adminBootstrapAndProcessedEvents() {
    var boot = new JdbcAdminBootstrapRepository(db.provider());
    assertThat(db.rows("SELECT EXISTS", Map.of("exists", true)).calls()).isEmpty();
    assertThat(boot.adminExists()).isTrue();
    assertThat(boot.createAdmin(id, "a@b.c", "h", "Admin")).isTrue();
    db.updateCount("code = 'ADMIN'", 0);
    assertThat(boot.createAdmin(id, "a@b.c", "h", "Admin")).isFalse();
    var processed = new JdbcProcessedEventRepository(db.provider());
    assertThat(processed.markProcessed(id, "T")).isTrue();
    db.updateCount("processed_domain_events", 0);
    assertThat(processed.markProcessed(id, "T")).isFalse();
  }
}

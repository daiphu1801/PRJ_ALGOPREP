package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.model.account.MyProfile;
import com.algoprep.identity.domain.model.account.MySettings;
import com.algoprep.identity.domain.model.account.ProfileFields;
import com.algoprep.identity.domain.model.account.RestorableAccount;
import com.algoprep.identity.domain.model.account.RestorationGrant;
import com.algoprep.identity.domain.model.password.PasswordChangeAccount;
import com.algoprep.identity.domain.model.password.PasswordResetAccount;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/**
 * Account lookup, restoration window, password change and reset, profile and settings persistence
 * on PostgreSQL. Row locks are proved with {@code FOR UPDATE NOWAIT} from a second connection.
 */
class JdbcAccountLifecyclePgTest extends PostgresTestBase {
  private final JdbcUserAccountRepository accounts = new JdbcUserAccountRepository(provider());
  private final JdbcAccountRestorationRepository restoration =
      new JdbcAccountRestorationRepository(provider());
  private final JdbcPasswordChangeRepository change =
      new JdbcPasswordChangeRepository(provider(), accounts);
  private final JdbcPasswordResetRepository reset =
      new JdbcPasswordResetRepository(provider(), accounts);
  private final JdbcMyProfileRepository profile = new JdbcMyProfileRepository(provider());
  private final JdbcMySettingsRepository settings = new JdbcMySettingsRepository(provider());

  // ---- helpers -------------------------------------------------------------------------------

  private void setPassword(UUID id, String hash) {
    jdbc.sql("UPDATE identity.users SET password_hash = :h WHERE id = :id")
        .param("h", hash)
        .param("id", id)
        .update();
  }

  private UUID token(UUID userId, UUID family, String hash) {
    var id = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO identity.refresh_tokens(id,user_id,token_hash,family_id,expires_at)"
                + " VALUES(:id,:u,:h,:f, now() + interval '7 days')")
        .param("id", id)
        .param("u", userId)
        .param("h", hash)
        .param("f", family)
        .update();
    return id;
  }

  private boolean revoked(UUID tokenId) {
    return jdbc.sql("SELECT revoked_at IS NOT NULL FROM identity.refresh_tokens WHERE id = :id")
        .param("id", tokenId)
        .query(Boolean.class)
        .single();
  }

  private UUID deactivated(String email, String source, Instant at, boolean anonymized) {
    UUID id = user(email, "STUDENT", "DEACTIVATED");
    jdbc.sql(
            "UPDATE identity.users SET deactivation_source = :s, deactivated_at = :at,"
                + " anonymized_at = CASE WHEN :anon THEN now() END WHERE id = :id")
        .param("s", source)
        .param("at", utc(at))
        .param("anon", anonymized)
        .param("id", id)
        .update();
    return id;
  }

  private String userStatus(UUID id) {
    return jdbc.sql("SELECT status FROM identity.users WHERE id = :id")
        .param("id", id)
        .query(String.class)
        .single();
  }

  private void mustChange(UUID id) {
    jdbc.sql("UPDATE identity.users SET must_change_password = true WHERE id = :id")
        .param("id", id)
        .update();
  }

  /**
   * Runs {@code lock} inside a transaction and, while it is open, tries to lock the same user row
   * from another connection with NOWAIT. Returns the SQLSTATE seen by the other connection while
   * the lock is held (index 0) and after the transaction committed (index 1, null when free).
   */
  private String[] lockStates(UUID userId, Runnable lock) {
    String[] states = new String[2];
    tx.executeWithoutResult(
        s -> {
          lock.run();
          states[0] = CompletableFuture.supplyAsync(() -> nowait(userId)).join();
        });
    states[1] = nowait(userId);
    return states;
  }

  private <T> java.util.Optional<T> inTx(java.util.function.Supplier<java.util.Optional<T>> body) {
    return tx.execute(s -> body.get());
  }

  private String nowait(UUID userId) {
    try (Connection c = data.getConnection();
        Statement st = c.createStatement()) {
      st.execute("SELECT 1 FROM identity.users WHERE id = '" + userId + "' FOR UPDATE NOWAIT");
      return null;
    } catch (SQLException e) {
      return e.getSQLState();
    }
  }

  // ---- user account --------------------------------------------------------------------------

  @Test
  void findsAccountByEmailIgnoringCaseAndByIdWithRoleCategory() {
    UUID id = user("Mixed.Case@x.io", "INSTRUCTOR");
    setPassword(id, "hash");

    UserAccount byEmail = accounts.findByEmail("mixed.CASE@X.io").orElseThrow();
    assertThat(byEmail.id()).isEqualTo(id);
    assertThat(byEmail.passwordHash()).isEqualTo("hash");
    assertThat(byEmail.status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(byEmail.mustChangePassword()).isFalse();
    assertThat(byEmail.roleCode()).isEqualTo("INSTRUCTOR");
    assertThat(byEmail.baseCategory()).isEqualTo(BaseCategory.INSTRUCTOR);
    assertThat(accounts.findById(id)).contains(byEmail);
    assertThat(accounts.findByEmail("nobody@x.io")).isEmpty();
    assertThat(accounts.findById(UUID.randomUUID())).isEmpty();
  }

  @Test
  void markActiveOnlyPromotesPendingVerification() {
    UUID pending = user("p@x.io", "STUDENT", "PENDING_VERIFICATION");
    UUID deactivated = user("d@x.io", "STUDENT", "DEACTIVATED");

    accounts.markActive(pending);
    accounts.markActive(deactivated);
    accounts.markActive(UUID.randomUUID());

    assertThat(userStatus(pending)).isEqualTo("ACTIVE");
    assertThat(userStatus(deactivated)).isEqualTo("DEACTIVATED");
  }

  @Test
  void touchLastActiveWritesOnceThenThrottlesWithinAMinute() {
    UUID id = user("t@x.io", "STUDENT");
    Instant first = Instant.parse("2026-03-01T10:00:00Z");

    accounts.touchLastActive(id, first);
    assertThat(lastActive(id)).isEqualTo(utc(first));
    accounts.touchLastActive(id, first.plusSeconds(30));
    assertThat(lastActive(id)).isEqualTo(utc(first));
    accounts.touchLastActive(id, first.plusSeconds(61));
    assertThat(lastActive(id)).isEqualTo(utc(first.plusSeconds(61)));
    accounts.touchLastActive(id, first);
    assertThat(lastActive(id)).isEqualTo(utc(first.plusSeconds(61)));
  }

  private OffsetDateTime lastActive(UUID id) {
    return jdbc.sql("SELECT last_active_at FROM identity.users WHERE id = :id")
        .param("id", id)
        .query(OffsetDateTime.class)
        .single()
        .withOffsetSameInstant(ZoneOffset.UTC);
  }

  // ---- restoration ---------------------------------------------------------------------------

  @Test
  void lockByIdReadsLifecycleColumnsAndDatabaseClock() {
    Instant at = Instant.now().minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
    UUID id = deactivated("r@x.io", "SELF", at, false);

    RestorableAccount found = tx.execute(s -> restoration.lockById(id).orElseThrow());

    assertThat(found.status()).isEqualTo(UserStatus.DEACTIVATED);
    assertThat(found.deactivationSource()).isEqualTo("SELF");
    assertThat(found.deactivatedAt()).isEqualTo(at);
    assertThat(found.anonymizedAt()).isNull();
    assertThat(found.databaseTime()).isBetween(Instant.now().minusSeconds(60), Instant.now());
    assertThat(found.canRestore()).isTrue();
    assertThat(inTx(() -> restoration.lockById(UUID.randomUUID()))).isEmpty();
  }

  @Test
  void lockByIdHoldsTheRowLockUntilCommit() {
    UUID id = deactivated("lock@x.io", "SELF", Instant.now().minusSeconds(60), false);

    String[] states = lockStates(id, () -> restoration.lockById(id));

    assertThat(states[0]).isEqualTo("55P03");
    assertThat(states[1]).isNull();
  }

  @Test
  void restoreReactivatesSelfDeactivatedAccountInsideTheWindow() {
    Instant at = Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
    UUID id = deactivated("in@x.io", "SELF", at, false);

    boolean restored =
        restoration.restore(new RestorationGrant(id, at, Instant.now().plusSeconds(600)));

    assertThat(restored).isTrue();
    var row =
        jdbc.sql(
                "SELECT status || '|' || coalesce(deactivation_source, 'null') || '|' ||"
                    + " coalesce(deactivated_at::text, 'null') FROM identity.users WHERE id = :id")
            .param("id", id)
            .query(String.class)
            .single();
    assertThat(row).isEqualTo("ACTIVE|null|null");
    // The row is no longer deactivated, so a second restore with the same grant does nothing.
    assertThat(restoration.restore(new RestorationGrant(id, at, Instant.now().plusSeconds(600))))
        .isFalse();
  }

  @Test
  void restoreRefusesEveryAccountOutsideTheRules() {
    Instant recent = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
    Instant old = Instant.now().minus(31, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
    Instant future = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
    Instant expires = Instant.now().plusSeconds(600);
    UUID admin = deactivated("a@x.io", "ADMIN", recent, false);
    UUID anonymized = deactivated("an@x.io", "SELF", recent, true);
    UUID expired = deactivated("e@x.io", "SELF", old, false);
    UUID later = deactivated("f@x.io", "SELF", future, false);
    UUID active = user("act@x.io", "STUDENT");
    UUID mismatch = deactivated("m@x.io", "SELF", recent, false);
    UUID expiredGrant = deactivated("g@x.io", "SELF", recent, false);

    assertThat(restoration.restore(new RestorationGrant(admin, recent, expires))).isFalse();
    assertThat(restoration.restore(new RestorationGrant(anonymized, recent, expires))).isFalse();
    assertThat(restoration.restore(new RestorationGrant(expired, old, expires))).isFalse();
    assertThat(restoration.restore(new RestorationGrant(later, future, expires))).isFalse();
    assertThat(restoration.restore(new RestorationGrant(active, recent, expires))).isFalse();
    assertThat(restoration.restore(new RestorationGrant(mismatch, old, expires))).isFalse();
    assertThat(
            restoration.restore(
                new RestorationGrant(expiredGrant, recent, Instant.now().minusSeconds(5))))
        .isFalse();
    assertThat(restoration.restore(new RestorationGrant(UUID.randomUUID(), recent, expires)))
        .isFalse();

    for (UUID id : new UUID[] {admin, anonymized, expired, later, mismatch, expiredGrant}) {
      assertThat(userStatus(id)).isEqualTo("DEACTIVATED");
    }
    assertThat(userStatus(active)).isEqualTo("ACTIVE");
  }

  // ---- password change -----------------------------------------------------------------------

  @Test
  void passwordChangeLocksTheRowAndReturnsAccountWithEmail() {
    UUID id = user("pc@x.io", "STUDENT");
    setPassword(id, "old");

    PasswordChangeAccount found = tx.execute(s -> change.lockById(id).orElseThrow());

    assertThat(found.email()).isEqualTo("pc@x.io");
    assertThat(found.user().id()).isEqualTo(id);
    assertThat(found.user().passwordHash()).isEqualTo("old");
    assertThat(inTx(() -> change.lockById(UUID.randomUUID()))).isEmpty();
    String[] states = lockStates(id, () -> change.lockById(id));
    assertThat(states[0]).isEqualTo("55P03");
    assertThat(states[1]).isNull();
  }

  @Test
  void passwordChangeStoresHashAndRevokesOnlyOtherLiveFamilies() {
    UUID id = user("pc2@x.io", "STUDENT");
    mustChange(id);
    UUID keep = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    UUID kept = token(id, keep, "h1");
    UUID revokedOther = token(id, other, "h2");
    UUID alreadyRevoked = token(id, other, "h3");
    jdbc.sql("UPDATE identity.refresh_tokens SET revoked_at = now() - interval '1 day' WHERE id=:i")
        .param("i", alreadyRevoked)
        .update();
    UUID stranger = user("other@x.io", "STUDENT");
    UUID strangerToken = token(stranger, UUID.randomUUID(), "h4");
    OffsetDateTime before = revokedAt(alreadyRevoked);

    change.updatePasswordAndRevokeOtherSessions(id, "new-hash", keep);

    var row =
        jdbc.sql(
                "SELECT password_hash || '|' || must_change_password || '|' ||"
                    + " (password_changed_at IS NOT NULL) FROM identity.users WHERE id = :id")
            .param("id", id)
            .query(String.class)
            .single();
    assertThat(row).isEqualTo("new-hash|false|true");
    assertThat(revoked(kept)).isFalse();
    assertThat(revoked(revokedOther)).isTrue();
    assertThat(revoked(strangerToken)).isFalse();
    assertThat(revokedAt(alreadyRevoked)).isEqualTo(before);
  }

  private OffsetDateTime revokedAt(UUID tokenId) {
    return jdbc.sql("SELECT revoked_at FROM identity.refresh_tokens WHERE id=:i")
        .param("i", tokenId)
        .query(OffsetDateTime.class)
        .single();
  }

  // ---- password reset ------------------------------------------------------------------------

  @Test
  void resetLocksByEmailIgnoringCaseAndReportsDatabaseTime() {
    UUID id = user("Reset@x.io", "STUDENT");
    setPassword(id, "old");

    PasswordResetAccount found = tx.execute(s -> reset.lockByEmail("RESET@x.IO").orElseThrow());

    assertThat(found.user().id()).isEqualTo(id);
    assertThat(found.email()).isEqualTo("Reset@x.io");
    assertThat(found.passwordChangedAt()).isNull();
    assertThat(found.databaseTime()).isBetween(Instant.now().minusSeconds(60), Instant.now());
    assertThat(found.canResetPassword()).isTrue();
    assertThat(inTx(() -> reset.lockByEmail("nobody@x.io"))).isEmpty();
    String[] states = lockStates(id, () -> reset.lockByEmail("reset@x.io"));
    assertThat(states[0]).isEqualTo("55P03");
    assertThat(states[1]).isNull();
  }

  @Test
  void resetTokenIsSingleUseBecausePasswordChangedAtMovesForward() {
    UUID id = user("once@x.io", "STUDENT");
    setPassword(id, "old");
    UUID live = token(id, UUID.randomUUID(), "t1");
    UUID alreadyOut = token(id, UUID.randomUUID(), "t2");
    jdbc.sql("UPDATE identity.refresh_tokens SET revoked_at = now() WHERE id=:i")
        .param("i", alreadyOut)
        .update();
    PasswordResetAccount first = tx.execute(s -> reset.lockByEmail("once@x.io").orElseThrow());
    assertThat(first.passwordChangedAt()).isNull();

    tx.executeWithoutResult(s -> reset.updatePasswordAndRevokeSessions(id, "fresh"));

    PasswordResetAccount second = tx.execute(s -> reset.lockByEmail("once@x.io").orElseThrow());
    assertThat(second.passwordChangedAt()).isNotNull();
    assertThat(second.passwordChangedAt()).isAfterOrEqualTo(first.databaseTime());
    assertThat(second.passwordChangedAt()).isBeforeOrEqualTo(second.databaseTime());
    assertThat(second.user().passwordHash()).isEqualTo("fresh");
    assertThat(revoked(live)).isTrue();
    assertThat(revoked(alreadyOut)).isTrue();
  }

  @Test
  void resetActivatesInvitedAccountButNotAPlainPendingOne() {
    UUID invited = user("inv@x.io", "STUDENT", "PENDING_VERIFICATION");
    setPassword(invited, "temp");
    mustChange(invited);
    UUID unverified = user("unv@x.io", "STUDENT", "PENDING_VERIFICATION");
    setPassword(unverified, "temp");
    user("np@x.io", "STUDENT");

    reset.updatePasswordAndRevokeSessions(invited, "n1");
    reset.updatePasswordAndRevokeSessions(unverified, "n2");

    assertThat(userStatus(invited)).isEqualTo("ACTIVE");
    assertThat(userStatus(unverified)).isEqualTo("PENDING_VERIFICATION");
    assertThat(tx.execute(s -> reset.lockByEmail("unv@x.io").orElseThrow()).canResetPassword())
        .isFalse();
    assertThat(tx.execute(s -> reset.lockByEmail("np@x.io").orElseThrow()).canResetPassword())
        .isFalse();
  }

  // ---- profile -------------------------------------------------------------------------------

  @Test
  void profileReadsAccountRoleAndOrderedProviders() {
    UUID id = user("prof@x.io", "INSTRUCTOR");
    setPassword(id, "h");
    link(id, "GOOGLE", "g1", "2026-01-02T00:00:00Z");
    link(id, "GITHUB", "h1", "2026-01-01T00:00:00Z");

    MyProfile p = profile.findById(id).orElseThrow();

    assertThat(p.userId()).isEqualTo(id);
    assertThat(p.displayName()).isEqualTo("Name of prof@x.io");
    assertThat(p.email()).isEqualTo("prof@x.io");
    assertThat(p.roleName()).isEqualTo("Giảng viên");
    assertThat(p.defaultLanguage()).isEqualTo("PYTHON");
    assertThat(p.hasPassword()).isTrue();
    assertThat(p.passwordChangedAt()).isNull();
    assertThat(p.mustChangePassword()).isFalse();
    assertThat(p.schoolOrCompany()).isNull();
    assertThat(p.linkedProviders()).containsExactly("GITHUB", "GOOGLE");
    assertThat(p.joinedAt()).isBeforeOrEqualTo(Instant.now());
    assertThat(profile.findById(UUID.randomUUID())).isEmpty();
  }

  @Test
  void profileWithoutPasswordOrProvidersReportsEmptyState() {
    UUID id = user("bare@x.io", "STUDENT");

    MyProfile p = profile.findById(id).orElseThrow();

    assertThat(p.hasPassword()).isFalse();
    assertThat(p.linkedProviders()).isEmpty();
  }

  private void link(UUID userId, String provider, String providerUser, String at) {
    jdbc.sql(
            "INSERT INTO identity.oauth_identities(id,user_id,provider,provider_user_id,linked_at)"
                + " VALUES(:id,:u,:p,:pu,CAST(:at AS timestamptz))")
        .param("id", UUID.randomUUID())
        .param("u", userId)
        .param("p", provider)
        .param("pu", providerUser)
        .param("at", at)
        .update();
  }

  @Test
  void updateFieldsOverwritesAllFiveColumnsIncludingNulls() {
    UUID id = user("uf@x.io", "STUDENT");

    profile.updateFields(id, new ProfileFields("New Name", "HUCE", "Intern", "Backend", "0901"));
    MyProfile full = profile.findById(id).orElseThrow();
    assertThat(full.displayName()).isEqualTo("New Name");
    assertThat(full.schoolOrCompany()).isEqualTo("HUCE");
    assertThat(full.currentPosition()).isEqualTo("Intern");
    assertThat(full.targetPosition()).isEqualTo("Backend");
    assertThat(full.phone()).isEqualTo("0901");

    profile.updateFields(id, new ProfileFields("Only Name", null, null, null, null));
    MyProfile cleared = profile.findById(id).orElseThrow();
    assertThat(cleared.displayName()).isEqualTo("Only Name");
    assertThat(cleared.schoolOrCompany()).isNull();
    assertThat(cleared.phone()).isNull();
  }

  @Test
  void updateFieldsRejectsValuesLongerThanTheColumns() {
    UUID id = user("long@x.io", "STUDENT");
    ProfileFields tooLong = new ProfileFields("n", null, null, null, "0".repeat(21));

    assertThatThrownBy(() -> profile.updateFields(id, tooLong))
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("22001"));
    assertThat(profile.findById(id).orElseThrow().phone()).isNull();
  }

  @Test
  void emailTakenIgnoresCaseAndUpdateEmailReportsDuplicates() {
    UUID a = user("a@x.io", "STUDENT");
    user("b@x.io", "STUDENT");

    assertThat(profile.emailTaken("A@X.IO")).isTrue();
    assertThat(profile.emailTaken("free@x.io")).isFalse();
    assertThat(profile.updateEmail(a, "B@x.io")).isFalse();
    assertThat(profile.updateEmail(a, "c@x.io")).isTrue();
    assertThat(profile.findById(a).orElseThrow().email()).isEqualTo("c@x.io");
    assertThat(profile.emailTaken("a@x.io")).isFalse();
    // Re-saving the same address with other casing is not a duplicate of itself.
    assertThat(profile.updateEmail(a, "C@x.io")).isTrue();
  }

  // ---- settings ------------------------------------------------------------------------------

  @Test
  void settingsFallBackToColumnDefaultsWithoutAPreferencesRow() {
    UUID id = user("s@x.io", "STUDENT");

    MySettings s = settings.find(id).orElseThrow();

    assertThat(s).isEqualTo(new MySettings(true, false, "MIDDLE", 12, true, "PYTHON"));
    assertThat(count("user_preferences")).isZero();
    assertThat(settings.find(UUID.randomUUID())).isEmpty();
    assertThat(inTx(() -> settings.findForUpdate(UUID.randomUUID()))).isEmpty();
  }

  @Test
  void saveUpsertsPreferencesAndUpdatesLanguageOnTheUser() {
    UUID id = user("sv@x.io", "STUDENT");

    settings.save(id, new MySettings(false, true, "SENIOR", 16, false, "JAVA"));
    assertThat(count("user_preferences")).isOne();
    assertThat(settings.find(id).orElseThrow())
        .isEqualTo(new MySettings(false, true, "SENIOR", 16, false, "JAVA"));
    assertThat(
            jdbc.sql("SELECT default_language FROM identity.users WHERE id = :id")
                .param("id", id)
                .query(String.class)
                .single())
        .isEqualTo("JAVA");

    settings.save(id, new MySettings(true, false, "INTERN", 8, true, "CPP"));
    assertThat(count("user_preferences")).isOne();
    assertThat(settings.find(id).orElseThrow())
        .isEqualTo(new MySettings(true, false, "INTERN", 8, true, "CPP"));
  }

  @Test
  void saveRejectsValuesOutsideTheCheckConstraintsAndLeavesStateUntouched() {
    UUID id = user("bad@x.io", "STUDENT");
    settings.save(id, new MySettings(true, true, "JUNIOR", 8, true, "JAVA"));
    MySettings badTurns = new MySettings(true, true, "JUNIOR", 10, true, "C");
    MySettings badLevel = new MySettings(true, true, "EXPERT", 12, true, "C");
    MySettings fine = new MySettings(true, true, "JUNIOR", 8, true, "C");
    UUID stranger = UUID.randomUUID();

    assertThatThrownBy(() -> settings.save(id, badTurns))
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23514"));
    assertThatThrownBy(() -> settings.save(id, badLevel))
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23514"));
    assertThatThrownBy(() -> settings.save(stranger, fine))
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23503"));

    assertThat(settings.find(id).orElseThrow())
        .isEqualTo(new MySettings(true, true, "JUNIOR", 8, true, "JAVA"));
    assertThat(count("user_preferences")).isOne();
  }

  @Test
  void findForUpdateLocksTheUserRowUntilCommit() {
    UUID id = user("fu@x.io", "STUDENT");

    String[] states = lockStates(id, () -> settings.findForUpdate(id));

    assertThat(states[0]).isEqualTo("55P03");
    assertThat(states[1]).isNull();
    assertThat(inTx(() -> settings.findForUpdate(id))).isPresent();
  }
}

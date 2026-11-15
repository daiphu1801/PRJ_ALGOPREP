package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.identity.domain.model.admin.AdminUserItem;
import com.algoprep.identity.domain.model.auth.StoredRefreshToken;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;

/** Admin user search, admin writes, account lifecycle batches and refresh tokens on PostgreSQL. */
class JdbcUserAdminPgTest extends PostgresTestBase {
  private final JdbcAdminUserRepository admin = new JdbcAdminUserRepository(provider());
  private final JdbcAccountLifecycleRepository lifecycle =
      new JdbcAccountLifecycleRepository(provider());
  private final JdbcRefreshTokenRepository tokens = new JdbcRefreshTokenRepository(provider());

  private String status(UUID id) {
    return jdbc.sql("SELECT status FROM identity.users WHERE id = :id")
        .param("id", id)
        .query(String.class)
        .single();
  }

  private UUID named(String email, String displayName) {
    UUID id = user(email, "STUDENT");
    jdbc.sql("UPDATE identity.users SET display_name = :n WHERE id = :id")
        .param("n", displayName)
        .param("id", id)
        .update();
    return id;
  }

  private void token(UUID user, String hash, UUID family, Instant issued, Instant expires) {
    tokens.insert(
        new StoredRefreshToken(UUID.randomUUID(), user, family, expires, null), hash, issued);
  }

  private long revokedCount(UUID user) {
    return jdbc.sql(
            "SELECT count(*) FROM identity.refresh_tokens WHERE user_id = :u"
                + " AND revoked_at IS NOT NULL")
        .param("u", user)
        .query(Long.class)
        .single();
  }

  // ---- admin user search -------------------------------------------------------------------

  @Test
  void searchEscapesLikeWildcardsAndBackslash() {
    named("a@x.io", "100% Sure");
    named("b@x.io", "100 Sure");
    named("c@x.io", "snake_case");
    named("d@x.io", "snakeXcase");
    named("e@x.io", "back\\slash");

    assertThat(admin.search(null, null, "100%", 1, 10).items())
        .extracting(AdminUserItem::displayName)
        .containsExactly("100% Sure");
    assertThat(admin.search(null, null, "e_c", 1, 10).items())
        .extracting(AdminUserItem::displayName)
        .containsExactly("snake_case");
    assertThat(admin.search(null, null, "k\\s", 1, 10).items())
        .extracting(AdminUserItem::displayName)
        .containsExactly("back\\slash");
    assertThat(admin.search(null, null, "SNAKE", 1, 10).total()).isEqualTo(2);
    assertThat(admin.search(null, null, "D@X.IO", 1, 10).total()).isEqualTo(1);
  }

  @Test
  void searchFiltersByStatusAndCategoryAndPages() {
    for (int i = 0; i < 5; i++) {
      UUID id = user("s" + i + "@x.io", "STUDENT");
      jdbc.sql(
              "UPDATE identity.users SET created_at = now() - make_interval(mins => :m) WHERE id = :id")
          .param("m", i)
          .param("id", id)
          .update();
    }
    user("t@x.io", "INSTRUCTOR");
    user("gone@x.io", "STUDENT", "PENDING_VERIFICATION");

    var page1 = admin.search("ACTIVE", "STUDENT", null, 1, 2);
    assertThat(page1.total()).isEqualTo(5);
    assertThat(page1.items())
        .extracting(AdminUserItem::email)
        .containsExactly("s0@x.io", "s1@x.io");
    var page3 = admin.search("ACTIVE", "STUDENT", null, 3, 2);
    assertThat(page3.items()).extracting(AdminUserItem::email).containsExactly("s4@x.io");
    assertThat(admin.search("PENDING_VERIFICATION", null, null, 1, 10).total()).isEqualTo(1);
    assertThat(admin.search(null, "INSTRUCTOR", null, 1, 10).total()).isEqualTo(1);
    assertThat(admin.search("ACTIVE", "ADMIN", null, 1, 10).items()).isEmpty();
  }

  @Test
  void findCarriesSolvedCountTotalsAndFlagsAndCountsAreGrouped() {
    UUID student = user("s@x.io", "STUDENT");
    jdbc.sql(
            "INSERT INTO identity.user_problem_best_score(user_id,problem_id,best_ratio,best_verdict)"
                + " VALUES (:u,:p1,1,'ACCEPTED'),(:u,:p2,0.5,'WRONG_ANSWER')")
        .param("u", student)
        .param("p1", UUID.randomUUID())
        .param("p2", UUID.randomUUID())
        .update();
    jdbc.sql("INSERT INTO identity.user_submission_stats(user_id,total_submissions) VALUES (:u,7)")
        .param("u", student)
        .update();
    var item = admin.find(student).orElseThrow();
    assertThat(item.solvedCount()).isEqualTo(1);
    assertThat(item.totalSubmissions()).isEqualTo(7);
    assertThat(item.hasPassword()).isFalse();
    assertThat(admin.find(UUID.randomUUID())).isEmpty();

    user("t@x.io", "INSTRUCTOR");
    user("d@x.io", "STUDENT", "DEACTIVATED");
    user("p@x.io", "STUDENT", "PENDING_VERIFICATION");
    var counts = admin.groupCounts();
    assertThat(counts)
        .containsEntry("STUDENT", 2L)
        .containsEntry("INSTRUCTOR", 1L)
        .containsEntry("ADMIN", 0L)
        .containsEntry("DEACTIVATED", 1L);
    assertThat(admin.countPendingVerification()).isEqualTo(1);
  }

  @Test
  void insertCreatedByAdminLosesToExistingEmailIgnoringCase() {
    UUID role = roleId("STUDENT");
    UUID first = UUID.randomUUID();
    assertThat(admin.insertCreatedByAdmin(first, "Mixed@X.io", "hash", "First", role)).isTrue();
    assertThat(admin.insertCreatedByAdmin(UUID.randomUUID(), "mixed@x.IO", "h2", "Second", role))
        .isFalse();
    assertThat(count("users")).isEqualTo(1);
    var row =
        jdbc.sql("SELECT status, must_change_password, display_name FROM identity.users")
            .query((rs, n) -> rs.getString(1) + "|" + rs.getBoolean(2) + "|" + rs.getString(3))
            .single();
    assertThat(row).isEqualTo("PENDING_VERIFICATION|true|First");
  }

  @Test
  void changeRoleToUnknownRoleViolatesForeignKey() {
    UUID student = user("s@x.io", "STUDENT");
    UUID unknown = UUID.randomUUID();
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> admin.changeRole(student, unknown))
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23503"));
    admin.changeRole(student, roleId("INSTRUCTOR"));
    assertThat(admin.find(student).orElseThrow().baseCategory()).isEqualTo("INSTRUCTOR");
  }

  @Test
  void deactivateReactivateTemporaryPasswordAndRevokeSessionsWriteTheirColumns() {
    UUID id = user("s@x.io", "STUDENT");
    token(id, "h1", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(600));
    token(id, "h2", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(600));

    admin.deactivateByAdmin(id);
    var row =
        jdbc.sql("SELECT status, deactivation_source, deactivated_at FROM identity.users")
            .query(
                (rs, n) ->
                    rs.getString(1) + "|" + rs.getString(2) + "|" + (rs.getObject(3) != null))
            .single();
    assertThat(row).isEqualTo("DEACTIVATED|ADMIN|true");

    admin.revokeAllSessions(id);
    assertThat(revokedCount(id)).isEqualTo(2);

    admin.reactivate(id);
    var back =
        jdbc.sql("SELECT status, deactivation_source, deactivated_at FROM identity.users")
            .query((rs, n) -> rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getObject(3))
            .single();
    assertThat(back).isEqualTo("ACTIVE|null|null");

    admin.setTemporaryPassword(id, "tmp-hash");
    var pw =
        jdbc.sql(
                "SELECT password_hash, must_change_password, password_changed_at IS NOT NULL FROM identity.users")
            .query((rs, n) -> rs.getString(1) + "|" + rs.getBoolean(2) + "|" + rs.getBoolean(3))
            .single();
    assertThat(pw).isEqualTo("tmp-hash|true|true");
  }

  @Test
  void deactivatingAnActiveUserWithSelfSourceIsRejectedByCheckConstraint() {
    UUID id = user("s@x.io", "STUDENT");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                jdbc.sql("UPDATE identity.users SET deactivation_source = 'SELF' WHERE id = :id")
                    .param("id", id)
                    .update())
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23514"));
  }

  @Test
  void lockSelectsRowForUpdateOfUsers() {
    UUID id = user("s@x.io", "STUDENT");
    tx.executeWithoutResult(
        s -> {
          assertThat(admin.lock(id)).isPresent();
          assertThat(admin.lock(UUID.randomUUID())).isEmpty();
          assertThat(lockedByAnotherConnection(id)).isTrue();
        });
    assertThat(lockedByAnotherConnection(id)).isFalse();
  }

  private boolean lockedByAnotherConnection(UUID id) {
    try (var other = data.getConnection();
        var st =
            other.prepareStatement("SELECT 1 FROM identity.users WHERE id = ? FOR UPDATE NOWAIT")) {
      other.setAutoCommit(false);
      st.setObject(1, id);
      st.execute();
      return false;
    } catch (java.sql.SQLException e) {
      return "55P03".equals(e.getSQLState());
    }
  }

  // ---- last admin protection -----------------------------------------------------------------

  @Test
  void lastActiveAdminProtectsSoleAdminAndSoleMatrixEditor() {
    UUID only = user("a1@x.io", "ADMIN");
    assertThat(lifecycle.isLastActiveAdmin(only)).isTrue();
    assertThat(lifecycle.isLastActiveAdmin(user("s@x.io", "STUDENT"))).isFalse();

    // A second active admin of the same (matrix-editing) role: neither is the last any more.
    UUID second = user("a2@x.io", "ADMIN");
    assertThat(lifecycle.isLastActiveAdmin(only)).isFalse();
    assertThat(lifecycle.isLastActiveAdmin(second)).isFalse();

    // A deactivated admin does not count as a spare.
    admin.deactivateByAdmin(second);
    assertThat(lifecycle.isLastActiveAdmin(only)).isTrue();
    assertThat(lifecycle.isLastActiveAdmin(second)).isFalse();
  }

  @Test
  void lastMatrixEditorIsProtectedEvenWithOtherAdmins() {
    UUID editor = user("editor@x.io", "ADMIN");
    UUID limitedRole = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO identity.roles(id,code,name,base_category) VALUES (:id,'OPS','Ops','ADMIN')")
        .param("id", limitedRole)
        .update();
    UUID ops = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO identity.users(id,email,display_name,role_id,status) VALUES (:id,'ops@x.io','Ops',:r,'ACTIVE')")
        .param("id", ops)
        .param("r", limitedRole)
        .update();
    // OPS has no PERMISSION_MATRIX/UPDATE cell, so removing the ADMIN user would strand the matrix.
    assertThat(lifecycle.isLastActiveAdmin(editor)).isTrue();
    assertThat(lifecycle.isLastActiveAdmin(ops)).isFalse();
  }

  // ---- self deactivation and batches ---------------------------------------------------------

  @Test
  void deactivateSelfSetsSourceRevokesTokensAndIsOnlyOnce() {
    UUID id = user("s@x.io", "STUDENT");
    token(id, "h1", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(600));
    assertThat(lifecycle.deactivateSelf(id)).isTrue();
    assertThat(status(id)).isEqualTo("DEACTIVATED");
    assertThat(
            jdbc.sql("SELECT deactivation_source FROM identity.users WHERE id=:id")
                .param("id", id)
                .query(String.class)
                .single())
        .isEqualTo("SELF");
    assertThat(revokedCount(id)).isEqualTo(1);
    assertThat(lifecycle.deactivateSelf(id)).isFalse();
    assertThat(lifecycle.deactivateSelf(UUID.randomUUID())).isFalse();
  }

  @Test
  void anonymizeDueBatchScrubsOnlyDueSelfDeletedAccountsAndUnlinksOAuth() {
    UUID due = user("due@x.io", "STUDENT");
    UUID recent = user("recent@x.io", "STUDENT");
    UUID admins = user("byadmin@x.io", "STUDENT");
    for (UUID id : new UUID[] {due, recent, admins}) {
      lifecycle.deactivateSelf(id);
    }
    jdbc.sql("UPDATE identity.users SET deactivated_at = now() - interval '31 days' WHERE id = :id")
        .param("id", due)
        .update();
    jdbc.sql(
            "UPDATE identity.users SET deactivated_at = now() - interval '31 days',"
                + " deactivation_source = 'ADMIN' WHERE id = :id")
        .param("id", admins)
        .update();
    jdbc.sql(
            "INSERT INTO identity.oauth_identities(id,user_id,provider,provider_user_id)"
                + " VALUES (:i,:u,'GITHUB','gh-1')")
        .param("i", UUID.randomUUID())
        .param("u", due)
        .update();

    assertThat(lifecycle.anonymizeDueBatch(10)).isEqualTo(1);

    var row =
        jdbc.sql(
                "SELECT email, display_name, password_hash, anonymized_at IS NOT NULL FROM identity.users WHERE id=:id")
            .param("id", due)
            .query(
                (rs, n) ->
                    rs.getString(1)
                        + "|"
                        + rs.getString(2)
                        + "|"
                        + rs.getString(3)
                        + "|"
                        + rs.getBoolean(4))
            .single();
    assertThat(row).isEqualTo("deleted-user-" + due + "@anon.local|Người dùng đã xoá|null|true");
    assertThat(count("oauth_identities")).isZero();
    assertThat(
            jdbc.sql("SELECT count(*) FROM identity.users WHERE anonymized_at IS NOT NULL")
                .query(Integer.class)
                .single())
        .isEqualTo(1);
    // Idempotent: a second run finds nothing due.
    assertThat(lifecycle.anonymizeDueBatch(10)).isZero();
  }

  @Test
  void anonymizeDueBatchHonoursLimit() {
    for (int i = 0; i < 3; i++) {
      UUID id = user("u" + i + "@x.io", "STUDENT");
      lifecycle.deactivateSelf(id);
      jdbc.sql(
              "UPDATE identity.users SET deactivated_at = now() - make_interval(days => :d) WHERE id = :id")
          .param("d", 40 + i)
          .param("id", id)
          .update();
    }
    assertThat(lifecycle.anonymizeDueBatch(2)).isEqualTo(2);
    assertThat(lifecycle.anonymizeDueBatch(2)).isEqualTo(1);
  }

  @Test
  void deleteStalePendingBatchRemovesOnlyUntouchedStalePendingAccountsAndChildren() {
    UUID stale = user("stale@x.io", "STUDENT", "PENDING_VERIFICATION");
    UUID fresh = user("fresh@x.io", "STUDENT", "PENDING_VERIFICATION");
    UUID withLog = user("log@x.io", "ADMIN", "PENDING_VERIFICATION");
    UUID forced = user("forced@x.io", "STUDENT", "PENDING_VERIFICATION");
    UUID active = user("active@x.io", "STUDENT");
    for (UUID id : new UUID[] {stale, withLog, forced, active}) {
      jdbc.sql("UPDATE identity.users SET created_at = now() - interval '3 days' WHERE id = :id")
          .param("id", id)
          .update();
    }
    jdbc.sql("UPDATE identity.users SET must_change_password = true WHERE id = :id")
        .param("id", forced)
        .update();
    jdbc.sql(
            "INSERT INTO identity.system_audit_logs(id,actor_user_id,action_type,target_type)"
                + " VALUES (:i,:u,'USER_CREATED','USER')")
        .param("i", UUID.randomUUID())
        .param("u", withLog)
        .update();
    jdbc.sql("INSERT INTO identity.user_preferences(user_id) VALUES (:u)")
        .param("u", stale)
        .update();
    jdbc.sql(
            "INSERT INTO identity.oauth_identities(id,user_id,provider,provider_user_id)"
                + " VALUES (:i,:u,'GOOGLE','g-1')")
        .param("i", UUID.randomUUID())
        .param("u", stale)
        .update();
    token(stale, "stale-h", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(60));

    assertThat(lifecycle.deleteStalePendingBatch(Duration.ofDays(1), 10)).isEqualTo(1);

    assertThat(
            jdbc.sql("SELECT email FROM identity.users ORDER BY email").query(String.class).list())
        .containsExactly("active@x.io", "forced@x.io", "fresh@x.io", "log@x.io");
    assertThat(count("user_preferences")).isZero();
    assertThat(count("oauth_identities")).isZero();
    assertThat(count("refresh_tokens")).isZero();
    assertThat(fresh).isNotNull();
  }

  // ---- refresh tokens ------------------------------------------------------------------------

  @Test
  void duplicateTokenHashViolatesUniqueIndex() {
    UUID id = user("s@x.io", "STUDENT");
    token(id, "same", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(60));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                token(id, "same", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(60)))
        .isInstanceOf(DuplicateKeyException.class)
        .satisfies(e -> assertThat(sqlState((DataAccessException) e)).isEqualTo("23505"));
    assertThat(count("refresh_tokens")).isEqualTo(1);
  }

  @Test
  void refreshTokenRotationRevokesOldAndFamilyReuseRevokesAllLiveSiblings() {
    UUID id = user("s@x.io", "STUDENT");
    UUID family = UUID.randomUUID();
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    token(id, "old", family, now, now.plusSeconds(600));
    var old = tokens.findByHashForUpdate("old").orElseThrow();
    assertThat(old.familyId()).isEqualTo(family);
    assertThat(old.isRevoked()).isFalse();
    assertThat(tokens.findByHashForUpdate("missing")).isEmpty();

    tokens.revoke(old.id(), now);
    token(id, "new", family, now, now.plusSeconds(600));
    assertThat(tokens.findByHashForUpdate("old").orElseThrow().revokedAt()).isEqualTo(now);

    // Reuse of the rotated token: the whole family is revoked, other families are untouched.
    token(id, "other", UUID.randomUUID(), now, now.plusSeconds(600));
    tokens.revokeFamily(family, now.plusSeconds(5));
    assertThat(tokens.findByHashForUpdate("new").orElseThrow().revokedAt())
        .isEqualTo(now.plusSeconds(5));
    // The already revoked token keeps its original revocation time.
    assertThat(tokens.findByHashForUpdate("old").orElseThrow().revokedAt()).isEqualTo(now);
    assertThat(tokens.findByHashForUpdate("other").orElseThrow().revokedAt()).isNull();
  }

  @Test
  void deleteExpiredBatchRemovesOnlyOldAndDeadTokensWithinLimit() {
    UUID id = user("s@x.io", "STUDENT");
    Instant old = Instant.now().minus(200, ChronoUnit.DAYS);
    Instant recent = Instant.now().minus(1, ChronoUnit.DAYS);
    token(id, "old-expired-1", UUID.randomUUID(), old, old.plusSeconds(60));
    token(id, "old-expired-2", UUID.randomUUID(), old.plusSeconds(1), old.plusSeconds(60));
    token(id, "old-live", UUID.randomUUID(), old, Instant.now().plus(1, ChronoUnit.DAYS));
    token(id, "recent-expired", UUID.randomUUID(), recent, recent.plusSeconds(60));
    token(id, "old-revoked", UUID.randomUUID(), old, Instant.now().plus(1, ChronoUnit.DAYS));
    tokens.revoke(tokens.findByHashForUpdate("old-revoked").orElseThrow().id(), Instant.now());

    assertThat(tokens.deleteExpiredBatch(1)).isEqualTo(1);
    assertThat(count("refresh_tokens")).isEqualTo(4);
    assertThat(tokens.deleteExpiredBatch(10)).isEqualTo(2);
    assertThat(
            jdbc.sql("SELECT token_hash FROM identity.refresh_tokens ORDER BY token_hash")
                .query(String.class)
                .list())
        .containsExactly("old-live", "recent-expired");
  }
}

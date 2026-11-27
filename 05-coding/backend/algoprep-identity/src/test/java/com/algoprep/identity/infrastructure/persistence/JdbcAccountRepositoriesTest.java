package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.model.account.MySettings;
import com.algoprep.identity.domain.model.account.ProfileFields;
import com.algoprep.identity.domain.model.audit.ExpiredLogPolicy;
import com.algoprep.identity.domain.model.audit.LogRetentionSettings;
import com.algoprep.identity.domain.model.auth.OAuthProvider;
import com.algoprep.identity.domain.model.auth.StoredRefreshToken;
import com.algoprep.identity.domain.model.progress.ActivityEntry;
import com.algoprep.identity.domain.model.progress.GradedSubmission;
import com.algoprep.identity.domain.model.progress.SolveState;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F1 account-side persistence: profile, settings, lifecycle, tokens, OAuth links, stats. */
class JdbcAccountRepositoriesTest {
  private final FakeDb db = new FakeDb();
  private final UUID id = UUID.randomUUID();
  private final UUID fixed = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-05-01T00:00:00Z");

  @Test
  void progressCountersFallBackToZerosWhenNoStatsRow() {
    JdbcMyProgressReadRepository repo = new JdbcMyProgressReadRepository(db.provider());
    db.rows("SELECT count(*) FROM identity.user_problem_best_score", Map.of("count", 4));
    var c = repo.counters(id);
    assertThat(c.solvedProblemCount()).isEqualTo(4);
    assertThat(c.totalSubmissions()).isZero();
    assertThat(c.topLanguage()).isNull();
  }

  @Test
  void progressCountersReadStatsRow() {
    JdbcMyProgressReadRepository repo = new JdbcMyProgressReadRepository(db.provider());
    db.rows("SELECT count(*) FROM identity.user_problem_best_score", Map.of("count", 1));
    db.rows(
        "FROM identity.user_submission_stats",
        Map.of("total_submissions", 10, "accepted_count", 5, "top_language", "JAVA"));
    var c = repo.counters(id);
    assertThat(c.totalSubmissions()).isEqualTo(10);
    assertThat(c.acceptedCount()).isEqualTo(5);
    assertThat(c.topLanguage()).isEqualTo("JAVA");
  }

  @Test
  void progressActivityDailyCountsAndSolveStates() {
    JdbcMyProgressReadRepository repo = new JdbcMyProgressReadRepository(db.provider());
    UUID p1 = UUID.randomUUID();
    UUID p2 = UUID.randomUUID();
    db.generic("FROM identity.identity_recent_activity", 2);
    db.rows(
        "FROM identity.user_daily_submission_stats",
        Map.of("day", LocalDate.parse("2026-04-30"), "submission_count", 3));
    db.rows(
        "SELECT problem_id, best_verdict",
        Map.of("problem_id", p1, "best_verdict", "ACCEPTED"),
        Map.of("problem_id", p2, "best_verdict", "WRONG_ANSWER"));
    assertThat(repo.recentActivity(id, now, 5)).hasSize(2);
    assertThat(db.last().values()).contains(id, 5);
    assertThat(repo.dailyCounts(id, LocalDate.parse("2026-04-01"), LocalDate.parse("2026-04-30")))
        .containsEntry(LocalDate.parse("2026-04-30"), 3);
    assertThat(repo.solveStates(id, List.of(p1, p2)))
        .containsEntry(p1, SolveState.SOLVED)
        .containsEntry(p2, SolveState.IN_PROGRESS);
    assertThat(repo.allSolveStates(id))
        .containsEntry(p1, SolveState.SOLVED)
        .containsEntry(p2, SolveState.IN_PROGRESS);
  }

  @Test
  void profileJoinsLinkedProvidersAndMapsNullPasswordChange() {
    JdbcMyProfileRepository repo = new JdbcMyProfileRepository(db.provider());
    Map<String, Object> row = new HashMap<>();
    row.put("display_name", "An");
    row.put("password_changed_at", null);
    row.put("has_password", true);
    db.rows("FROM identity.users u JOIN identity.roles r", row);
    db.rows("FROM identity.oauth_identities", Map.of("provider", "GITHUB"));
    var profile = repo.findById(id).orElseThrow();
    assertThat(profile.displayName()).isEqualTo("An");
    assertThat(profile.passwordChangedAt()).isNull();
    assertThat(profile.hasPassword()).isTrue();
    assertThat(profile.linkedProviders()).containsExactly("GITHUB");
  }

  @Test
  void profileMissingAndUpdates() {
    JdbcMyProfileRepository repo = new JdbcMyProfileRepository(db.provider());
    assertThat(repo.findById(id)).isEmpty();
    repo.updateFields(id, new ProfileFields("N", "S", "C", "T", "P"));
    assertThat(db.last().values()).contains("N", "S", "C", "T", "P", id);
    db.rows("SELECT EXISTS", Map.of("exists", true));
    assertThat(repo.emailTaken("a@b.c")).isTrue();
    assertThat(repo.updateEmail(id, "a@b.c")).isTrue();
    db.fail("UPDATE identity.users SET email", "23505");
    assertThat(repo.updateEmail(id, "a@b.c")).isFalse();
  }

  @Test
  void settingsFindLockAndSave() {
    JdbcMySettingsRepository repo = new JdbcMySettingsRepository(db.provider());
    assertThat(repo.find(id)).isEmpty();
    assertThat(repo.findForUpdate(id)).isEmpty();
    db.rows("FOR UPDATE", Map.of("one", 1));
    db.rows(
        "LEFT JOIN identity.user_preferences",
        Map.of("level", "SENIOR", "turns", 8, "streak", true));
    var s = repo.findForUpdate(id).orElseThrow();
    assertThat(s.preferredInterviewLevel()).isEqualTo("SENIOR");
    assertThat(s.preferredMaxTurns()).isEqualTo(8);
    int before = db.calls().size();
    repo.save(id, new MySettings(true, false, "JUNIOR", 6, true, "vi"));
    assertThat(db.calls()).hasSize(before + 2);
    assertThat(db.last().sql()).contains("default_language");
    assertThat(db.last().values()).contains("vi", id);
  }

  @Test
  void lastAdminIsFalseWhenCallerIsNotAnActiveAdmin() {
    JdbcAccountLifecycleRepository repo = new JdbcAccountLifecycleRepository(db.provider());
    db.rows("SELECT EXISTS", Map.of("exists", false));
    assertThat(repo.isLastActiveAdmin(id)).isFalse();
    assertThat(db.calls()).hasSize(1);
  }

  @Test
  void lastAdminTrueWhenOnlyActiveAdmin() {
    JdbcAccountLifecycleRepository repo = new JdbcAccountLifecycleRepository(db.provider());
    db.rows("SELECT EXISTS", Map.of("exists", true));
    db.rows("FOR UPDATE OF u", Map.of("id", id, "editor", true));
    assertThat(repo.isLastActiveAdmin(id)).isTrue();
  }

  @Test
  void lastAdminTrueWhenOnlyMatrixEditorAmongSeveralAdmins() {
    JdbcAccountLifecycleRepository repo = new JdbcAccountLifecycleRepository(db.provider());
    db.rows("SELECT EXISTS", Map.of("exists", true));
    db.rows(
        "FOR UPDATE OF u",
        Map.of("id", id, "editor", true),
        Map.of("id", UUID.randomUUID(), "editor", false));
    assertThat(repo.isLastActiveAdmin(id)).isTrue();
  }

  @Test
  void lastAdminFalseWhenAnotherEditorRemains() {
    JdbcAccountLifecycleRepository repo = new JdbcAccountLifecycleRepository(db.provider());
    db.rows("SELECT EXISTS", Map.of("exists", true));
    db.rows(
        "FOR UPDATE OF u",
        Map.of("id", id, "editor", true),
        Map.of("id", UUID.randomUUID(), "editor", true));
    assertThat(repo.isLastActiveAdmin(id)).isFalse();
  }

  @Test
  void deactivateSelfRevokesTokensOnlyWhenRowChanged() {
    JdbcAccountLifecycleRepository repo = new JdbcAccountLifecycleRepository(db.provider());
    assertThat(repo.deactivateSelf(id)).isTrue();
    assertThat(db.calls()).hasSize(2);
    assertThat(db.last().sql()).contains("refresh_tokens SET revoked_at");
    FakeDb none = new FakeDb().updateCount("deactivation_source = 'SELF'", 0);
    assertThat(new JdbcAccountLifecycleRepository(none.provider()).deactivateSelf(id)).isFalse();
    assertThat(none.calls()).hasSize(1);
  }

  @Test
  void batchJobsBindTtlAndLimit() {
    JdbcAccountLifecycleRepository repo = new JdbcAccountLifecycleRepository(db.provider());
    db.rows("SELECT count(*) FROM gone", Map.of("count", 3));
    assertThat(repo.deleteStalePendingBatch(Duration.ofHours(1), 50)).isEqualTo(3);
    assertThat(db.last().values()).contains(3600L, 50);
    db.rows("SELECT count(*) FROM anonymized", Map.of("count", 2));
    assertThat(repo.anonymizeDueBatch(10)).isEqualTo(2);
  }

  @Test
  void refreshTokenRoundTrip() {
    JdbcRefreshTokenRepository repo = new JdbcRefreshTokenRepository(db.provider());
    var token = new StoredRefreshToken(id, id, fixed, now.plusSeconds(60), null);
    repo.insert(token, "hash", now);
    assertThat(db.last().values()).contains("hash", Timestamp.from(now), id, fixed);
    assertThat(repo.findByHashForUpdate("hash")).isEmpty();
    Map<String, Object> revoked = new HashMap<>();
    revoked.put("expires_at", Timestamp.from(now));
    revoked.put("revoked_at", Timestamp.from(now));
    db.rows("WHERE token_hash", revoked);
    assertThat(repo.findByHashForUpdate("hash").orElseThrow().revokedAt()).isEqualTo(now);
    repo.revoke(id, now);
    assertThat(db.last().sql()).contains("WHERE id = ?");
    repo.revokeFamily(fixed, now);
    assertThat(db.last().sql()).contains("family_id = ?");
    db.updateCount("DELETE FROM identity.refresh_tokens", 7);
    assertThat(repo.deleteExpiredBatch(100)).isEqualTo(7);
  }

  @Test
  void refreshTokenWithoutRevocationMapsNull() {
    JdbcRefreshTokenRepository repo = new JdbcRefreshTokenRepository(db.provider());
    Map<String, Object> live = new HashMap<>();
    live.put("revoked_at", null);
    db.rows("WHERE token_hash", live);
    assertThat(repo.findByHashForUpdate("h").orElseThrow().revokedAt()).isNull();
  }

  @Test
  void userAccountLookupsMapEnums() {
    JdbcUserAccountRepository repo = new JdbcUserAccountRepository(db.provider());
    db.rows(
        "JOIN identity.roles r ON r.id = u.role_id",
        Map.of("id", id, "status", "ACTIVE", "base_category", "ADMIN", "role_code", "ADMIN"));
    var byEmail = repo.findByEmail("A@B.C").orElseThrow();
    assertThat(byEmail.status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(byEmail.baseCategory()).isEqualTo(BaseCategory.ADMIN);
    assertThat(repo.findById(id)).isPresent();
    repo.markActive(id);
    assertThat(db.last().sql()).contains("status = 'PENDING_VERIFICATION'");
    repo.touchLastActive(id, now);
    assertThat(db.last().values()).contains(Timestamp.from(now), id);
  }

  @Test
  void userAccountMissingIsEmpty() {
    assertThat(new JdbcUserAccountRepository(db.provider()).findById(id)).isEmpty();
  }

  @Test
  void oauthLookupsCreateLinkAndClaim() {
    JdbcOAuthAccountRepository repo = new JdbcOAuthAccountRepository(db.provider(), () -> fixed);
    assertThat(repo.findLinkedUser(OAuthProvider.GITHUB, "g1")).isEmpty();
    assertThat(db.last().values()).contains("GITHUB", "g1");
    db.rows("lower(email) = lower(?) FOR UPDATE", Map.of("id", id));
    assertThat(repo.lockUserByEmail("a@b.c")).contains(id);
    assertThat(repo.createStudent("a@b.c", "A")).contains(fixed);
    db.updateCount("ON CONFLICT (lower(email))", 0);
    assertThat(repo.createStudent("a@b.c", "A")).isEmpty();
    assertThat(repo.link(id, OAuthProvider.GOOGLE, "p", "a@b.c")).isTrue();
    db.updateCount("ON CONFLICT (provider, provider_user_id)", 0);
    assertThat(repo.link(id, OAuthProvider.GOOGLE, "p", "a@b.c")).isFalse();
    repo.claimPendingAccount(id);
    assertThat(db.last().sql()).contains("must_change_password = false");
  }

  @Test
  void submissionStatsCountEligibilityAndFirstTry() {
    JdbcSubmissionStatsRepository repo = new JdbcSubmissionStatsRepository(db.provider());
    repo.keepBestScore(id, id, new BigDecimal("0.5"), "WRONG_ANSWER");
    assertThat(db.last().values()).contains(new BigDecimal("0.5"), "WRONG_ANSWER");
    repo.addToTotals(
        new GradedSubmission(id, id, id, "ACCEPTED", BigDecimal.ONE, "JAVA", now, true));
    assertThat(db.calls().get(1).values()).contains(1, 1, 1, "JAVA");
    repo.addToTotals(
        new GradedSubmission(id, id, id, "SYSTEM_ERROR", BigDecimal.ZERO, "CPP", now, true));
    assertThat(db.calls().get(3).values()).contains(0, 0, 0, "CPP");
    repo.addToDay(id, LocalDate.parse("2026-05-01"));
    assertThat(db.last().sql()).contains("user_daily_submission_stats");
  }

  @Test
  void recentActivityAddAndConditionalAdd() {
    JdbcRecentActivityRepository repo = new JdbcRecentActivityRepository(db.provider());
    var entry = new ActivityEntry(id, id, fixed, "SUBMITTED", "PROBLEM", id, now);
    repo.add(entry);
    assertThat(db.last().values()).contains(id, fixed, "SUBMITTED");
    assertThat(repo.addIfClassExists(entry)).isTrue();
    db.updateCount("FROM identity.classes c", 0);
    assertThat(repo.addIfClassExists(entry)).isFalse();
    db.rows("SELECT class_id", Map.of("class_id", fixed));
    assertThat(repo.classesEnrolledAt(id, now)).containsExactly(fixed);
  }

  @Test
  void logRetentionDefaultsAndSave() {
    JdbcLogRetentionRepository repo = new JdbcLogRetentionRepository(db.provider());
    var defaults = repo.read();
    assertThat(defaults.retentionDays()).isEqualTo(90);
    assertThat(defaults.expiredPolicy()).isEqualTo(ExpiredLogPolicy.DELETE);
    db.rows(
        "FOR UPDATE",
        Map.of("key", "audit_log.retention_days", "value", "30"),
        Map.of("key", "audit_log.expired_policy", "value", "ARCHIVE"));
    var stored = repo.readForUpdate();
    assertThat(stored.retentionDays()).isEqualTo(30);
    assertThat(stored.expiredPolicy()).isEqualTo(ExpiredLogPolicy.ARCHIVE);
    repo.save(new LogRetentionSettings(45, ExpiredLogPolicy.DELETE), id);
    assertThat(db.last().values()).contains("audit_log.expired_policy", "\"DELETE\"", id);
    db.rows("SELECT count(*) FROM gone", Map.of("count", 12));
    assertThat(repo.purgeExpiredBatch(90, 500)).isEqualTo(12);
  }

  @Test
  void adminDashboardCountsAndSeries() {
    JdbcAdminDashboardRepository repo = new JdbcAdminDashboardRepository(db.provider());
    db.rows("SELECT count(DISTINCT t.user_id)", Map.of("count", 8L));
    db.rows("SELECT count(*) FROM identity.system_audit_logs", Map.of("count", 2L));
    assertThat(repo.activeUsers(7, 0)).isEqualTo(8);
    assertThat(db.last().values()).contains(7, 0);
    assertThat(repo.activeUsersAtDayEnd(1)).isEqualTo(8);
    assertThat(repo.passwordResets(24, 0)).isEqualTo(2);
    db.rows("to_char(m.start", Map.of("month", "2026-04", "new_users", 3L, "returning_users", 1L));
    assertThat(repo.userRetention(6)).singleElement().extracting("newUsers").isEqualTo(3L);
    db.rows("AS total", Map.of("total", 4L), Map.of("total", 0L));
    assertThat(repo.passwordResetsPerDay(2)).containsExactly(4L, 0L);
  }
}

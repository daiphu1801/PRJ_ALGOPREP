package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.admin.UserRetentionGroup;
import com.algoprep.identity.domain.model.classes.ClassActivityItem;
import com.algoprep.identity.domain.model.classes.SubmissionClassScope;
import com.algoprep.identity.domain.model.progress.ActivityEntry;
import com.algoprep.identity.domain.model.progress.GradedSubmission;
import com.algoprep.identity.domain.model.progress.ProgressCounters;
import com.algoprep.identity.domain.model.progress.SolveState;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/**
 * Read models, submission statistics, activity feeds, class scope, lookups and the admin dashboard
 * queries on PostgreSQL.
 */
class JdbcReadModelsPgTest extends PostgresTestBase {
  private static final ZoneId HCM = ZoneId.of("Asia/Ho_Chi_Minh");

  private final JdbcSubmissionStatsRepository stats = new JdbcSubmissionStatsRepository(provider());
  private final JdbcMyProgressReadRepository progress =
      new JdbcMyProgressReadRepository(provider());
  private final JdbcRecentActivityRepository recent = new JdbcRecentActivityRepository(provider());
  private final JdbcClassActivityReadRepository classFeed =
      new JdbcClassActivityReadRepository(provider());
  private final JdbcSubmissionClassScopeRepository scope =
      new JdbcSubmissionClassScopeRepository(provider());
  private final JdbcUserBaseCategoriesRepository categories =
      new JdbcUserBaseCategoriesRepository(provider());
  private final JdbcUserDisplayNamesRepository names =
      new JdbcUserDisplayNamesRepository(provider());
  private final JdbcAdminDashboardRepository dashboard =
      new JdbcAdminDashboardRepository(provider());

  private static GradedSubmission graded(
      UUID user, String verdict, String language, boolean firstTry) {
    return new GradedSubmission(
        UUID.randomUUID(),
        user,
        UUID.randomUUID(),
        verdict,
        BigDecimal.ONE,
        language,
        Instant.now(),
        firstTry);
  }

  private static ActivityEntry entry(UUID actor, UUID classId, String type, Instant at) {
    return new ActivityEntry(UUID.randomUUID(), actor, classId, type, "SUBMISSION", null, at);
  }

  private UUID enroll(UUID classId, UUID student, String joined, String left) {
    var id = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO identity.class_enrollments(id,class_id,student_id,joined_at,left_at)"
                + " VALUES(:id,:c,:s,CAST(:j AS timestamptz),CAST(:l AS timestamptz))")
        .param("id", id)
        .param("c", classId)
        .param("s", student)
        .param("j", joined)
        .param("l", left)
        .update();
    return id;
  }

  // ---- submission statistics -----------------------------------------------------------------

  @Test
  void keepBestScoreInsertsThenOnlyOverwritesWithAStrictlyHigherRatio() {
    UUID u = UUID.randomUUID();
    UUID p = UUID.randomUUID();

    stats.keepBestScore(u, p, new BigDecimal("0.5000"), "WRONG_ANSWER");
    assertThat(best(u, p)).isEqualTo("0.5000|WRONG_ANSWER");
    stats.keepBestScore(u, p, new BigDecimal("0.5000"), "TIME_LIMIT");
    assertThat(best(u, p)).isEqualTo("0.5000|WRONG_ANSWER");
    stats.keepBestScore(u, p, new BigDecimal("0.2500"), "WRONG_ANSWER");
    assertThat(best(u, p)).isEqualTo("0.5000|WRONG_ANSWER");
    stats.keepBestScore(u, p, BigDecimal.ONE, "ACCEPTED");
    assertThat(best(u, p)).isEqualTo("1.0000|ACCEPTED");
    stats.keepBestScore(u, p, new BigDecimal("0.9000"), "WRONG_ANSWER");
    assertThat(best(u, p)).isEqualTo("1.0000|ACCEPTED");
    assertThat(count("user_problem_best_score")).isOne();
  }

  @Test
  void keepBestScoreRejectsRatioOutsideZeroToOne() {
    UUID u = UUID.randomUUID();
    UUID p = UUID.randomUUID();
    BigDecimal tooBig = new BigDecimal("1.5");

    assertThatThrownBy(() -> stats.keepBestScore(u, p, tooBig, "ACCEPTED"))
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23514"));
    assertThat(count("user_problem_best_score")).isZero();
  }

  private String best(UUID u, UUID p) {
    return jdbc.sql(
            "SELECT best_ratio || '|' || best_verdict FROM identity.user_problem_best_score"
                + " WHERE user_id=:u AND problem_id=:p")
        .param("u", u)
        .param("p", p)
        .query(String.class)
        .single();
  }

  private String totals(UUID u) {
    return jdbc.sql(
            "SELECT total_submissions || '|' || ac_eligible_count || '|' || accepted_count"
                + " || '|' || first_try_accepted_count || '|' || coalesce(top_language,'null')"
                + " FROM identity.user_submission_stats WHERE user_id=:u")
        .param("u", u)
        .query(String.class)
        .single();
  }

  @Test
  void addToTotalsCreatesTheRowThenIncrementsEveryCounterOnConflict() {
    UUID u = UUID.randomUUID();

    stats.addToTotals(graded(u, "ACCEPTED", "PYTHON", true));
    assertThat(totals(u)).isEqualTo("1|1|1|1|PYTHON");
    stats.addToTotals(graded(u, "ACCEPTED", "PYTHON", false));
    assertThat(totals(u)).isEqualTo("2|2|2|1|PYTHON");
    stats.addToTotals(graded(u, "WRONG_ANSWER", "PYTHON", true));
    assertThat(totals(u)).isEqualTo("3|3|2|1|PYTHON");
    stats.addToTotals(graded(u, "SYSTEM_ERROR", "PYTHON", true));
    assertThat(totals(u)).isEqualTo("4|3|2|1|PYTHON");
    assertThat(count("user_submission_stats")).isOne();
    assertThat(
            jdbc.sql("SELECT language_counts ->> 'PYTHON' FROM identity.user_submission_stats")
                .query(String.class)
                .single())
        .isEqualTo("4");
  }

  @Test
  void topLanguageKeepsTheCurrentLeaderOnATieAndSwitchesWhenOvertaken() {
    UUID u = UUID.randomUUID();

    stats.addToTotals(graded(u, "WRONG_ANSWER", "PYTHON", false));
    stats.addToTotals(graded(u, "WRONG_ANSWER", "JAVA", false));
    assertThat(totals(u)).endsWith("|PYTHON");
    stats.addToTotals(graded(u, "WRONG_ANSWER", "JAVA", false));
    assertThat(totals(u)).endsWith("|JAVA");
    stats.addToTotals(graded(u, "WRONG_ANSWER", "PYTHON", false));
    assertThat(totals(u)).endsWith("|JAVA");
    stats.addToTotals(graded(u, "WRONG_ANSWER", "CPP", false));
    stats.addToTotals(graded(u, "WRONG_ANSWER", "CPP", false));
    // Three languages tied at two: the one already on top stays.
    assertThat(totals(u)).endsWith("|JAVA");
    stats.addToTotals(graded(u, "WRONG_ANSWER", "CPP", false));
    assertThat(totals(u)).endsWith("|CPP");
    assertThat(
            jdbc.sql(
                    "SELECT language_counts::text FROM identity.user_submission_stats"
                        + " WHERE user_id=:u")
                .param("u", u)
                .query(String.class)
                .single())
        .contains("\"JAVA\": 2", "\"PYTHON\": 2", "\"CPP\": 3");
  }

  @Test
  void addToDayCountsPerUserAndDay() {
    UUID u = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    LocalDate day = LocalDate.of(2026, 3, 1);

    stats.addToDay(u, day);
    stats.addToDay(u, day);
    stats.addToDay(u, day.plusDays(1));
    stats.addToDay(other, day);

    assertThat(progress.dailyCounts(u, day, day.plusDays(1)))
        .containsExactlyInAnyOrderEntriesOf(Map.of(day, 2, day.plusDays(1), 1));
    assertThat(progress.dailyCounts(other, day, day)).isEqualTo(Map.of(day, 1));
    assertThat(count("user_daily_submission_stats")).isEqualTo(3);
  }

  // ---- progress reads ------------------------------------------------------------------------

  @Test
  void countersAreZeroForAUserWithNoRows() {
    assertThat(progress.counters(UUID.randomUUID()))
        .isEqualTo(new ProgressCounters(0, 0, 0, 0, 0, null));
  }

  @Test
  void countersJoinSolvedProblemsWithTheTotalsRow() {
    UUID u = UUID.randomUUID();
    stats.keepBestScore(u, UUID.randomUUID(), BigDecimal.ONE, "ACCEPTED");
    stats.keepBestScore(u, UUID.randomUUID(), BigDecimal.ONE, "ACCEPTED");
    stats.keepBestScore(u, UUID.randomUUID(), new BigDecimal("0.3000"), "WRONG_ANSWER");
    stats.keepBestScore(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE, "ACCEPTED");
    stats.addToTotals(graded(u, "ACCEPTED", "JAVA", true));
    stats.addToTotals(graded(u, "WRONG_ANSWER", "JAVA", false));

    assertThat(progress.counters(u)).isEqualTo(new ProgressCounters(2, 2, 2, 1, 1, "JAVA"));
  }

  @Test
  void countersWithSolvedProblemsButNoTotalsRowStillReportSolvedCount() {
    UUID u = UUID.randomUUID();
    stats.keepBestScore(u, UUID.randomUUID(), BigDecimal.ONE, "ACCEPTED");

    assertThat(progress.counters(u)).isEqualTo(new ProgressCounters(1, 0, 0, 0, 0, null));
  }

  @Test
  void solveStatesMapAcceptedToSolvedAndEverythingElseToInProgress() {
    UUID u = UUID.randomUUID();
    UUID solved = UUID.randomUUID();
    UUID tried = UUID.randomUUID();
    UUID untouched = UUID.randomUUID();
    stats.keepBestScore(u, solved, BigDecimal.ONE, "ACCEPTED");
    stats.keepBestScore(u, tried, new BigDecimal("0.5000"), "WRONG_ANSWER");
    stats.keepBestScore(UUID.randomUUID(), untouched, BigDecimal.ONE, "ACCEPTED");

    assertThat(progress.solveStates(u, List.of(solved, tried, untouched)))
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(solved, SolveState.SOLVED, tried, SolveState.IN_PROGRESS));
    assertThat(progress.solveStates(u, List.of(UUID.randomUUID()))).isEmpty();
    assertThat(progress.allSolveStates(u))
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(solved, SolveState.SOLVED, tried, SolveState.IN_PROGRESS));
    assertThat(progress.allSolveStates(UUID.randomUUID())).isEmpty();
  }

  @Test
  void recentActivityIsNewestFirstLimitedAndFilteredBySinceAndActor() {
    UUID u = user("act@x.io", "STUDENT");
    UUID other = user("oth@x.io", "STUDENT");
    Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    ActivityEntry oldest = entry(u, null, "SUBMISSION_ACCEPTED", now.minus(10, ChronoUnit.DAYS));
    ActivityEntry middle =
        entry(u, null, "SOLUTION_REVIEW_COMPLETED", now.minus(2, ChronoUnit.DAYS));
    ActivityEntry newest = entry(u, null, "MOCK_INTERVIEW_FINISHED", now.minus(1, ChronoUnit.DAYS));
    recent.add(oldest);
    recent.add(middle);
    recent.add(newest);
    recent.add(entry(other, null, "SUBMISSION_ACCEPTED", now));

    assertThat(progress.recentActivity(u, now.minus(30, ChronoUnit.DAYS), 10))
        .containsExactly(newest, middle, oldest);
    assertThat(progress.recentActivity(u, now.minus(5, ChronoUnit.DAYS), 10))
        .containsExactly(newest, middle);
    assertThat(progress.recentActivity(u, now.minus(30, ChronoUnit.DAYS), 1))
        .containsExactly(newest);
    assertThat(progress.recentActivity(UUID.randomUUID(), now.minus(30, ChronoUnit.DAYS), 10))
        .isEmpty();
  }

  // ---- recent activity writes ----------------------------------------------------------------

  @Test
  void addStoresEveryFieldAndRejectsUnknownActorOrType() {
    UUID u = user("w@x.io", "STUDENT");
    UUID teacher = user("tw@x.io", "INSTRUCTOR");
    UUID cls = classOf(teacher, "C");
    Instant at = Instant.now().truncatedTo(ChronoUnit.MICROS);
    UUID ref = UUID.randomUUID();
    ActivityEntry e =
        new ActivityEntry(UUID.randomUUID(), u, cls, "CLASS_PROBLEM_PUBLISHED", "PROBLEM", ref, at);

    recent.add(e);

    assertThat(progress.recentActivity(u, at.minusSeconds(1), 5)).containsExactly(e);
    ActivityEntry badActor = entry(UUID.randomUUID(), null, "SUBMISSION_ACCEPTED", at);
    ActivityEntry badType = entry(u, null, "NOT_A_TYPE", at);
    assertThatThrownBy(() -> recent.add(badActor))
        .isInstanceOfSatisfying(
            DataAccessException.class, x -> assertThat(sqlState(x)).isEqualTo("23503"));
    assertThatThrownBy(() -> recent.add(badType))
        .isInstanceOfSatisfying(
            DataAccessException.class, x -> assertThat(sqlState(x)).isEqualTo("23514"));
    assertThatThrownBy(() -> recent.add(e))
        .isInstanceOfSatisfying(
            DataAccessException.class, x -> assertThat(sqlState(x)).isEqualTo("23505"));
    assertThat(count("identity_recent_activity")).isOne();
  }

  @Test
  void addIfClassExistsInsertsOnlyForAKnownClass() {
    UUID u = user("ic@x.io", "STUDENT");
    UUID teacher = user("ict@x.io", "INSTRUCTOR");
    UUID cls = classOf(teacher, "C");
    Instant at = Instant.now().truncatedTo(ChronoUnit.MICROS);

    assertThat(recent.addIfClassExists(entry(u, cls, "STUDENT_STREAK_ACHIEVED", at))).isTrue();
    assertThat(recent.addIfClassExists(entry(u, UUID.randomUUID(), "STUDENT_STREAK_ACHIEVED", at)))
        .isFalse();
    assertThat(recent.addIfClassExists(entry(u, null, "STUDENT_STREAK_ACHIEVED", at))).isFalse();

    assertThat(count("identity_recent_activity")).isOne();
    assertThat(
            jdbc.sql("SELECT scope_class_id FROM identity.identity_recent_activity")
                .query(UUID.class)
                .single())
        .isEqualTo(cls);
  }

  @Test
  void classesEnrolledAtHonoursJoinAndLeaveBoundaries() {
    UUID teacher = user("en@x.io", "INSTRUCTOR");
    UUID student = user("es@x.io", "STUDENT");
    UUID open = classOf(teacher, "open");
    UUID left = classOf(teacher, "left");
    UUID future = classOf(teacher, "future");
    enroll(open, student, "2026-01-01T00:00:00Z", null);
    enroll(left, student, "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z");
    enroll(future, student, "2026-06-01T00:00:00Z", null);

    assertThat(recent.classesEnrolledAt(student, Instant.parse("2026-01-15T00:00:00Z")))
        .containsExactlyInAnyOrder(open, left);
    assertThat(recent.classesEnrolledAt(student, Instant.parse("2026-02-01T00:00:00Z")))
        .containsExactly(open);
    assertThat(recent.classesEnrolledAt(student, Instant.parse("2026-06-01T00:00:00Z")))
        .containsExactlyInAnyOrder(open, future);
    assertThat(recent.classesEnrolledAt(student, Instant.parse("2025-12-31T00:00:00Z"))).isEmpty();
    assertThat(recent.classesEnrolledAt(UUID.randomUUID(), Instant.parse("2026-01-15T00:00:00Z")))
        .isEmpty();
  }

  // ---- class activity feed -------------------------------------------------------------------

  @Test
  void classFeedShowsOnlyOwnClassesNewestFirstWithActorNames() {
    UUID teacher = user("cf@x.io", "INSTRUCTOR");
    UUID rival = user("rv@x.io", "INSTRUCTOR");
    UUID student = user("cs@x.io", "STUDENT");
    UUID mine = classOf(teacher, "mine");
    UUID mine2 = classOf(teacher, "mine2");
    UUID theirs = classOf(rival, "theirs");
    Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    recent.add(entry(student, mine, "SUBMISSION_ACCEPTED", now.minusSeconds(300)));
    recent.add(entry(student, mine2, "SOLUTION_REVIEW_COMPLETED", now.minusSeconds(100)));
    recent.add(entry(student, theirs, "SUBMISSION_ACCEPTED", now.minusSeconds(50)));
    recent.add(entry(student, null, "SUBMISSION_ACCEPTED", now.minusSeconds(40)));
    recent.add(entry(student, mine, "SUBMISSION_ACCEPTED", now.minus(40, ChronoUnit.DAYS)));

    Instant since = now.minus(7, ChronoUnit.DAYS);
    List<ClassActivityItem> all = classFeed.recent(teacher, null, since, 10);

    assertThat(all).extracting(ClassActivityItem::scopeClassId).containsExactly(mine2, mine);
    assertThat(all).extracting(ClassActivityItem::actorDisplayName).containsOnly("Name of cs@x.io");
    assertThat(all.get(0).activityType()).isEqualTo("SOLUTION_REVIEW_COMPLETED");
    assertThat(all.get(0).actorUserId()).isEqualTo(student);
    assertThat(all.get(0).occurredAt()).isEqualTo(now.minusSeconds(100));
    assertThat(classFeed.recent(teacher, mine, since, 10)).hasSize(1);
    assertThat(classFeed.recent(teacher, theirs, since, 10)).isEmpty();
    assertThat(classFeed.recent(teacher, null, since, 1)).hasSize(1);
    assertThat(classFeed.recent(teacher, null, now.plusSeconds(1), 10)).isEmpty();
    assertThat(classFeed.recent(UUID.randomUUID(), null, since, 10)).isEmpty();
  }

  // ---- submission class scope ----------------------------------------------------------------

  @Test
  void classScopeListsEachEnrollmentAndKeepsEmptyClassesWithNullStudent() {
    UUID teacher = user("sc@x.io", "INSTRUCTOR");
    UUID rival = user("sr@x.io", "INSTRUCTOR");
    UUID s1 = user("s1@x.io", "STUDENT");
    UUID s2 = user("s2@x.io", "STUDENT");
    UUID full = classOf(teacher, "full");
    UUID empty = classOf(teacher, "empty");
    UUID theirs = classOf(rival, "theirs");
    enroll(full, s1, "2026-01-01T00:00:00Z", null);
    enroll(full, s2, "2026-01-02T00:00:00Z", "2026-02-02T00:00:00Z");
    enroll(theirs, s1, "2026-01-01T00:00:00Z", null);

    List<SubmissionClassScope> rows = scope.read(teacher);

    assertThat(rows).hasSize(3);
    assertThat(rows).extracting(SubmissionClassScope::classId).containsOnly(full, empty);
    SubmissionClassScope emptyRow =
        rows.stream().filter(r -> r.classId().equals(empty)).findFirst().orElseThrow();
    assertThat(emptyRow.className()).isEqualTo("empty");
    assertThat(emptyRow.studentId()).isNull();
    assertThat(emptyRow.studentName()).isNull();
    assertThat(emptyRow.joinedAt()).isNull();
    assertThat(emptyRow.leftAt()).isNull();
    SubmissionClassScope active =
        rows.stream().filter(r -> s1.equals(r.studentId())).findFirst().orElseThrow();
    assertThat(active.studentName()).isEqualTo("Name of s1@x.io");
    assertThat(active.joinedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    assertThat(active.leftAt()).isNull();
    SubmissionClassScope gone =
        rows.stream().filter(r -> s2.equals(r.studentId())).findFirst().orElseThrow();
    assertThat(gone.leftAt()).isEqualTo(Instant.parse("2026-02-02T00:00:00Z"));
    assertThat(scope.read(UUID.randomUUID())).isEmpty();
  }

  // ---- lookups -------------------------------------------------------------------------------

  @Test
  void baseCategoriesIgnoreUnknownIdsAndShortCircuitOnEmptyInput() {
    UUID student = user("bs@x.io", "STUDENT");
    UUID teacher = user("bt@x.io", "INSTRUCTOR");
    UUID admin = user("ba@x.io", "ADMIN");

    assertThat(categories.findBaseCategories(Set.of(student, teacher, admin, UUID.randomUUID())))
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                student, BaseCategory.STUDENT,
                teacher, BaseCategory.INSTRUCTOR,
                admin, BaseCategory.ADMIN));
    assertThat(categories.findBaseCategories(Set.of(UUID.randomUUID()))).isEmpty();
    assertThat(categories.findBaseCategories(Set.of())).isEmpty();
  }

  @Test
  void displayNamesMaskAnonymisedUsersAndIgnoreUnknownIds() {
    UUID plain = user("dn@x.io", "STUDENT");
    UUID gone = user("dg@x.io", "STUDENT", "DEACTIVATED");
    jdbc.sql(
            "UPDATE identity.users SET deactivation_source='SELF', deactivated_at=now(),"
                + " anonymized_at=now() WHERE id=:id")
        .param("id", gone)
        .update();

    assertThat(names.findDisplayNames(Set.of(plain, gone, UUID.randomUUID())))
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(plain, "Name of dn@x.io", gone, "Người dùng đã xoá"));
    assertThat(names.findDisplayNames(Set.of(UUID.randomUUID()))).isEmpty();
    assertThat(names.findDisplayNames(Set.of())).isEmpty();
  }

  // ---- admin dashboard -----------------------------------------------------------------------

  private void tokenAt(UUID userId, String issuedAgo) {
    jdbc.sql(
            "INSERT INTO identity.refresh_tokens(id,user_id,token_hash,family_id,issued_at,"
                + "expires_at) VALUES(:id,:u,:h,:f, now() - CAST(:ago AS interval),"
                + " now() + interval '7 days')")
        .param("id", UUID.randomUUID())
        .param("u", userId)
        .param("h", UUID.randomUUID().toString())
        .param("f", UUID.randomUUID())
        .param("ago", issuedAgo)
        .update();
  }

  private void audit(UUID actor, String type, String ago) {
    jdbc.sql(
            "INSERT INTO identity.system_audit_logs(id,actor_user_id,action_type,target_type,"
                + "created_at) VALUES(:id,:a,:t,'USER', now() - CAST(:ago AS interval))")
        .param("id", UUID.randomUUID())
        .param("a", actor)
        .param("t", type)
        .param("ago", ago)
        .update();
  }

  @Test
  void dashboardReturnsZerosOnEmptyData() {
    assertThat(dashboard.activeUsers(7, 0)).isZero();
    assertThat(dashboard.activeUsersAtDayEnd(0)).isZero();
    assertThat(dashboard.passwordResets(24, 0)).isZero();
    assertThat(dashboard.passwordResetsPerDay(4)).containsExactly(0L, 0L, 0L, 0L);
    assertThat(dashboard.userRetention(2))
        .extracting(UserRetentionGroup::newUsers, UserRetentionGroup::returningUsers)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(0L, 0L),
            org.assertj.core.groups.Tuple.tuple(0L, 0L));
  }

  @Test
  void activeUsersCountsDistinctNonDeactivatedUsersInsideTheWindow() {
    UUID a = user("ua@x.io", "STUDENT");
    UUID b = user("ub@x.io", "STUDENT");
    UUID gone = user("ug@x.io", "STUDENT", "DEACTIVATED");
    UUID idle = user("ui@x.io", "STUDENT");
    tokenAt(a, "1 day");
    tokenAt(a, "2 days");
    tokenAt(b, "10 days");
    tokenAt(gone, "1 day");
    tokenAt(idle, "40 days");

    assertThat(dashboard.activeUsers(7, 0)).isOne();
    assertThat(dashboard.activeUsers(14, 7)).isOne();
    assertThat(dashboard.activeUsers(30, 0)).isEqualTo(2);
    assertThat(dashboard.activeUsers(60, 30)).isOne();
    assertThat(dashboard.activeUsersAtDayEnd(0)).isEqualTo(2);
    assertThat(dashboard.activeUsersAtDayEnd(15)).isOne();
    assertThat(dashboard.activeUsersAtDayEnd(60)).isZero();
  }

  @Test
  void userRetentionSplitsNewAndReturningUsersPerHoChiMinhMonth() {
    UUID fresh = user("rf@x.io", "STUDENT");
    UUID veteran = user("rvt@x.io", "STUDENT");
    UUID gone = user("rg@x.io", "STUDENT", "DEACTIVATED");
    jdbc.sql("UPDATE identity.users SET created_at = now() - interval '100 days' WHERE id=:id")
        .param("id", veteran)
        .update();
    tokenAt(fresh, "1 minute");
    tokenAt(veteran, "1 minute");
    tokenAt(gone, "1 minute");

    List<UserRetentionGroup> rows = dashboard.userRetention(3);

    YearMonth current = YearMonth.now(HCM);
    assertThat(rows)
        .extracting(UserRetentionGroup::month)
        .containsExactly(
            current.minusMonths(2).toString(),
            current.minusMonths(1).toString(),
            current.toString());
    UserRetentionGroup last = rows.get(2);
    assertThat(last.newUsers()).isOne();
    assertThat(last.returningUsers()).isOne();
    assertThat(rows.get(0).returningUsers()).isZero();
    assertThat(rows.get(1).returningUsers()).isZero();
  }

  @Test
  void passwordResetCountersUseOnlyResetRequestsInsideTheWindow() {
    UUID admin = user("pa@x.io", "ADMIN");
    audit(admin, "PASSWORD_RESET_REQUESTED", "30 minutes");
    audit(admin, "PASSWORD_RESET_REQUESTED", "2 hours");
    audit(admin, "PASSWORD_RESET_REQUESTED", "1 day");
    audit(admin, "PASSWORD_RESET_REQUESTED", "9 days");
    audit(admin, "USER_CREATED", "30 minutes");

    assertThat(dashboard.passwordResets(1, 0)).isOne();
    assertThat(dashboard.passwordResets(3, 1)).isOne();
    assertThat(dashboard.passwordResets(48, 0)).isEqualTo(3);
    assertThat(dashboard.passwordResets(240, 48)).isOne();
    List<Long> perDay = dashboard.passwordResetsPerDay(3);
    assertThat(perDay).hasSize(3);
    assertThat(perDay.get(1)).isGreaterThanOrEqualTo(1L);
    assertThat(perDay.stream().mapToLong(Long::longValue).sum()).isEqualTo(3L);
    assertThat(dashboard.passwordResetsPerDay(1)).hasSize(1);
  }
}

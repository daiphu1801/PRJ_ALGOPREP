package com.algoprep.aireview.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.outbox.AiOutboxEvent;
import com.algoprep.aireview.domain.model.usage.AlertReview;
import com.algoprep.aireview.domain.model.usage.AnomalyAlert;
import com.algoprep.aireview.domain.model.usage.DailyFeatureTokens;
import com.algoprep.aireview.domain.model.usage.ProblemUsage;
import com.algoprep.aireview.domain.model.usage.UserUsage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** Usage aggregation, anomaly alerts and the transactional outbox against real PostgreSQL. */
class UsageAndOutboxPgTest extends PostgresTestBase {
  private final JdbcAiUsageRepository usage = new JdbcAiUsageRepository(provider());
  private final JdbcAiOutboxRepository outbox = new JdbcAiOutboxRepository(provider());

  private void price(String model, String usdPerMillion) {
    jdbc.sql(
            "INSERT INTO ai.ai_model_prices(id, model_name, usd_per_million_tokens) VALUES(:i,:m,:p)")
        .param("i", UUID.randomUUID())
        .param("m", model)
        .param("p", new BigDecimal(usdPerMillion))
        .update();
  }

  private AnomalyAlert alert(UUID user, Instant createdAt) {
    return new AnomalyAlert(
        UUID.randomUUID(),
        user,
        createdAt.minusSeconds(3600),
        createdAt,
        12,
        new BigDecimal("3.50"),
        new BigDecimal("3.43"),
        createdAt);
  }

  private AiOutboxEvent event(String type, Instant at) {
    return new AiOutboxEvent(UUID.randomUUID(), type, "{\"k\": 1}", at, 0);
  }

  @Test
  void dailyTokensGroupByVietnamLocalDayNotUtcDay() {
    UUID user = UUID.randomUUID();
    UUID problem = UUID.randomUUID();
    // 16:59 UTC is 23:59 on Jan 1 in UTC+7; 17:00 UTC is already Jan 2 there.
    Instant lateJan1 = Instant.parse("2027-01-01T16:59:59Z");
    Instant earlyJan2 = Instant.parse("2027-01-01T17:00:00Z");
    usage(user, FeatureCode.SOLUTION_REVIEW, problem, new Spend(10, 5, "m", lateJan1));
    usage(user, FeatureCode.SOLUTION_REVIEW, problem, new Spend(1, 1, "m", earlyJan2));
    usage(user, FeatureCode.MOCK_INTERVIEW, null, new Spend(3, 3, "m", earlyJan2));
    usage(
        user,
        FeatureCode.MOCK_INTERVIEW,
        null,
        new Spend(99, 99, "m", Instant.parse("2027-01-03T00:00:00Z")));

    List<DailyFeatureTokens> rows =
        usage.dailyTokens(
            Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2027-01-02T00:00:00Z"));

    assertThat(rows)
        .containsExactlyInAnyOrder(
            new DailyFeatureTokens(LocalDate.parse("2027-01-01"), FeatureCode.SOLUTION_REVIEW, 15L),
            new DailyFeatureTokens(LocalDate.parse("2027-01-02"), FeatureCode.SOLUTION_REVIEW, 2L),
            new DailyFeatureTokens(LocalDate.parse("2027-01-02"), FeatureCode.MOCK_INTERVIEW, 6L));
  }

  @Test
  void topUsersAggregatePerModelPriceCaseInsensitivelyAndNullWhenAPriceIsMissing() {
    UUID heavy = UUID.randomUUID();
    UUID unpriced = UUID.randomUUID();
    UUID light = UUID.randomUUID();
    Instant at = now();
    price("Gemini", "2.0000");
    price("gpt", "10.0000");
    // Same model written with different case collapses into one group and joins its price.
    usage(heavy, FeatureCode.MOCK_INTERVIEW, null, new Spend(600_000, 0, "GEMINI", at));
    usage(heavy, FeatureCode.MOCK_INTERVIEW, null, new Spend(400_000, 0, "gemini", at));
    usage(heavy, FeatureCode.MOCK_INTERVIEW, null, new Spend(100_000, 0, "gpt", at));
    usage(unpriced, FeatureCode.MOCK_INTERVIEW, null, new Spend(500_000, 0, "gemini", at));
    usage(unpriced, FeatureCode.MOCK_INTERVIEW, null, new Spend(500_000, 0, "mystery", at));
    usage(light, FeatureCode.MOCK_INTERVIEW, null, new Spend(10, 0, "gemini", at));
    usage(
        light,
        FeatureCode.MOCK_INTERVIEW,
        null,
        new Spend(99, 0, "gemini", at.minus(40, ChronoUnit.DAYS)));

    List<UserUsage> top = usage.topUsers(at.minusSeconds(60), 2);

    assertThat(top).hasSize(2);
    assertThat(top.get(0).userId()).isEqualTo(heavy);
    assertThat(top.get(0).tokens()).isEqualTo(1_100_000L);
    assertThat(top.get(0).calls()).isEqualTo(3);
    // 1.0M * 2 + 0.1M * 10 = 3.0000
    assertThat(top.get(0).costUsd()).isEqualByComparingTo("3.0000");
    assertThat(top.get(1).userId()).isEqualTo(unpriced);
    assertThat(top.get(1).tokens()).isEqualTo(1_000_000L);
    assertThat(top.get(1).calls()).isEqualTo(2);
    assertThat(top.get(1).costUsd()).isNull();
  }

  @Test
  void topUsersBreakTokenTiesByUserId() {
    UUID low = UUID.fromString("00000000-0000-4000-8000-000000000001");
    UUID high = UUID.fromString("00000000-0000-4000-8000-000000000002");
    Instant at = now();
    usage(high, FeatureCode.MOCK_INTERVIEW, null, new Spend(5, 5, "m", at));
    usage(low, FeatureCode.MOCK_INTERVIEW, null, new Spend(5, 5, "m", at));

    assertThat(usage.topUsers(at.minusSeconds(1), 10))
        .extracting(UserUsage::userId)
        .containsExactly(low, high);
  }

  @Test
  void topProblemsSkipLedgerRowsWithoutProblemAndOrderByTokens() {
    UUID big = UUID.randomUUID();
    UUID small = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    Instant at = now();
    usage(user, FeatureCode.SOLUTION_REVIEW, small, new Spend(5, 5, "m", at));
    usage(user, FeatureCode.SOLUTION_REVIEW, big, new Spend(50, 50, "m", at));
    usage(user, FeatureCode.GENERATE_TESTCASE, big, new Spend(25, 25, "m", at));
    usage(user, FeatureCode.MOCK_INTERVIEW, null, new Spend(9_999, 0, "m", at));

    assertThat(usage.topProblems(at.minusSeconds(1), 5))
        .containsExactly(new ProblemUsage(big, 150L, 2), new ProblemUsage(small, 10L, 1));
    assertThat(usage.topProblems(at.minusSeconds(1), 1)).hasSize(1);
  }

  @Test
  void openAlertsCapAtOneHundredNewestFirstAndSkipReviewed() {
    UUID user = UUID.randomUUID();
    Instant base = now().minus(1, ChronoUnit.DAYS);
    for (int i = 0; i < 105; i++) {
      usage.insertAlert(alert(user, base.plusSeconds(i)));
    }
    AnomalyAlert newest = alert(user, base.plusSeconds(500));
    usage.insertAlert(newest);
    usage.markReviewed(newest.id());

    List<AnomalyAlert> open = usage.openAlerts();

    assertThat(count("ai_usage_anomaly_alerts")).isEqualTo(106);
    assertThat(open).hasSize(100);
    assertThat(open.get(0).createdAt()).isEqualTo(base.plusSeconds(104));
    assertThat(open.get(99).createdAt()).isEqualTo(base.plusSeconds(5));
    assertThat(open).extracting(AnomalyAlert::id).doesNotContain(newest.id());
  }

  @Test
  void insertAlertStoresNumericColumnsAndMapsBackExactly() {
    AnomalyAlert stored = alert(UUID.randomUUID(), now());

    usage.insertAlert(stored);

    assertThat(usage.openAlerts()).containsExactly(stored);
    assertThat(
            jdbc.sql("SELECT status FROM ai.ai_usage_anomaly_alerts WHERE id = :id")
                .param("id", stored.id())
                .query(String.class)
                .single())
        .isEqualTo("OPEN");
  }

  @Test
  void invalidAlertWindowIsRejectedByTheCheckConstraint() {
    Instant at = now();
    var inverted =
        new AnomalyAlert(
            UUID.randomUUID(),
            UUID.randomUUID(),
            at,
            at.minusSeconds(1),
            1,
            BigDecimal.ONE,
            BigDecimal.ONE,
            at);

    assertThatThrownBy(() -> usage.insertAlert(inverted))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(count("ai_usage_anomaly_alerts")).isZero();
  }

  @Test
  void markReviewedFlipsOnceThenReportsAlreadyReviewedOrMissing() {
    AnomalyAlert stored = alert(UUID.randomUUID(), now());
    usage.insertAlert(stored);

    AlertReview first = usage.markReviewed(stored.id()).orElseThrow();
    AlertReview second = usage.markReviewed(stored.id()).orElseThrow();

    assertThat(first.alreadyReviewed()).isFalse();
    assertThat(second.alreadyReviewed()).isTrue();
    assertThat(second.reviewedAt()).isEqualTo(first.reviewedAt());
    assertThat(usage.markReviewed(UUID.randomUUID())).isEmpty();
    assertThat(
            jdbc.sql("SELECT status FROM ai.ai_usage_anomaly_alerts WHERE id = :id")
                .param("id", stored.id())
                .query(String.class)
                .single())
        .isEqualTo("REVIEWED");
    assertThat(usage.openAlerts()).isEmpty();
  }

  @Test
  void callCountsAndUsersWithOpenAlertReflectTheTables() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    Instant at = now();
    usage(a, FeatureCode.MOCK_INTERVIEW, null, new Spend(1, 1, "m", at));
    usage(a, FeatureCode.MOCK_INTERVIEW, null, new Spend(1, 1, "m", at));
    usage(b, FeatureCode.MOCK_INTERVIEW, null, new Spend(1, 1, "m", at.minus(3, ChronoUnit.DAYS)));
    usage.insertAlert(alert(a, at));
    usage.insertAlert(alert(a, at.plusSeconds(1)));
    AnomalyAlert closed = alert(b, at);
    usage.insertAlert(closed);
    usage.markReviewed(closed.id());

    assertThat(usage.callCountsSince(at.minusSeconds(60))).isEqualTo(java.util.Map.of(a, 2));
    assertThat(usage.usersWithOpenAlert()).containsExactly(a);
  }

  @Test
  void anomalyAdvisoryLockExcludesASecondTransactionUntilCommit() {
    Boolean[] seen = new Boolean[3];

    tx.executeWithoutResult(
        status -> {
          seen[0] = usage.tryAnomalyLock();
          seen[1] = other.execute(inner -> usage.tryAnomalyLock());
        });
    seen[2] = tx.execute(status -> usage.tryAnomalyLock());

    assertThat(seen).containsExactly(true, false, true);
  }

  @Test
  void outboxInsertCastsPayloadToJsonbAndRejectsMismatchedPair() {
    UUID aggregate = UUID.randomUUID();
    AiOutboxEvent done = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, now());

    outbox.insert(done, "SolutionReview", aggregate);

    assertThat(
            jdbc.sql("SELECT jsonb_typeof(payload) || ':' || (payload->>'k') FROM ai.outbox_events")
                .query(String.class)
                .single())
        .isEqualTo("object:1");
    assertThatThrownBy(
            () ->
                outbox.insert(
                    event(AiOutboxEvent.MOCK_INTERVIEW_FINISHED, now()),
                    "SolutionReview",
                    aggregate))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(count("outbox_events")).isEqualTo(1);
  }

  @Test
  void lockPendingReturnsOldestUnpublishedFirstWithinLimit() {
    Instant base = now().minusSeconds(100);
    AiOutboxEvent oldest = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, base);
    AiOutboxEvent middle = event(AiOutboxEvent.MOCK_INTERVIEW_FINISHED, base.plusSeconds(1));
    AiOutboxEvent published = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, base.plusSeconds(2));
    outbox.insert(middle, "InterviewSession", UUID.randomUUID());
    outbox.insert(oldest, "SolutionReview", UUID.randomUUID());
    outbox.insert(published, "SolutionReview", UUID.randomUUID());
    outbox.attempted(published.id(), true);

    List<AiOutboxEvent> locked = tx.execute(status -> outbox.lockPending(10));
    List<AiOutboxEvent> limited = tx.execute(status -> outbox.lockPending(1));

    assertThat(locked).extracting(AiOutboxEvent::id).containsExactly(oldest.id(), middle.id());
    assertThat(locked.get(0).payload()).isEqualTo("{\"k\": 1}");
    assertThat(locked.get(0).createdAt()).isEqualTo(base);
    assertThat(limited).extracting(AiOutboxEvent::id).containsExactly(oldest.id());
  }

  @Test
  void skipLockedHandsEachConcurrentWorkerDifferentRows() {
    Instant base = now().minusSeconds(100);
    AiOutboxEvent first = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, base);
    AiOutboxEvent second = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, base.plusSeconds(1));
    AiOutboxEvent third = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, base.plusSeconds(2));
    for (AiOutboxEvent e : List.of(first, second, third)) {
      outbox.insert(e, "SolutionReview", UUID.randomUUID());
    }

    List<UUID> inner = new java.util.ArrayList<>();
    List<UUID> outer = new java.util.ArrayList<>();
    tx.executeWithoutResult(
        status -> {
          outbox.lockPending(1).forEach(e -> outer.add(e.id()));
          other.executeWithoutResult(s -> outbox.lockPending(10).forEach(e -> inner.add(e.id())));
        });

    assertThat(outer).containsExactly(first.id());
    assertThat(inner).containsExactly(second.id(), third.id());
  }

  @Test
  void attemptedCountsEveryTryAndStampsPublishedAtOnlyWhenConfirmedOnce() {
    AiOutboxEvent e = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, now());
    outbox.insert(e, "SolutionReview", UUID.randomUUID());

    outbox.attempted(e.id(), false);
    outbox.attempted(e.id(), false);
    assertThat(attempts(e.id())).isEqualTo(2);
    assertThat(publishedAt(e.id())).isEqualTo("none");

    outbox.attempted(e.id(), true);
    String stamped = publishedAt(e.id());
    assertThat(attempts(e.id())).isEqualTo(3);
    assertThat(stamped).isNotEqualTo("none");

    outbox.attempted(e.id(), true);
    assertThat(attempts(e.id())).isEqualTo(3);
    assertThat(publishedAt(e.id())).isEqualTo(stamped);
  }

  @Test
  void attemptedDoesNotOverflowTheIntCounter() {
    AiOutboxEvent e = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, now());
    outbox.insert(e, "SolutionReview", UUID.randomUUID());
    jdbc.sql("UPDATE ai.outbox_events SET publish_attempts = 2147483647").update();

    outbox.attempted(e.id(), false);

    assertThat(attempts(e.id())).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void cleanupDeletesOnlyOldPublishedRowsUpToTheLimit() {
    Instant at = now();
    AiOutboxEvent old1 = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, at);
    AiOutboxEvent old2 = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, at);
    AiOutboxEvent recent = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, at);
    AiOutboxEvent pending = event(AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, at);
    for (AiOutboxEvent e : List.of(old1, old2, recent, pending)) {
      outbox.insert(e, "SolutionReview", UUID.randomUUID());
    }
    outbox.attempted(old1.id(), true);
    outbox.attempted(old2.id(), true);
    outbox.attempted(recent.id(), true);
    jdbc.sql(
            "UPDATE ai.outbox_events SET published_at = now() - interval '10 days' WHERE id IN (:a,:b)")
        .param("a", old1.id())
        .param("b", old2.id())
        .update();

    assertThat(outbox.deletePublishedOlderThanDays(7, 1)).isEqualTo(1);
    assertThat(outbox.deletePublishedOlderThanDays(7, 10)).isEqualTo(1);
    assertThat(outbox.deletePublishedOlderThanDays(7, 10)).isZero();

    assertThat(jdbc.sql("SELECT id FROM ai.outbox_events").query(UUID.class).list())
        .containsExactlyInAnyOrder(recent.id(), pending.id());
  }

  @Test
  void cleanupAdvisoryLockExcludesASecondTransactionUntilCommit() {
    Boolean[] seen = new Boolean[3];

    tx.executeWithoutResult(
        status -> {
          seen[0] = outbox.tryCleanupLock();
          seen[1] = other.execute(inner -> outbox.tryCleanupLock());
        });
    seen[2] = tx.execute(status -> outbox.tryCleanupLock());

    assertThat(seen).containsExactly(true, false, true);
  }

  private int attempts(UUID id) {
    return jdbc.sql("SELECT publish_attempts FROM ai.outbox_events WHERE id = :id")
        .param("id", id)
        .query(Integer.class)
        .single();
  }

  /** Text form of published_at, "none" while unpublished (keeps the null case simple). */
  private String publishedAt(UUID id) {
    return jdbc.sql(
            "SELECT COALESCE(published_at::text, 'none') FROM ai.outbox_events WHERE id = :id")
        .param("id", id)
        .query(String.class)
        .single();
  }
}

package com.algoprep.aireview.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.aireview.domain.model.AiOutboxEvent;
import com.algoprep.aireview.domain.model.AlertReview;
import com.algoprep.aireview.domain.model.AnomalyAlert;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.ResponseGuards;
import com.algoprep.aireview.domain.model.TokenUsageRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SQL, binding and mapping for usage, anomaly, outbox, token-usage and prompt-snapshot access. */
class JdbcUsageAndOutboxRepositoriesTest {
  private static final Instant T0 = Instant.parse("2026-11-01T10:00:00Z");
  private static final OffsetDateTime T0_UTC = OffsetDateTime.ofInstant(T0, ZoneOffset.UTC);
  private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID P1 = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

  @Test
  void dailyTokensMapsDayFeatureAndTokens() {
    var jdbc = new FakeJdbc();
    jdbc.rows(
        List.of(
            FakeJdbc.row0(
                "day",
                LocalDate.of(2026, 11, 1),
                "feature_code",
                "MOCK_INTERVIEW",
                "tokens",
                77L)));
    var repo = new JdbcAiUsageRepository(jdbc.provider());

    var result = repo.dailyTokens(T0, T0.plusSeconds(3600));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).feature()).isEqualTo(FeatureCode.MOCK_INTERVIEW);
    assertThat(result.get(0).tokens()).isEqualTo(77L);
    assertThat(jdbc.lastParams().get(1)).isEqualTo(T0_UTC);
  }

  @Test
  void topUsersKeepsNullCostWhenAPriceIsMissing() {
    var jdbc = new FakeJdbc();
    jdbc.rows(
        List.of(
            FakeJdbc.row0("user_id", U1, "tokens", 500L, "calls", 4, "cost", new BigDecimal("1.2")),
            FakeJdbc.row0("user_id", P1, "tokens", 100L, "calls", 1, "cost", null)));
    var repo = new JdbcAiUsageRepository(jdbc.provider());

    var users = repo.topUsers(T0, 5);

    assertThat(users).hasSize(2);
    assertThat(users.get(0).costUsd()).isEqualByComparingTo("1.2");
    assertThat(users.get(1).costUsd()).isNull();
    assertThat(jdbc.lastParams()).containsEntry(2, 5);
  }

  @Test
  void topProblemsMapsRows() {
    var jdbc = new FakeJdbc();
    jdbc.rows(List.of(FakeJdbc.row0("problem_id", P1, "tokens", 9L, "calls", 2)));
    var repo = new JdbcAiUsageRepository(jdbc.provider());

    var problems = repo.topProblems(T0, 3);

    assertThat(problems).singleElement().satisfies(p -> assertThat(p.calls()).isEqualTo(2));
    assertThat(jdbc.lastSql()).contains("problem_id IS NOT NULL");
  }

  @Test
  void openAlertsMapAndCapAtOneHundred() {
    var jdbc = new FakeJdbc();
    jdbc.rows(
        List.of(
            FakeJdbc.row0(
                "id",
                P1,
                "user_id",
                U1,
                "window_start",
                T0_UTC,
                "window_end",
                T0_UTC.plusHours(1),
                "call_count",
                30,
                "baseline_avg",
                new BigDecimal("3.0"),
                "ratio",
                new BigDecimal("10.0"),
                "created_at",
                T0_UTC)));
    var repo = new JdbcAiUsageRepository(jdbc.provider());

    List<AnomalyAlert> alerts = repo.openAlerts();

    assertThat(alerts).singleElement().satisfies(a -> assertThat(a.callCount()).isEqualTo(30));
    assertThat(alerts.get(0).windowEnd()).isEqualTo(T0.plusSeconds(3600));
    assertThat(jdbc.lastParams()).containsEntry(1, 100);
  }

  @Test
  void markReviewedReturnsFreshReviewWhenAlertWasOpen() {
    var jdbc = new FakeJdbc();
    jdbc.row("reviewed_at", T0_UTC);
    var repo = new JdbcAiUsageRepository(jdbc.provider());

    Optional<AlertReview> review = repo.markReviewed(P1);

    assertThat(review).contains(new AlertReview(P1, T0, false));
  }

  @Test
  void markReviewedReportsAlreadyReviewedOrMissing() {
    var jdbc = new FakeJdbc();
    jdbc.none().row("reviewed_at", T0_UTC);
    jdbc.none().none();
    var repo = new JdbcAiUsageRepository(jdbc.provider());

    assertThat(repo.markReviewed(P1)).contains(new AlertReview(P1, T0, true));
    assertThat(repo.markReviewed(P1)).isEmpty();
  }

  @Test
  void anomalyLockCountsAndOpenAlertUsers() {
    var jdbc = new FakeJdbc();
    jdbc.row("l", true);
    jdbc.rows(List.of(FakeJdbc.row0("user_id", U1, "calls", 7)));
    jdbc.rows(List.of(FakeJdbc.row0("user_id", U1), FakeJdbc.row0("user_id", P1)));
    var repo = new JdbcAiUsageRepository(jdbc.provider());

    assertThat(repo.tryAnomalyLock()).isTrue();
    Map<UUID, Integer> counts = repo.callCountsSince(T0);
    assertThat(counts).containsEntry(U1, 7);
    Set<UUID> users = repo.usersWithOpenAlert();
    assertThat(users).containsExactlyInAnyOrder(U1, P1);
  }

  @Test
  void insertAlertBindsAllColumnsAsOpen() {
    var jdbc = new FakeJdbc();
    var repo = new JdbcAiUsageRepository(jdbc.provider());
    var alert =
        new AnomalyAlert(
            P1, U1, T0, T0.plusSeconds(60), 12, new BigDecimal("2.5"), new BigDecimal("4.8"), T0);

    repo.insertAlert(alert);

    assertThat(jdbc.lastSql()).contains("'OPEN'");
    assertThat(jdbc.lastParams())
        .containsEntry(1, P1)
        .containsEntry(2, U1)
        .containsEntry(3, T0_UTC)
        .containsEntry(5, 12);
  }

  @Test
  void outboxInsertLockPendingAttemptedAndCleanup() {
    var jdbc = new FakeJdbc();
    jdbc.rows(
        List.of(
            FakeJdbc.row0(
                "id",
                P1,
                "event_type",
                "SolutionReviewCompleted",
                "payload",
                "{\"a\":1}",
                "created_at",
                T0_UTC,
                "publish_attempts",
                2)));
    jdbc.row("l", true);
    jdbc.update(1).update(1).update(3);
    var repo = new JdbcAiOutboxRepository(jdbc.provider());
    var event = new AiOutboxEvent(P1, "SolutionReviewCompleted", "{\"a\":1}", T0, 0);

    repo.insert(event, "solution_review", U1);
    assertThat(jdbc.lastSql()).contains("CAST(? AS jsonb)");
    assertThat(jdbc.lastParams()).containsEntry(2, "solution_review").containsEntry(3, U1);

    List<AiOutboxEvent> pending = repo.lockPending(10);
    assertThat(pending).singleElement().satisfies(e -> assertThat(e.attempts()).isEqualTo(2));
    assertThat(pending.get(0).createdAt()).isEqualTo(T0);
    assertThat(jdbc.lastSql()).contains("SKIP LOCKED");

    repo.attempted(P1, true);
    assertThat(jdbc.lastParams()).containsEntry(1, true).containsEntry(2, P1);

    assertThat(repo.tryCleanupLock()).isTrue();
    assertThat(repo.deletePublishedOlderThanDays(14, 500)).isEqualTo(3);
    assertThat(jdbc.lastParams()).containsEntry(1, 14).containsEntry(2, 500);
  }

  @Test
  void tokenUsageInsertReportsWhetherRowWasNew() {
    var jdbc = new FakeJdbc();
    jdbc.update(1).update(0);
    UUID generated = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
    var repo = new JdbcAiTokenUsageRepository(jdbc.provider(), () -> generated);
    var usage = new TokenUsageRecord(P1, U1, FeatureCode.SOLUTION_REVIEW, null, U1, 100, 50, "m");

    assertThat(repo.insertIfAbsent(usage)).isTrue();
    assertThat(jdbc.lastParams()).containsEntry(1, generated).containsEntry(2, P1);
    assertThat(jdbc.lastParams()).containsEntry(4, "SOLUTION_REVIEW");
    assertThat(repo.insertIfAbsent(usage)).isFalse();
    assertThat(jdbc.lastSql()).contains("ON CONFLICT (call_id) DO NOTHING");
  }

  @Test
  void readActivePromptParsesGuardsAndDefaultsToNone() {
    var jdbc = new FakeJdbc();
    jdbc.row(
        "id", P1,
        "system_instruction", "SYS",
        "response_schema_version", "v1",
        "model_id", "model-a",
        "guards", "{\"citeLines\":true,\"noFullSolution\":true}");
    jdbc.row(
        "id", P1,
        "system_instruction", "SYS2",
        "response_schema_version", "v1",
        "model_id", "model-a",
        "guards", null);
    jdbc.none();
    var repo = new JdbcAiPromptSnapshotRepository(jdbc.provider());

    PromptSnapshot guarded = repo.readActive(FeatureCode.SOLUTION_REVIEW).orElseThrow();
    PromptSnapshot plain = repo.readActive(FeatureCode.GENERATE_TESTCASE).orElseThrow();

    assertThat(guarded.guards()).isEqualTo(new ResponseGuards(true, false, true));
    assertThat(guarded.systemInstruction()).isEqualTo("SYS");
    assertThat(plain.guards()).isEqualTo(ResponseGuards.NONE);
    assertThat(repo.readActive(FeatureCode.MOCK_INTERVIEW)).isEmpty();
    assertThat(jdbc.lastParams()).containsEntry(1, "MOCK_INTERVIEW");
  }
}

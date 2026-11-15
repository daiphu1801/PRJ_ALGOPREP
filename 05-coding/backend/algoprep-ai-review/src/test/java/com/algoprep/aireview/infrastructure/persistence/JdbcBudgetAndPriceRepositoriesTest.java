package com.algoprep.aireview.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.domain.exception.AiModelPriceConflictException;
import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.LimitsConfig;
import com.algoprep.aireview.domain.model.LimitsPatch;
import com.algoprep.aireview.domain.model.ModelPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** SQL text, parameter binding and row mapping of the budget and model-price repositories. */
class JdbcBudgetAndPriceRepositoriesTest {
  private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

  private static Map<String, Object> budgetRow(Object lockedAt) {
    return FakeJdbc.row0(
        "limit_tokens",
        5_000_000L,
        "period_start",
        LocalDate.of(2026, 11, 1),
        "locked_at",
        lockedAt,
        "review_per_hour",
        6,
        "interview_per_hour",
        3,
        "cooldown_seconds",
        30);
  }

  @Test
  void readConfigMapsUnlockedAndLockedRows() {
    var jdbc = new FakeJdbc();
    jdbc.rows(List.of(budgetRow(null)));
    jdbc.rows(List.of(budgetRow(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))));
    var repo = new JdbcAiBudgetRepository(jdbc.provider());

    BudgetConfig open = repo.readConfig();
    BudgetConfig locked = repo.readConfig();

    assertThat(open.lockedAt()).isNull();
    assertThat(open.limitTokens()).isEqualTo(5_000_000L);
    assertThat(open.periodStart()).isEqualTo(LocalDate.of(2026, 11, 1));
    assertThat(open.cooldownSeconds()).isEqualTo(30);
    assertThat(locked.lockedAt()).isEqualTo(NOW);
    assertThat(jdbc.lastSql()).contains("ai.ai_token_budget_configs").doesNotContain("FOR UPDATE");
  }

  @Test
  void usedTokensSinceBindsUtcInstantAndLockUpdatesOnce() {
    var jdbc = new FakeJdbc();
    jdbc.row("sum", 1234L);
    var repo = new JdbcAiBudgetRepository(jdbc.provider());

    assertThat(repo.usedTokensSince(NOW)).isEqualTo(1234L);
    assertThat(jdbc.lastParams().get(1)).isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));

    jdbc.update(1).update(0);
    assertThat(repo.lockIfUnlocked()).isTrue();
    assertThat(repo.lockIfUnlocked()).isFalse();
    assertThat(jdbc.lastSql()).contains("locked_at IS NULL");
  }

  @Test
  void adminReadsLimitsAndConfigForUpdate() {
    var jdbc = new FakeJdbc();
    jdbc.row(
        "review_per_hour", 6, "interview_per_hour", 3, "cooldown_seconds", 30, "warn_percent", 80);
    jdbc.rows(List.of(budgetRow(null)));
    var repo = new JdbcAiBudgetAdminRepository(jdbc.provider());

    assertThat(repo.readLimits()).isEqualTo(new LimitsConfig(6, 3, 30, 80));
    assertThat(repo.readConfigForUpdate().reviewPerHour()).isEqualTo(6);
    assertThat(jdbc.lastSql()).endsWith("FOR UPDATE");
  }

  @Test
  void tokensByFeatureSinceGroupsPerFeature() {
    var jdbc = new FakeJdbc();
    jdbc.rows(
        List.of(
            FakeJdbc.row0("feature_code", "SOLUTION_REVIEW", "tokens", 10L),
            FakeJdbc.row0("feature_code", "MOCK_INTERVIEW", "tokens", 20L)));
    var repo = new JdbcAiBudgetAdminRepository(jdbc.provider());

    Map<FeatureCode, Long> byFeature = repo.tokensByFeatureSince(NOW);

    assertThat(byFeature)
        .containsEntry(FeatureCode.SOLUTION_REVIEW, 10L)
        .containsEntry(FeatureCode.MOCK_INTERVIEW, 20L);
  }

  @Test
  void updateLimitReturnsPreviousLimitAndRecordsActor() {
    var jdbc = new FakeJdbc();
    jdbc.row("limit_tokens", 100L);
    var repo = new JdbcAiBudgetAdminRepository(jdbc.provider());

    assertThat(repo.updateLimit(900L, ACTOR)).isEqualTo(100L);
    assertThat(jdbc.lastParams().values()).contains(900L, ACTOR);
  }

  @Test
  void clearLockAndRolloverIssueUpdates() {
    var jdbc = new FakeJdbc();
    jdbc.update(1).update(1);
    var repo = new JdbcAiBudgetAdminRepository(jdbc.provider());

    repo.clearLock(ACTOR);
    assertThat(jdbc.lastSql()).contains("locked_at = NULL");
    assertThat(jdbc.lastParams().values()).contains(ACTOR);

    assertThat(repo.rollover(LocalDate.of(2026, 12, 1))).isEqualTo(1);
    assertThat(jdbc.lastSql()).contains("period_start <");
  }

  @Test
  void updateLimitsBindsNullPatchFieldsAsNullAndReturnsRow() {
    var jdbc = new FakeJdbc();
    jdbc.row(
        "review_per_hour", 9, "interview_per_hour", 3, "cooldown_seconds", 30, "warn_percent", 80);
    var repo = new JdbcAiBudgetAdminRepository(jdbc.provider());

    LimitsConfig result = repo.updateLimits(new LimitsPatch(9, null, null, null), ACTOR);

    assertThat(result.reviewPerHour()).isEqualTo(9);
    assertThat(jdbc.lastParams()).containsEntry(1, 9).containsEntry(2, null);
    assertThat(jdbc.lastSql()).contains("COALESCE");
  }

  @Test
  void rolloverLockReflectsAdvisoryLockResult() {
    var jdbc = new FakeJdbc();
    jdbc.row("lock", true).row("lock", false);
    var repo = new JdbcAiBudgetAdminRepository(jdbc.provider());

    assertThat(repo.tryRolloverLock()).isTrue();
    assertThat(repo.tryRolloverLock()).isFalse();
    assertThat(jdbc.lastSql()).contains("pg_try_advisory_xact_lock");
  }

  @Test
  void modelPriceListCreateUpdateDelete() {
    UUID id = UUID.randomUUID();
    var jdbc = new FakeJdbc();
    var price = new BigDecimal("1.50");
    jdbc.row("id", id, "model_name", "gpt-x", "usd_per_million_tokens", price);
    jdbc.row("id", id, "model_name", "gpt-x", "usd_per_million_tokens", 0);
    jdbc.row("id", id, "model_name", "gpt-y", "usd_per_million_tokens", price);
    jdbc.none();
    var repo = new JdbcAiModelPriceRepository(jdbc.provider());

    assertThat(repo.list()).containsExactly(new ModelPrice(id, "gpt-x", price));
    assertThat(repo.create("gpt-x").usdPerMillionTokens()).isEqualByComparingTo("0");
    Optional<ModelPrice> updated = repo.update(id, "gpt-y", price);
    assertThat(updated).map(ModelPrice::modelName).contains("gpt-y");
    assertThat(repo.update(id, null, null)).isEmpty();
    repo.delete(id);
    assertThat(jdbc.lastSql()).startsWith("DELETE FROM ai.ai_model_prices");
    assertThat(jdbc.lastParams().get(1)).isEqualTo(id);
  }

  @Test
  void modelPriceDuplicateNameMapsToConflict() {
    var failing =
        new JdbcAiModelPriceRepository(
            new ObjectProvider<JdbcClient>() {
              @Override
              public JdbcClient getObject() {
                throw new DuplicateKeyException("dup");
              }
            });

    assertThatThrownBy(() -> failing.create("x")).isInstanceOf(AiModelPriceConflictException.class);
    assertThatThrownBy(() -> failing.update(UUID.randomUUID(), "x", null))
        .isInstanceOf(AiModelPriceConflictException.class);
  }
}

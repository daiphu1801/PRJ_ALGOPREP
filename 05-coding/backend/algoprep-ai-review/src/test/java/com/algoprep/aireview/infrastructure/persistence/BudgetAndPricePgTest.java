package com.algoprep.aireview.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.domain.exception.AiModelPriceConflictException;
import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.LimitsConfig;
import com.algoprep.aireview.domain.model.LimitsPatch;
import com.algoprep.aireview.domain.model.ModelPrice;
import com.algoprep.aireview.domain.model.TokenUsageRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** Budget, limits, model prices and the token ledger against real PostgreSQL. */
class BudgetAndPricePgTest extends PostgresTestBase {
  private final JdbcAiBudgetRepository budget = new JdbcAiBudgetRepository(provider());
  private final JdbcAiBudgetAdminRepository admin = new JdbcAiBudgetAdminRepository(provider());
  private final JdbcAiModelPriceRepository prices = new JdbcAiModelPriceRepository(provider());
  private final JdbcAiTokenUsageRepository ledger =
      new JdbcAiTokenUsageRepository(provider(), UUID::randomUUID);

  @Test
  void seededSingletonReadsWithDefaults() {
    BudgetConfig config = budget.readConfig();

    assertThat(config.limitTokens()).isZero();
    assertThat(config.lockedAt()).isNull();
    assertThat(config.reviewPerHour()).isEqualTo(10);
    assertThat(config.interviewPerHour()).isEqualTo(3);
    assertThat(config.cooldownSeconds()).isEqualTo(20);
    assertThat(admin.readLimits()).isEqualTo(new LimitsConfig(10, 3, 20, 70));
    BudgetConfig locked = tx.execute(status -> admin.readConfigForUpdate());
    assertThat(locked).isEqualTo(config);
  }

  @Test
  void patchSqlKeepsColumnsWhoseFieldIsNullAndStampsActor() {
    UUID actor = UUID.randomUUID();

    LimitsConfig first = admin.updateLimits(new LimitsPatch(null, 7, null, 90), actor);
    LimitsConfig second = admin.updateLimits(new LimitsPatch(null, null, null, null), actor);

    assertThat(first).isEqualTo(new LimitsConfig(10, 7, 20, 90));
    assertThat(second).isEqualTo(first);
    assertThat(
            jdbc.sql("SELECT updated_by FROM ai.ai_token_budget_configs WHERE id = 1")
                .query(UUID.class)
                .single())
        .isEqualTo(actor);
  }

  @Test
  void patchOutsideCheckRangeIsRejectedAndLeavesRowUntouched() {
    assertThatThrownBy(
            () -> admin.updateLimits(new LimitsPatch(0, null, null, null), UUID.randomUUID()))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(admin.readLimits()).isEqualTo(new LimitsConfig(10, 3, 20, 70));
  }

  @Test
  void updateLimitReturnsTheOldValueFromTheLockedCte() {
    UUID actor = UUID.randomUUID();

    long first = admin.updateLimit(5_000_000L, actor);
    long second = admin.updateLimit(9_000_000L, actor);

    assertThat(first).isZero();
    assertThat(second).isEqualTo(5_000_000L);
    assertThat(budget.readConfig().limitTokens()).isEqualTo(9_000_000L);
  }

  @Test
  void lockIfUnlockedWinsOnceThenClearLockReopensIt() {
    assertThat(budget.lockIfUnlocked()).isTrue();
    assertThat(budget.lockIfUnlocked()).isFalse();
    assertThat(budget.readConfig().lockedAt()).isNotNull();

    admin.clearLock(UUID.randomUUID());

    assertThat(budget.readConfig().lockedAt()).isNull();
    assertThat(budget.lockIfUnlocked()).isTrue();
  }

  @Test
  void rolloverMovesPeriodOnlyForwardAndClearsTheLock() {
    LocalDate current = budget.readConfig().periodStart();
    LocalDate next = current.plusMonths(1);
    budget.lockIfUnlocked();

    assertThat(admin.rollover(current)).isZero();
    assertThat(admin.rollover(next)).isEqualTo(1);
    assertThat(admin.rollover(next)).isZero();

    BudgetConfig after = budget.readConfig();
    assertThat(after.periodStart()).isEqualTo(next);
    assertThat(after.lockedAt()).isNull();
  }

  @Test
  void rolloverAdvisoryLockExcludesASecondTransactionUntilCommit() {
    Boolean[] seen = new Boolean[3];

    tx.executeWithoutResult(
        status -> {
          seen[0] = admin.tryRolloverLock();
          seen[1] = other.execute(inner -> admin.tryRolloverLock());
        });
    seen[2] = tx.execute(status -> admin.tryRolloverLock());

    assertThat(seen).containsExactly(true, false, true);
  }

  @Test
  void sumsUseBigintSoLargeLedgersDoNotOverflowAnInt() {
    UUID user = UUID.randomUUID();
    Instant at = now();
    UUID problem = UUID.randomUUID();
    usage(
        user,
        FeatureCode.SOLUTION_REVIEW,
        problem,
        new Spend(2_000_000_000, 2_000_000_000, "m", at));
    usage(user, FeatureCode.MOCK_INTERVIEW, null, new Spend(10, 5, "m", at));
    usage(
        user, FeatureCode.MOCK_INTERVIEW, null, new Spend(1, 1, "m", at.minus(2, ChronoUnit.DAYS)));

    assertThat(budget.usedTokensSince(at.minusSeconds(60))).isEqualTo(4_000_000_015L);
    assertThat(admin.tokensByFeatureSince(at.minusSeconds(60)))
        .isEqualTo(
            Map.of(FeatureCode.SOLUTION_REVIEW, 4_000_000_000L, FeatureCode.MOCK_INTERVIEW, 15L));
    assertThat(budget.usedTokensSince(at.plusSeconds(60))).isZero();
    assertThat(admin.tokensByFeatureSince(at.plusSeconds(60))).isEmpty();
  }

  @Test
  void priceNameUniquenessIsCaseInsensitiveOnTheRealIndex() {
    ModelPrice created = prices.create("GPT-Test");

    assertThat(created.usdPerMillionTokens()).isEqualByComparingTo("0");
    assertThatThrownBy(() -> prices.create("gpt-test"))
        .isInstanceOf(AiModelPriceConflictException.class);
    assertThat(count("ai_model_prices")).isEqualTo(1);
  }

  @Test
  void priceUpdateCoalescesNullsAndMapsRenameClashToConflict() {
    ModelPrice a = prices.create("alpha");
    ModelPrice b = prices.create("beta");

    ModelPrice priced = prices.update(a.id(), null, new BigDecimal("1.5")).orElseThrow();
    ModelPrice renamed = prices.update(a.id(), "Alpha-2", null).orElseThrow();

    assertThat(priced.modelName()).isEqualTo("alpha");
    assertThat(priced.usdPerMillionTokens()).isEqualByComparingTo("1.5");
    assertThat(renamed.modelName()).isEqualTo("Alpha-2");
    assertThat(renamed.usdPerMillionTokens()).isEqualByComparingTo("1.5");
    assertThatThrownBy(() -> prices.update(b.id(), "ALPHA-2", null))
        .isInstanceOf(AiModelPriceConflictException.class);
    assertThat(prices.update(UUID.randomUUID(), "x", null)).isEmpty();
    assertThat(
            jdbc.sql("SELECT model_name FROM ai.ai_model_prices WHERE id = :id")
                .param("id", b.id())
                .query(String.class)
                .single())
        .isEqualTo("beta");
  }

  @Test
  void priceListIsSortedCaseInsensitivelyAndDeleteRemovesTheRow() {
    prices.create("beta");
    ModelPrice upper = prices.create("Alpha");
    prices.create("charlie");

    assertThat(prices.list())
        .extracting(ModelPrice::modelName)
        .containsExactly("Alpha", "beta", "charlie");

    prices.delete(upper.id());
    prices.delete(upper.id());

    assertThat(prices.list()).extracting(ModelPrice::modelName).containsExactly("beta", "charlie");
  }

  @Test
  void insertIfAbsentIsIdempotentOnCallIdAndWritesOneRow() {
    UUID call = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    var first =
        new TokenUsageRecord(call, user, FeatureCode.PRACTICE_FEEDBACK, null, user, 100, 20, "m");
    var retry =
        new TokenUsageRecord(call, user, FeatureCode.PRACTICE_FEEDBACK, null, user, 999, 999, "m");

    assertThat(ledger.insertIfAbsent(first)).isTrue();
    assertThat(ledger.insertIfAbsent(retry)).isFalse();

    assertThat(count("token_usage")).isEqualTo(1);
    assertThat(
            jdbc.sql("SELECT tokens_prompt + tokens_completion FROM ai.token_usage")
                .query(Integer.class)
                .single())
        .isEqualTo(120);
  }

  @Test
  void ledgerRejectsRowsBreakingTheFeatureProblemCheck() {
    UUID user = UUID.randomUUID();
    UUID problem = UUID.randomUUID();
    var noProblem =
        new TokenUsageRecord(
            UUID.randomUUID(), user, FeatureCode.SOLUTION_REVIEW, null, user, 1, 1, "m");
    var ownerMismatch =
        new TokenUsageRecord(
            UUID.randomUUID(), user, FeatureCode.GENERATE_TESTCASE, problem, user, 1, 1, "m");
    var valid =
        new TokenUsageRecord(
            UUID.randomUUID(), user, FeatureCode.GENERATE_TESTCASE, problem, problem, 1, 1, "m");

    assertThatThrownBy(() -> ledger.insertIfAbsent(noProblem))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> ledger.insertIfAbsent(ownerMismatch))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(count("token_usage")).isZero();

    assertThat(ledger.insertIfAbsent(valid)).isTrue();
    assertThat(count("token_usage")).isEqualTo(1);
  }
}

package com.algoprep.aireview.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.BudgetStatus;
import com.algoprep.aireview.application.dto.RolloverOutcome;
import com.algoprep.aireview.application.ports.out.AiAuditPort;
import com.algoprep.aireview.application.query.BudgetStatusAssembler;
import com.algoprep.aireview.domain.exception.AiBudgetUnlockException;
import com.algoprep.aireview.domain.exception.AiModelPriceNotFoundException;
import com.algoprep.aireview.domain.model.AiAuditEntry;
import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.LimitsConfig;
import com.algoprep.aireview.domain.model.LimitsPatch;
import com.algoprep.aireview.domain.model.ModelPrice;
import com.algoprep.aireview.domain.ports.out.AiBudgetAdminRepository;
import com.algoprep.aireview.domain.ports.out.AiBudgetRepository;
import com.algoprep.aireview.domain.ports.out.AiModelPriceRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.ServiceUnavailableException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminBudgetHandlersTest {
  private static final LocalDate PERIOD = LocalDate.of(2026, 11, 1);
  private static final Instant PERIOD_AT = Instant.parse("2026-10-31T17:00:00Z");
  private static final BudgetStatus STATUS =
      new BudgetStatus(10, 0, PERIOD, null, false, null, Map.of(), 70);
  private final AiBudgetAdminRepository admin = mock(AiBudgetAdminRepository.class);
  private final AiBudgetRepository budget = mock(AiBudgetRepository.class);
  private final BudgetStatusAssembler assembler = mock(BudgetStatusAssembler.class);
  private final AiAuditPort audit = mock(AiAuditPort.class);
  private final UUID actor = UUID.randomUUID();

  private static BudgetConfig config(long limit, Instant lockedAt) {
    return new BudgetConfig(limit, PERIOD, lockedAt, 10, 3, 20);
  }

  @Test
  void settingTheLimitStoresItAndWritesOneAuditRowWithOldAndNewValue() {
    when(admin.updateLimit(5000, actor)).thenReturn(1000L);
    when(assembler.status()).thenReturn(STATUS);
    BudgetStatus result =
        new SetAiTokenLimitCommandHandler(admin, assembler, audit).setLimit(actor, 5000);
    assertThat(result).isSameAs(STATUS);
    ArgumentCaptor<AiAuditEntry> entry = ArgumentCaptor.forClass(AiAuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("AI_BUDGET_LIMIT_CHANGED");
    assertThat(entry.getValue().actorUserId()).isEqualTo(actor);
    assertThat(entry.getValue().beforeJson()).contains("1000");
    assertThat(entry.getValue().afterJson()).contains("5000");
  }

  @Test
  void anAuditFailureOnTheLimitBecomesAir503SoTheTransactionRollsBack() {
    when(admin.updateLimit(anyLong(), any())).thenReturn(0L);
    doThrow(new IllegalStateException("down")).when(audit).record(any());
    assertThatThrownBy(
            () -> new SetAiTokenLimitCommandHandler(admin, assembler, audit).setLimit(actor, 5))
        .isInstanceOf(ServiceUnavailableException.class)
        .hasMessageNotContaining("down");
  }

  @Test
  void unlockingAnUnlockedBudgetChangesNothingAndWritesNoAudit() {
    when(admin.readConfigForUpdate()).thenReturn(config(1000, null));
    when(assembler.status()).thenReturn(STATUS);
    new UnlockAiTokenBudgetCommandHandler(admin, budget, assembler, audit).unlock(actor);
    verify(admin, never()).clearLock(any());
    verifyNoInteractions(audit);
  }

  @Test
  void unlockingWhileTheTotalReachedTheLimitIsAir203AndChangesNothing() {
    when(admin.readConfigForUpdate()).thenReturn(config(1000, Instant.EPOCH));
    when(budget.usedTokensSince(PERIOD_AT)).thenReturn(1000L);
    assertThatThrownBy(
            () ->
                new UnlockAiTokenBudgetCommandHandler(admin, budget, assembler, audit)
                    .unlock(actor))
        .isInstanceOf(AiBudgetUnlockException.class);
    verify(admin, never()).clearLock(any());
    verifyNoInteractions(audit);
  }

  @Test
  void unlockingBelowTheLimitClearsTheLockAndAudits() {
    when(admin.readConfigForUpdate()).thenReturn(config(1000, Instant.EPOCH));
    when(budget.usedTokensSince(PERIOD_AT)).thenReturn(999L);
    when(assembler.status()).thenReturn(STATUS);
    new UnlockAiTokenBudgetCommandHandler(admin, budget, assembler, audit).unlock(actor);
    verify(admin).clearLock(actor);
    ArgumentCaptor<AiAuditEntry> entry = ArgumentCaptor.forClass(AiAuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("AI_BUDGET_UNLOCKED");
  }

  @Test
  void anAuditFailureOnUnlockBecomesAir503() {
    when(admin.readConfigForUpdate()).thenReturn(config(1000, Instant.EPOCH));
    when(budget.usedTokensSince(PERIOD_AT)).thenReturn(1L);
    doThrow(new IllegalStateException("down")).when(audit).record(any());
    assertThatThrownBy(
            () ->
                new UnlockAiTokenBudgetCommandHandler(admin, budget, assembler, audit)
                    .unlock(actor))
        .isInstanceOf(ServiceUnavailableException.class);
  }

  @Test
  void limitsUpdateDelegatesTheWholePatch() {
    LimitsPatch patch = new LimitsPatch(5, null, null, null);
    LimitsConfig stored = new LimitsConfig(5, 3, 20, 70);
    when(admin.updateLimits(patch, actor)).thenReturn(stored);
    assertThat(new UpdateAiLimitsConfigCommandHandler(admin).update(actor, patch))
        .isEqualTo(stored);
  }

  @Test
  void priceHandlersDelegateAndAnUnknownIdIsNotFound() {
    AiModelPriceRepository prices = mock(AiModelPriceRepository.class);
    ModelPriceCommandHandlers handlers = new ModelPriceCommandHandlers(prices);
    UUID id = UUID.randomUUID();
    ModelPrice row = new ModelPrice(id, "gpt-x", BigDecimal.ZERO);
    when(prices.create("gpt-x")).thenReturn(row);
    when(prices.update(id, null, BigDecimal.ONE)).thenReturn(Optional.of(row));
    when(prices.update(eq(id), eq("n"), any())).thenReturn(Optional.empty());

    assertThat(handlers.create("gpt-x")).isSameAs(row);
    assertThat(handlers.update(id, null, BigDecimal.ONE)).isSameAs(row);
    assertThatThrownBy(() -> handlers.update(id, "n", null))
        .isInstanceOf(AiModelPriceNotFoundException.class);
    handlers.delete(id);
    verify(prices).delete(id);
  }

  @Test
  void rolloverTargetsTheFirstOfTheMonthInUtcPlus7AndReportsTheUnlock() {
    ClockPort clock = mock(ClockPort.class);
    // 2026-11-30T18:00Z is already 2026-12-01 01:00 in UTC+7.
    when(clock.now()).thenReturn(Instant.parse("2026-11-30T18:00:00Z"));
    when(admin.tryRolloverLock()).thenReturn(true);
    when(budget.readConfig()).thenReturn(config(1000, Instant.EPOCH));
    when(admin.rollover(LocalDate.of(2026, 12, 1))).thenReturn(1);
    RolloverOutcome outcome =
        new RolloverAiBudgetPeriodCommandHandler(admin, budget, clock).rollover();
    assertThat(outcome.changed()).isTrue();
    assertThat(outcome.unlocked()).isTrue();
    assertThat(outcome.periodStart()).isEqualTo(LocalDate.of(2026, 12, 1));
  }

  @Test
  void rolloverIsANoOpWhenTheMonthIsAlreadyCurrentOrTheLockIsHeld() {
    ClockPort clock = mock(ClockPort.class);
    when(clock.now()).thenReturn(Instant.parse("2026-11-15T00:00:00Z"));
    when(admin.tryRolloverLock()).thenReturn(true, false);
    when(budget.readConfig()).thenReturn(config(1000, null));
    RolloverAiBudgetPeriodCommandHandler handler =
        new RolloverAiBudgetPeriodCommandHandler(admin, budget, clock);
    RolloverOutcome same = handler.rollover();
    assertThat(same.changed()).isFalse();
    assertThat(same.unlocked()).isFalse();
    assertThat(same.periodStart()).isEqualTo(PERIOD);
    RolloverOutcome busy = handler.rollover();
    assertThat(busy.skippedByLock()).isTrue();
    verify(admin).rollover(any());
  }
}

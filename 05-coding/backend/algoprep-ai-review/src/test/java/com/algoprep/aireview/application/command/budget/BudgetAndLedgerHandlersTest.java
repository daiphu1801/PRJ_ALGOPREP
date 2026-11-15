package com.algoprep.aireview.application.command.budget;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.usage.RecordAiTokenUsageCommand;
import com.algoprep.aireview.domain.exception.budget.AiBudgetExceededException;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.budget.BudgetConfig;
import com.algoprep.aireview.domain.ports.out.AiBudgetRepository;
import com.algoprep.aireview.domain.ports.out.AiTokenUsageRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BudgetAndLedgerHandlersTest {
  private static final LocalDate PERIOD = LocalDate.of(2026, 11, 1);
  private static final Instant PERIOD_AT = Instant.parse("2026-10-31T17:00:00Z");
  private final AiBudgetRepository budget = mock(AiBudgetRepository.class);
  private final AiTokenUsageRepository usage = mock(AiTokenUsageRepository.class);
  private final UUID user = UUID.randomUUID();

  private BudgetConfig config(long limit, Instant lockedAt) {
    return new BudgetConfig(limit, PERIOD, lockedAt, 10, 3, 20);
  }

  private RecordAiTokenUsageCommand command(Integer prompt, Integer completion) {
    return new RecordAiTokenUsageCommand(
        UUID.randomUUID(),
        user,
        FeatureCode.PRACTICE_FEEDBACK,
        null,
        UUID.randomUUID(),
        prompt,
        completion,
        "default");
  }

  @Test
  void adminAlwaysPassesAndNothingIsRead() {
    new CheckAiBudgetCommandHandler(budget).check(user, CallerCategory.ADMIN);
    verifyNoInteractions(budget);
  }

  @Test
  void underTheLimitPasses() {
    when(budget.readConfig()).thenReturn(config(1000, null));
    when(budget.usedTokensSince(PERIOD_AT)).thenReturn(999L);
    new CheckAiBudgetCommandHandler(budget).check(user, CallerCategory.STUDENT);
    verify(budget, never()).lockIfUnlocked();
  }

  @Test
  void anUnconfiguredLimitNeverLocks() {
    when(budget.readConfig()).thenReturn(config(0, null));
    when(budget.usedTokensSince(PERIOD_AT)).thenReturn(9_999_999L);
    new CheckAiBudgetCommandHandler(budget).check(user, CallerCategory.INSTRUCTOR);
    verify(budget, never()).lockIfUnlocked();
  }

  @Test
  void reachingTheLimitSetsTheLockThenRejects() {
    when(budget.readConfig()).thenReturn(config(1000, null));
    when(budget.usedTokensSince(PERIOD_AT)).thenReturn(1000L);
    assertThatThrownBy(
            () -> new CheckAiBudgetCommandHandler(budget).check(user, CallerCategory.STUDENT))
        .isInstanceOf(AiBudgetExceededException.class)
        .hasMessageNotContaining(user.toString())
        .extracting("errorCode")
        .isEqualTo("AIR-401");
    verify(budget).lockIfUnlocked();
  }

  @Test
  void anExistingLockRejectsWithoutCountingUsage() {
    when(budget.readConfig()).thenReturn(config(1000, Instant.now()));
    assertThatThrownBy(
            () -> new CheckAiBudgetCommandHandler(budget).check(user, CallerCategory.STUDENT))
        .isInstanceOf(AiBudgetExceededException.class);
    verify(budget, never()).usedTokensSince(any());
  }
}

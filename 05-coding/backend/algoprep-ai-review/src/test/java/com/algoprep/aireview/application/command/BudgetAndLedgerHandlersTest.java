package com.algoprep.aireview.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.RecordAiTokenUsageCommand;
import com.algoprep.aireview.domain.exception.AiBudgetExceededException;
import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.TokenUsageRecord;
import com.algoprep.aireview.domain.ports.out.AiBudgetRepository;
import com.algoprep.aireview.domain.ports.out.AiTokenUsageRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

  @Test
  void theLedgerWriterInsertsAndLocksWhenTheNewTotalReachesTheLimit() {
    when(usage.insertIfAbsent(any())).thenReturn(true);
    when(budget.readConfig()).thenReturn(config(1000, null));
    when(budget.usedTokensSince(PERIOD_AT)).thenReturn(1500L);
    assertThat(new AiTokenLedgerWriter(usage, budget).write(command(1000, 500))).isTrue();
    ArgumentCaptor<TokenUsageRecord> row = ArgumentCaptor.forClass(TokenUsageRecord.class);
    verify(usage).insertIfAbsent(row.capture());
    assertThat(row.getValue().tokensPrompt()).isEqualTo(1000);
    verify(budget).lockIfUnlocked();
  }

  @Test
  void theLedgerWriterDoesNotLockBelowTheLimitOrWhenAlreadyLockedOrOnDuplicate() {
    var writer = new AiTokenLedgerWriter(usage, budget);
    when(usage.insertIfAbsent(any())).thenReturn(true);
    when(budget.readConfig()).thenReturn(config(1000, null));
    when(budget.usedTokensSince(PERIOD_AT)).thenReturn(999L);
    writer.write(command(1, 1));
    when(budget.readConfig()).thenReturn(config(1000, Instant.now()));
    writer.write(command(1, 1));
    when(usage.insertIfAbsent(any())).thenReturn(false);
    assertThat(writer.write(command(1, 1))).isFalse();
    verify(budget, never()).lockIfUnlocked();
    verify(budget, times(1)).usedTokensSince(any());
  }

  @Test
  void unknownUsageWritesNoRow() {
    var writer = mock(AiTokenLedgerWriter.class);
    var handler = new RecordAiTokenUsageCommandHandler(writer);
    handler.record(command(null, null));
    handler.record(command(10, null));
    verifyNoInteractions(writer);
  }

  @Test
  void aZeroUsageThatTheProviderReportedIsStillRecorded() {
    var writer = mock(AiTokenLedgerWriter.class);
    new RecordAiTokenUsageCommandHandler(writer).record(command(0, 0));
    verify(writer).write(any());
  }

  @Test
  void aFailingWriteIsRetriedWithTheSameCallIdThreeTimesThenGivenUpSilently() {
    var writer = mock(AiTokenLedgerWriter.class);
    when(writer.write(any())).thenThrow(new IllegalStateException("db down"));
    var cmd = command(5, 5);
    assertThatCode(() -> new RecordAiTokenUsageCommandHandler(writer).record(cmd))
        .doesNotThrowAnyException();
    ArgumentCaptor<RecordAiTokenUsageCommand> seen =
        ArgumentCaptor.forClass(RecordAiTokenUsageCommand.class);
    verify(writer, times(RecordAiTokenUsageCommandHandler.MAX_ATTEMPTS)).write(seen.capture());
    assertThat(seen.getAllValues()).allMatch(c -> c.callId().equals(cmd.callId()));
  }

  @Test
  void aWriteThatSucceedsOnTheSecondAttemptStops() {
    var writer = mock(AiTokenLedgerWriter.class);
    when(writer.write(any())).thenThrow(new IllegalStateException("blip")).thenReturn(true);
    new RecordAiTokenUsageCommandHandler(writer).record(command(5, 5));
    verify(writer, times(2)).write(any());
  }

  @Test
  void theCommandRejectsNegativeTokensAndMissingFields() {
    assertThatThrownBy(() -> command(-1, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> command(1, -1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new RecordAiTokenUsageCommand(
                    null, user, FeatureCode.PRACTICE_FEEDBACK, null, user, 1, 1, "m"))
        .isInstanceOf(NullPointerException.class);
    assertThat(command(1, 2).usageKnown()).isTrue();
  }
}

package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.model.AdminSubmissionStats.LevelProblems;
import com.algoprep.judge.domain.model.AdminSubmissionStats.Totals;
import com.algoprep.judge.domain.ports.out.AdminSubmissionStatsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetAdminSubmissionStatsQueryHandlerTest {
  private final AdminSubmissionStatsRepository repository =
      mock(AdminSubmissionStatsRepository.class);
  private final GetAdminSubmissionStatsQueryHandler handler =
      new GetAdminSubmissionStatsQueryHandler(repository);
  private final Instant now = Instant.parse("2026-11-04T00:00:00Z");

  @Test
  void brokenVerdictBlockDoesNotExecuteOrInvalidateAnotherAggregation() {
    when(repository.now()).thenReturn(now);
    when(repository.verdicts(now)).thenThrow(new IllegalStateException("private SQL password"));
    assertThatThrownBy(handler::verdicts)
        .hasMessage("Admin submission statistics are temporarily unavailable")
        .hasNoCause();
    verify(repository, never()).totals(any());
    when(repository.daily(any(), any())).thenReturn(List.of());
    when(repository.totals(now)).thenReturn(new Totals(4, 4, 0));
    assertThat(handler.summary().value()).isEqualTo(4);
    assertThat(handler.summary().sparklineSeries()).hasSize(7);
  }

  @Test
  void languageDayAndMonthEachUseTheirOwnWindowAndReturnCompleteShapes() {
    when(repository.now()).thenReturn(now);
    when(repository.languages(any(), any())).thenReturn(List.of());
    when(repository.daily(any(), any())).thenReturn(List.of());
    when(repository.months(any(), any())).thenReturn(List.of());
    when(repository.verdicts(now)).thenReturn(Map.of());
    assertThat(handler.byLanguage().series()).hasSize(3);
    assertThat(handler.byDay().columns()).hasSize(7);
    assertThat(handler.byMonth().points()).hasSize(6);
    assertThat(handler.verdicts().slices()).hasSize(5);
    verify(repository).languages(Instant.parse("2026-10-15T17:00:00Z"), now);
    verify(repository).daily(Instant.parse("2026-10-28T17:00:00Z"), now);
    verify(repository).months(Instant.parse("2026-05-31T17:00:00Z"), now);
    verify(repository, never()).totals(any());
  }

  @Test
  void windowsFollowTheUtcPlusSevenMidnightNotTheUtcOne() {
    // 16:59:59Z is still 04 Nov at 23:59:59 in UTC+7; one second later it is 05 Nov at 00:00:00.
    Instant lastSecond = Instant.parse("2026-11-04T16:59:59Z");
    Instant midnight = Instant.parse("2026-11-04T17:00:00Z");
    when(repository.daily(any(), any())).thenReturn(List.of());

    when(repository.now()).thenReturn(lastSecond);
    assertThat(handler.byDay().columns().getLast().day()).isEqualTo(LocalDate.parse("2026-11-04"));
    verify(repository).daily(Instant.parse("2026-10-28T17:00:00Z"), lastSecond);

    when(repository.now()).thenReturn(midnight);
    assertThat(handler.byDay().columns().getLast().day()).isEqualTo(LocalDate.parse("2026-11-05"));
    verify(repository).daily(Instant.parse("2026-10-29T17:00:00Z"), midnight);
  }

  @Test
  void monthWindowRollsOverAtTheUtcPlusSevenMonthEnd() {
    Instant lastSecond = Instant.parse("2026-10-31T16:59:59Z");
    Instant midnight = Instant.parse("2026-10-31T17:00:00Z");
    when(repository.months(any(), any())).thenReturn(List.of());

    when(repository.now()).thenReturn(lastSecond);
    assertThat(handler.byMonth().points().getLast().month()).isEqualTo(YearMonth.of(2026, 10));
    verify(repository).months(Instant.parse("2026-04-30T17:00:00Z"), lastSecond);

    when(repository.now()).thenReturn(midnight);
    assertThat(handler.byMonth().points().getLast().month()).isEqualTo(YearMonth.of(2026, 11));
    verify(repository).months(Instant.parse("2026-05-31T17:00:00Z"), midnight);
  }

  @Test
  void difficultyAndTopReceiveOnlyTheSuppliedPublishedProblemScope() {
    when(repository.now()).thenReturn(now);
    UUID problem = UUID.randomUUID();
    UUID level = UUID.randomUUID();
    when(repository.problems(Set.of(problem), now)).thenReturn(List.of());
    assertThat(
            handler
                .byDifficulty(List.of(new LevelProblems(level, Set.of(problem))))
                .groups()
                .getFirst()
                .submissions())
        .isZero();
    assertThat(handler.topProblems(Set.of(problem)).items()).isEmpty();
    org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(2))
        .problems(Set.of(problem), now);
    verify(repository, never()).totals(any());
  }
}

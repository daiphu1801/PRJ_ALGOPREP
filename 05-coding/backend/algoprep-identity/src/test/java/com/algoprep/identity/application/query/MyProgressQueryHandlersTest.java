package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.ActivityCalendar;
import com.algoprep.identity.domain.model.MyProgressSummary;
import com.algoprep.identity.domain.model.ProgressCounters;
import com.algoprep.identity.domain.model.SolveState;
import com.algoprep.identity.domain.ports.out.MyProgressReadRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MyProgressQueryHandlersTest {
  private final MyProgressReadRepository repository = mock(MyProgressReadRepository.class);
  private final ClockPort clock = mock(ClockPort.class);
  private final UUID user = UUID.randomUUID();

  @Test
  void rateDividesByEligibleCountAndRoundsHalfUp() {
    when(repository.counters(user)).thenReturn(new ProgressCounters(3, 10, 8, 5, 2, "JAVA"));
    MyProgressSummary summary = new GetMyProgressSummaryQueryHandler(repository).get(user);

    assertThat(summary.acceptanceRatePercent()).isEqualTo(63); // 5 / 8 = 62.5
    assertThat(summary.totalSubmissions()).isEqualTo(10);
    assertThat(summary.solvedProblemCount()).isEqualTo(3);
    assertThat(summary.firstTryAcceptedCount()).isEqualTo(2);
    assertThat(summary.topLanguage()).isEqualTo("JAVA");
  }

  @Test
  void rateIsNullWhenNothingIsEligible() {
    when(repository.counters(user)).thenReturn(new ProgressCounters(0, 2, 0, 0, 0, null));
    MyProgressSummary summary = new GetMyProgressSummaryQueryHandler(repository).get(user);

    assertThat(summary.acceptanceRatePercent()).isNull();
    assertThat(summary.totalSubmissions()).isEqualTo(2);
  }

  @Test
  void recentActivityAsksForTenEntriesOfTheLastNinetyDays() {
    Instant now = Instant.parse("2026-11-05T00:00:00Z");
    when(clock.now()).thenReturn(now);
    new ListMyRecentActivityQueryHandler(repository, clock).list(user);

    verify(repository).recentActivity(user, Instant.parse("2026-08-07T00:00:00Z"), 10);
  }

  @Test
  void calendarIsAlways364DaysEndingTodayInUtcPlusSevenWithZeroFill() {
    // 18:00Z is already the next local day (UTC+7).
    when(clock.now()).thenReturn(Instant.parse("2026-11-05T18:00:00Z"));
    LocalDate today = LocalDate.parse("2026-11-06");
    LocalDate first = today.minusDays(363);
    when(repository.dailyCounts(user, first, today)).thenReturn(Map.of(today, 4, first, 1));

    ActivityCalendar calendar = new GetMyActivityCalendarQueryHandler(repository, clock).get(user);

    assertThat(calendar.from()).isEqualTo(first);
    assertThat(calendar.to()).isEqualTo(today);
    assertThat(calendar.days()).hasSize(364);
    assertThat(calendar.days().get(0)).isEqualTo(new ActivityCalendar.Day(first, 1));
    assertThat(calendar.days().get(363)).isEqualTo(new ActivityCalendar.Day(today, 4));
    assertThat(calendar.days().get(1).submissionCount()).isZero();
    assertThat(calendar.days().get(1).day()).isEqualTo(first.plusDays(1));
  }

  @Test
  void solveStatesPassOnlyTheDeduplicatedAskedIds() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    when(repository.solveStates(any(), anyCollection())).thenReturn(Map.of(a, SolveState.SOLVED));

    Map<UUID, SolveState> result =
        new ListMyProblemSolveStatesQueryHandler(repository).list(user, a + " , " + b + "," + a);

    assertThat(result).containsOnlyKeys(a);
    verify(repository).solveStates(user, java.util.Set.of(a, b));
  }

  @Test
  void solveStatesRejectBadInputBeforeTouchingTheDatabase() {
    var handler = new ListMyProblemSolveStatesQueryHandler(repository);
    List<String> hundredOne = new ArrayList<>();
    for (int i = 0; i < 101; i++) {
      hundredOne.add(UUID.randomUUID().toString());
    }
    List<String> hundred = hundredOne.subList(0, 100);

    for (String bad :
        new String[] {
          null, "", "  ", "not-a-uuid", UUID.randomUUID() + ",", String.join(",", hundredOne)
        }) {
      assertThatThrownBy(() -> handler.list(user, bad))
          .isInstanceOf(DomainRuleViolationException.class)
          .extracting(e -> ((DomainRuleViolationException) e).errorCode())
          .isEqualTo(IdentityErrorCodes.PROBLEM_IDS_INVALID);
    }
    verifyNoInteractions(repository);

    handler.list(user, String.join(",", hundred));
    verify(repository).solveStates(any(), anyCollection());
  }
}

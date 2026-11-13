package com.algoprep.aireview.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.InterviewSummary;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.RubricCriterion;
import com.algoprep.aireview.domain.model.RubricRow;
import com.algoprep.aireview.domain.ports.out.InterviewScoringRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Endpoint 13, DD api F5 section 13.3 to 13.7. */
class GetMyInterviewSummaryQueryHandlerTest {
  private static final Instant NOW = Instant.parse("2026-11-10T01:00:00Z");

  private final InterviewScoringRepository scoring = mock(InterviewScoringRepository.class);
  private final ClockPort clock = mock(ClockPort.class);
  private final GetMyInterviewSummaryQueryHandler handler =
      new GetMyInterviewSummaryQueryHandler(scoring, clock);
  private final UUID user = UUID.randomUUID();

  private static List<RubricRow> session(UUID id, Instant ended, String... scores) {
    List<RubricRow> rows = new ArrayList<>();
    for (int i = 0; i < scores.length; i++) {
      rows.add(
          new RubricRow(
              id,
              InterviewEntryType.TOPIC_SELECTION,
              ended.minusSeconds(600),
              ended,
              RubricCriterion.values()[i],
              new BigDecimal(scores[i])));
    }
    return rows;
  }

  @SafeVarargs
  private static List<RubricRow> all(List<RubricRow>... sessions) {
    List<RubricRow> rows = new ArrayList<>();
    for (List<RubricRow> one : sessions) {
      rows.addAll(one);
    }
    return rows;
  }

  @SafeVarargs
  private final void given(List<RubricRow>... sessions) {
    when(clock.now()).thenReturn(NOW);
    when(scoring.completedRubrics(eq(user), any())).thenReturn(all(sessions));
  }

  @Test
  void noSessionGivesZeroAndNulls() {
    given();

    InterviewSummary s = handler.summary(user, "all", 5);

    assertThat(s.completedSessionCount()).isZero();
    assertThat(s.averageScore()).isNull();
    assertThat(s.weakest()).isNull();
    assertThat(s.recent()).isEmpty();
  }

  @Test
  void aHalfScoredSessionIsIgnoredAndTheWeakestCriterionIsTheLowestAverage() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    UUID partial = UUID.randomUUID();
    given(
        session(a, NOW.minusSeconds(100), "8", "6", "7", "5"),
        session(b, NOW.minusSeconds(200), "6", "8", "9", "3"),
        session(partial, NOW.minusSeconds(300), "1", "1", "1"));

    InterviewSummary s = handler.summary(user, "all", 5);

    assertThat(s.completedSessionCount()).isEqualTo(2);
    // a: 6.55, b: 6*.25+8*.30+9*.25+3*.20 = 6.75 -> mean 6.65
    assertThat(s.averageScore()).isEqualByComparingTo("6.65");
    assertThat(s.weakest()).isEqualTo(RubricCriterion.COMPLEXITY_AWARENESS);
    assertThat(s.recent()).extracting(InterviewSummary.Recent::sessionId).containsExactly(a, b);
    assertThat(s.recent().get(0).rubric().get(RubricCriterion.CLARITY)).isEqualByComparingTo("8");
  }

  @Test
  void aTieGoesToTheCriterionEarlierInTheFixedOrder() {
    given(session(UUID.randomUUID(), NOW, "4", "4", "9", "9"));

    assertThat(handler.summary(user, "all", 5).weakest()).isEqualTo(RubricCriterion.CLARITY);
  }

  @Test
  void limitCutsTheRecentListButNotTheCount() {
    given(
        session(UUID.randomUUID(), NOW.minusSeconds(1), "5", "5", "5", "5"),
        session(UUID.randomUUID(), NOW.minusSeconds(2), "5", "5", "5", "5"),
        session(UUID.randomUUID(), NOW.minusSeconds(3), "5", "5", "5", "5"));

    InterviewSummary s = handler.summary(user, "all", 2);

    assertThat(s.completedSessionCount()).isEqualTo(3);
    assertThat(s.recent()).hasSize(2);
  }

  @Test
  void theRangeIsAppliedOnEndedAtThroughTheStore() {
    given();

    handler.summary(user, "7d", 5);
    handler.summary(user, "30d", 5);
    handler.summary(user, "all", 5);

    verify(scoring).completedRubrics(user, NOW.minusSeconds(7L * 24 * 3600));
    verify(scoring).completedRubrics(user, NOW.minusSeconds(30L * 24 * 3600));
    verify(scoring).completedRubrics(user, null);
  }

  @Test
  void anUnknownRangeIsAirV10() {
    assertThatThrownBy(() -> handler.summary(user, "90d", 5))
        .isInstanceOf(AiInvalidRequestException.class)
        .hasMessageContaining("range");
  }
}

package com.algoprep.aireview.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** BR-AI-09 step 8: fixed weights 25, 30, 25, 20; overall is derived, never stored. */
class InterviewResultTest {
  private static RubricScore score(RubricCriterion c, String value, String comment) {
    return new RubricScore(c, new BigDecimal(value), comment);
  }

  private static InterviewResult result(String a, String b, String c, String d) {
    return new InterviewResult(
        UUID.randomUUID(),
        List.of(
            score(RubricCriterion.CLARITY, a, "one"),
            score(RubricCriterion.TECHNICAL_ACCURACY, b, "two"),
            score(RubricCriterion.PUSHBACK_HANDLING, c, "three"),
            score(RubricCriterion.COMPLEXITY_AWARENESS, d, "four")),
        2,
        Instant.parse("2026-11-09T10:30:00Z"));
  }

  @Test
  void theWorkedExampleOfTheDdIs655() {
    assertThat(result("8", "6", "7", "5").overallScore()).isEqualByComparingTo("6.55");
  }

  @Test
  void roundsHalfUpToTwoDecimals() {
    // 0.01 x 25 / 100 = 0.0025 -> 0.00 ; 0.02 x 25 / 100 = 0.005 -> 0.01
    assertThat(result("0.01", "0", "0", "0").overallScore()).isEqualByComparingTo("0.00");
    assertThat(result("0.02", "0", "0", "0").overallScore()).isEqualByComparingTo("0.01");
  }

  @Test
  void theSummaryJoinsTheFourCommentsWithBlankLines() {
    assertThat(result("1", "1", "1", "1").feedbackSummary())
        .isEqualTo("one\n\ntwo\n\nthree\n\nfour");
  }

  @Test
  void aResultNeedsAllFourCriteria() {
    assertThatThrownBy(
            () ->
                new InterviewResult(
                    UUID.randomUUID(),
                    List.of(score(RubricCriterion.CLARITY, "1", "x")),
                    1,
                    Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

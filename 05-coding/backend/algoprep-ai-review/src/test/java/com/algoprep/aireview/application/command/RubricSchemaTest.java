package com.algoprep.aireview.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.model.RubricCriterion;
import com.algoprep.aireview.domain.model.RubricScore;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** DD validation F5 section 5, BR-AI-09 step 5. */
class RubricSchemaTest {
  private static String item(String code, String score, String comment) {
    return "{\"code\":\"" + code + "\",\"score\":" + score + ",\"comment\":\"" + comment + "\"}";
  }

  private static String valid(String firstScore) {
    return "{\"criteria\":["
        + item("COMPLEXITY_AWARENESS", "5", "d")
        + ","
        + item("CLARITY", firstScore, "a")
        + ","
        + item("PUSHBACK_HANDLING", "7", "c")
        + ","
        + item("TECHNICAL_ACCURACY", "6", "b")
        + "]}";
  }

  @Test
  void returnsTheFourScoresInTheFixedOrderWhateverOrderTheModelUsed() {
    List<RubricScore> scores = RubricSchema.parse(valid("8"));

    assertThat(scores).extracting(RubricScore::criterion).containsExactly(RubricCriterion.values());
    assertThat(scores).extracting(RubricScore::comment).containsExactly("a", "b", "c", "d");
  }

  @Test
  void roundsToTwoDecimalsAndAcceptsBothEdges() {
    assertThat(RubricSchema.parse(valid("7.125")).get(0).score())
        .isEqualByComparingTo(new BigDecimal("7.13"));
    assertThat(RubricSchema.parse(valid("0")).get(0).score()).isEqualByComparingTo("0");
    assertThat(RubricSchema.parse(valid("10")).get(0).score()).isEqualByComparingTo("10");
  }

  @Test
  void anOutOfRangeScoreIsRejectedNotClamped() {
    assertThatThrownBy(() -> RubricSchema.parse(valid("10.01")))
        .isInstanceOf(AiInvalidResponseException.class);
    assertThatThrownBy(() -> RubricSchema.parse(valid("-0.01")))
        .isInstanceOf(AiInvalidResponseException.class);
  }

  @Test
  void rejectsAnythingThatIsNotExactlyFourDistinctKnownCriteria() {
    String three =
        "{\"criteria\":["
            + item("CLARITY", "5", "a")
            + ","
            + item("TECHNICAL_ACCURACY", "5", "b")
            + ","
            + item("PUSHBACK_HANDLING", "5", "c")
            + "]}";
    String duplicate =
        "{\"criteria\":["
            + item("CLARITY", "5", "a")
            + ","
            + item("CLARITY", "5", "b")
            + ","
            + item("PUSHBACK_HANDLING", "5", "c")
            + ","
            + item("COMPLEXITY_AWARENESS", "5", "d")
            + "]}";
    String unknown = valid("5").replace("COMPLEXITY_AWARENESS", "STYLE");
    for (String bad : List.of(three, duplicate, unknown, "{}", "[]", "not json", "")) {
      assertThatThrownBy(() -> RubricSchema.parse(bad))
          .isInstanceOf(AiInvalidResponseException.class);
    }
  }

  @Test
  void aBlankCommentIsRejectedAndALongOneIsCutAt1000() {
    assertThatThrownBy(() -> RubricSchema.parse(valid("5").replace("\"a\"", "\"  \"")))
        .isInstanceOf(AiInvalidResponseException.class);
    String longComment = "x".repeat(1500);

    assertThat(
            RubricSchema.parse(valid("5").replace("\"a\"", "\"" + longComment + "\""))
                .get(0)
                .comment())
        .hasSize(1000);
  }

  @Test
  void aScoreThatIsAStringIsRejected() {
    assertThatThrownBy(() -> RubricSchema.parse(valid("\"8\"")))
        .isInstanceOf(AiInvalidResponseException.class);
  }
}

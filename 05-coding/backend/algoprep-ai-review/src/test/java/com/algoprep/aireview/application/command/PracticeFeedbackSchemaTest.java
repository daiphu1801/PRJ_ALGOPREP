package com.algoprep.aireview.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.application.dto.PracticeFeedback;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PracticeFeedbackSchemaTest {
  private static final List<PracticeFeedback.Criterion> RUBRIC =
      List.of(
          new PracticeFeedback.Criterion("Clarity", new BigDecimal("50"), null),
          new PracticeFeedback.Criterion("<b>Depth</b>", new BigDecimal("50"), null));

  private static PracticeFeedback.Output parse(String json) {
    return PracticeFeedbackSchema.parse(json, RUBRIC, "m");
  }

  private static String criteria(String body) {
    return "{\"criteria\":[" + body + "]}";
  }

  private static String one(String code, String percent) {
    return "{\"criterionCode\":\"" + code + "\",\"achievementPercent\":" + percent + "}";
  }

  @Test
  void acceptsAFullAnswerNormalisesCodesAndDropsUnknownKeys() {
    var out =
        parse(
            "{\"achievedPoints\":[\"a\"],\"total\":99,\"criteria\":["
                + one("CLARITY", "70")
                + ","
                + one("&lt;b&gt;depth&lt;/b&gt;", "0")
                + "]}");

    assertThat(out.criteria())
        .containsExactly(
            new PracticeFeedback.Scored("Clarity", 70),
            new PracticeFeedback.Scored("<b>Depth</b>", 0));
    assertThat(out.achievedPoints()).containsExactly("a");
    assertThat(out.missingPoints()).isEmpty();
  }

  @Test
  void boundsOfThePercentAreInclusive() {
    var out = parse(criteria(one("Clarity", "100") + "," + one("<b>Depth</b>", "0")));
    assertThat(out.criteria())
        .extracting(PracticeFeedback.Scored::achievementPercent)
        .containsExactly(100, 0);
  }

  @Test
  void rejectsEveryShapeThatDoesNotMatchTheSnapshot() {
    String ok = one("Clarity", "50");
    String other = one("<b>Depth</b>", "50");
    List<String> bad =
        List.of(
            "not json",
            "[]",
            "{}",
            "{\"criteria\":{}}",
            criteria(ok),
            criteria(ok + "," + ok),
            criteria(ok + "," + other + "," + one("Extra", "1")),
            criteria(ok + "," + one("<b>Depth</b>", "101")),
            criteria(ok + "," + one("<b>Depth</b>", "-1")),
            criteria(ok + "," + one("<b>Depth</b>", "50.5")),
            criteria(ok + "," + one("<b>Depth</b>", "\"50\"")),
            criteria(ok + ",{\"criterionCode\":5,\"achievementPercent\":1}"),
            criteria(ok + ",{\"achievementPercent\":1}"),
            "{\"achievedPoints\":\"x\",\"criteria\":[" + ok + "," + other + "]}",
            "{\"missingPoints\":[1],\"criteria\":[" + ok + "," + other + "]}");
    for (String json : bad) {
      assertThatThrownBy(() -> parse(json)).as(json).isInstanceOf(AiInvalidResponseException.class);
    }
    assertThatThrownBy(() -> PracticeFeedbackSchema.parse(null, RUBRIC, "m"))
        .isInstanceOf(AiInvalidResponseException.class);
  }

  @Test
  void pointListsAreCappedAtTenItemsOf500Characters() {
    StringBuilder items = new StringBuilder();
    for (int i = 0; i < 11; i++) {
      items.append(i == 0 ? "" : ",").append("\"").append("y".repeat(600)).append("\"");
    }
    var out =
        parse(
            "{\"improvementSuggestions\":["
                + items
                + "],\"criteria\":["
                + one("Clarity", "1")
                + ","
                + one("<b>Depth</b>", "1")
                + "]}");
    assertThat(out.improvementSuggestions()).hasSize(10);
    assertThat(out.improvementSuggestions().get(0)).hasSize(500);
  }
}

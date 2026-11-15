package com.algoprep.interviewbank.presentation.controller;

import static com.algoprep.interviewbank.presentation.controller.ControllerFixtures.caller;
import static com.algoprep.interviewbank.presentation.controller.ControllerFixtures.json;
import static com.algoprep.interviewbank.presentation.controller.ControllerFixtures.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.application.dto.ImportRow;
import com.algoprep.interviewbank.domain.model.QuestionSort;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Shape validation of the management endpoints (DD api F6 sections 19 to 24, codes INB-V*). */
class QuestionRequestsTest {
  private final UUID actorId = UUID.randomUUID();
  private final JwtAuthenticationToken caller = caller(actorId, "ROLE_ADMIN");
  private final UUID topic = UUID.randomUUID();
  private final UUID level = UUID.randomUUID();

  private static void rejects(Runnable call, String code) {
    assertThatThrownBy(call::run)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  private String valid(String extra) {
    return "{\"title\":\" T \",\"topicId\":\""
        + topic
        + "\",\"levelId\":\""
        + level
        + "\""
        + extra
        + "}";
  }

  @Test
  void actorComesFromTokenSubject() {
    assertThat(QuestionRequests.actor(caller)).isEqualTo(actorId);
  }

  @Test
  void questionIdThatIsNotAUuidReadsAsMissingQuestion() {
    assertThat(QuestionRequests.questionId(topic.toString())).isEqualTo(topic);
    assertThatThrownBy(() -> QuestionRequests.questionId("nope"))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class, e -> assertThat(e.errorCode()).isEqualTo("INB-010"));
  }

  @Test
  void bodyReadsJsonWithinTheLimit() throws Exception {
    assertThat(QuestionRequests.body(request("{\"a\":1.5}"), 100, "X").path("a").decimalValue())
        .isEqualByComparingTo("1.5");
  }

  @Test
  void bodyLargerThanLimitIsRefusedAndUnreadableUsesGivenCode() {
    assertThatThrownBy(() -> QuestionRequests.body(request("12345678"), 4, "X"))
        .isInstanceOfSatisfying(
            BodyTooLargeException.class, e -> assertThat(e.errorCode()).isEqualTo("INB-V17"));
    rejects(
        () -> {
          try {
            QuestionRequests.body(request("{not json"), 100, "INB-V08");
          } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
          }
        },
        "INB-V08");
  }

  @Test
  void listDefaultsToDisplayNumberFirstPageOfEight() {
    var query = QuestionRequests.list(new HashMap<>());
    assertThat(query.sort()).isEqualTo(QuestionSort.DISPLAY_NO);
    assertThat(query.page()).isEqualTo(1);
    assertThat(query.pageSize()).isEqualTo(8);
    assertThat(query.descending()).isFalse();
    assertThat(query.q()).isNull();
  }

  @Test
  void listParsesEveryParameter() {
    var params =
        Map.of(
            "q", "  hash ",
            "sort", "averageScorePercent",
            "dir", "desc",
            "page", " 3 ",
            "pageSize", "50",
            "topic", " CS ",
            "level", "  ");
    var query = QuestionRequests.list(params);
    assertThat(query.q()).isEqualTo("hash");
    assertThat(query.sort()).isEqualTo(QuestionSort.AVERAGE_SCORE_PERCENT);
    assertThat(query.descending()).isTrue();
    assertThat(query.page()).isEqualTo(3);
    assertThat(query.pageSize()).isEqualTo(50);
    assertThat(query.topic()).isEqualTo("CS");
    assertThat(query.level()).isNull();
    assertThat(QuestionRequests.list(Map.of("q", "   ")).q()).isNull();
  }

  @ParameterizedTest
  @CsvSource({"sort,bogus", "dir,sideways", "page,0", "page,abc", "pageSize,7", "pageSize,xyz"})
  void listRejectsBadParameters(String key, String value) {
    rejects(() -> QuestionRequests.list(Map.of(key, value)), "INB-V01");
  }

  @Test
  void listRejectsOverlongQueryByCodePoints() {
    assertThat(QuestionRequests.list(Map.of("q", "a".repeat(100))).q()).hasSize(100);
    rejects(() -> QuestionRequests.list(Map.of("q", "a".repeat(101))), "INB-V01");
  }

  @Test
  void questionParsesAndNormalisesAFullBody() {
    var command =
        QuestionRequests.question(
            caller,
            null,
            json(
                valid(
                    ",\"contentMarkdown\":\"body\",\"followUpQuestions\":[\" why \",\"  \"],"
                        + "\"rubric\":[{\"criterionCode\":\" c1 \",\"weightPercent\":60.50,"
                        + "\"description\":\"d\"},{\"criterionCode\":\"c2\",\"weightPercent\":39.5}]")));
    assertThat(command.actorId()).isEqualTo(actorId);
    assertThat(command.title()).isEqualTo("T");
    assertThat(command.topicId()).isEqualTo(topic);
    assertThat(command.contentMarkdown()).isEqualTo("body");
    assertThat(command.followUps()).containsExactly("why");
    assertThat(command.rubric()).hasSize(2);
    assertThat(command.rubric().get(0).criterionCode()).isEqualTo("c1");
    assertThat(command.rubric().get(0).weightPercent()).isEqualByComparingTo("60.5");
    assertThat(command.rubric().get(1).description()).isNull();
  }

  @Test
  void questionTreatsMissingBlankOrNullContentAsNull() {
    assertThat(QuestionRequests.question(caller, null, json(valid(""))).contentMarkdown()).isNull();
    assertThat(
            QuestionRequests.question(caller, null, json(valid(",\"contentMarkdown\":\"  \"")))
                .contentMarkdown())
        .isNull();
    assertThat(
            QuestionRequests.question(caller, null, json(valid(",\"contentMarkdown\":null")))
                .followUps())
        .isEmpty();
    assertThat(QuestionRequests.question(caller, null, json(valid(",\"rubric\":null"))).rubric())
        .isEmpty();
  }

  @Test
  void questionRejectsBadShapes() {
    rejects(() -> QuestionRequests.question(caller, null, json("[]")), "INB-V08");
    rejects(() -> QuestionRequests.question(caller, null, json("{\"title\":5}")), "INB-V08");
    rejects(
        () ->
            QuestionRequests.question(
                caller,
                null,
                json("{\"title\":\"" + "x".repeat(256) + "\",\"topicId\":\"" + topic + "\"}")),
        "INB-V08");
    rejects(
        () ->
            QuestionRequests.question(
                caller, null, json("{\"title\":\"t\",\"levelId\":\"" + level + "\"}")),
        "INB-V09");
    rejects(
        () ->
            QuestionRequests.question(
                caller,
                null,
                json("{\"title\":\"t\",\"topicId\":\"bad\",\"levelId\":\"" + level + "\"}")),
        "INB-V09");
    rejects(
        () -> QuestionRequests.question(caller, null, json(valid(",\"contentMarkdown\":5"))),
        "INB-V08");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        ",\"followUpQuestions\":\"text\"",
        ",\"followUpQuestions\":[1]",
      })
  void questionRejectsBadFollowUps(String extra) {
    rejects(() -> QuestionRequests.question(caller, null, json(valid(extra))), "INB-V12");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        ",\"rubric\":{}",
        ",\"rubric\":[1]",
        ",\"rubric\":[{\"weightPercent\":10}]",
        ",\"rubric\":[{\"criterionCode\":\"  \",\"weightPercent\":10}]",
        ",\"rubric\":[{\"criterionCode\":\"c\"}]",
        ",\"rubric\":[{\"criterionCode\":\"c\",\"weightPercent\":\"10\"}]",
        ",\"rubric\":[{\"criterionCode\":\"c\",\"weightPercent\":-1}]",
        ",\"rubric\":[{\"criterionCode\":\"c\",\"weightPercent\":100.01}]",
        ",\"rubric\":[{\"criterionCode\":\"c\",\"weightPercent\":1.234}]",
        ",\"rubric\":[{\"criterionCode\":\"c\",\"weightPercent\":1,\"description\":5}]"
      })
  void questionRejectsBadRubric(String extra) {
    rejects(() -> QuestionRequests.question(caller, null, json(valid(extra))), "INB-V10");
  }

  @Test
  void questionRejectsOverlongCriterionCode() {
    rejects(
        () ->
            QuestionRequests.question(
                caller,
                null,
                json(
                    valid(
                        ",\"rubric\":[{\"criterionCode\":\""
                            + "c".repeat(101)
                            + "\",\"weightPercent\":1}]"))),
        "INB-V10");
  }

  @Test
  void bulkParsesDistinctIds() {
    UUID other = UUID.randomUUID();
    var command =
        QuestionRequests.bulk(caller, json("{\"ids\":[\"" + topic + "\",\"" + other + "\"]}"));
    assertThat(command.ids()).containsExactly(topic, other);
    assertThat(command.actorId()).isEqualTo(actorId);
  }

  @Test
  void bulkRejectsBadIdLists() {
    rejects(() -> QuestionRequests.bulk(caller, json("{}")), "INB-V02");
    rejects(() -> QuestionRequests.bulk(caller, json("{\"ids\":[]}")), "INB-V02");
    rejects(() -> QuestionRequests.bulk(caller, json("{\"ids\":[1]}")), "INB-V02");
    rejects(() -> QuestionRequests.bulk(caller, json("{\"ids\":[\"bad\"]}")), "INB-V02");
    rejects(
        () ->
            QuestionRequests.bulk(caller, json("{\"ids\":[\"" + topic + "\",\"" + topic + "\"]}")),
        "INB-V02");
    var tooMany = new StringBuilder("{\"ids\":[");
    for (int i = 0; i < 101; i++) {
      tooMany.append(i == 0 ? "" : ",").append("\"").append(UUID.randomUUID()).append("\"");
    }
    rejects(() -> QuestionRequests.bulk(caller, json(tooMany.append("]}").toString())), "INB-V02");
  }

  @Test
  void importRowsParsesEveryOptionalField() {
    var command =
        QuestionRequests.importRows(
            caller,
            json(
                "{\"rows\":[{\"line\":7,\"topic\":\"CS\",\"level\":\"EASY\",\"question\":\"Q\","
                    + "\"rubric\":[{\"criterionCode\":\"c\",\"weightPercent\":100}],"
                    + "\"followUps\":[\"f\"],\"content\":\"body\",\"suggestedApproach\":[\"s\"],"
                    + "\"coreKeywords\":[\"k\"],\"sampleAnswer\":\"sa\"}]}"));
    ImportRow row = command.rows().get(0);
    assertThat(row.line()).isEqualTo(7);
    assertThat(row.topic()).isEqualTo("CS");
    assertThat(row.level()).isEqualTo("EASY");
    assertThat(row.question()).isEqualTo("Q");
    assertThat(row.rubricShapeOk()).isTrue();
    assertThat(row.rubric()).hasSize(1);
    assertThat(row.followUps()).containsExactly("f");
    assertThat(row.content()).isEqualTo("body");
    assertThat(row.suggestedApproach()).containsExactly("s");
    assertThat(row.coreKeywords()).containsExactly("k");
    assertThat(row.sampleAnswer()).isEqualTo("sa");
    assertThat(row.fieldsShapeOk()).isTrue();
  }

  @Test
  void importRowWithMinimalFieldsIsStillShapeOk() {
    var row = QuestionRequests.importRows(caller, json("{\"rows\":[{\"line\":1}]}")).rows().get(0);
    assertThat(row.topic()).isNull();
    assertThat(row.rubric()).isEmpty();
    assertThat(row.rubricShapeOk()).isTrue();
    assertThat(row.fieldsShapeOk()).isTrue();
    assertThat(row.followUps()).isEmpty();
  }

  @Test
  void importRowFlagsRubricShapeProblemsWithoutFailingTheRequest() {
    for (String rubric : new String[] {"5", "[1]"}) {
      var row =
          QuestionRequests.importRows(
                  caller, json("{\"rows\":[{\"line\":1,\"rubric\":" + rubric + "}]}"))
              .rows()
              .get(0);
      assertThat(row.rubricShapeOk()).isFalse();
      assertThat(row.rubric()).isEmpty();
    }
    var typed =
        QuestionRequests.importRows(
                caller,
                json(
                    "{\"rows\":[{\"line\":1,\"rubric\":[{\"criterionCode\":3,\"weightPercent\":\"x\"}]}]}"))
            .rows()
            .get(0);
    assertThat(typed.rubric().get(0).criterionCode()).isNull();
    assertThat(typed.rubric().get(0).weightPercent()).isNull();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\"followUps\":\"x\"",
        "\"followUps\":[1]",
        "\"content\":5",
        "\"suggestedApproach\":5",
        "\"coreKeywords\":[true]",
        "\"sampleAnswer\":[]"
      })
  void importRowFlagsFieldShapeProblems(String field) {
    var row =
        QuestionRequests.importRows(caller, json("{\"rows\":[{\"line\":1," + field + "}]}"))
            .rows()
            .get(0);
    assertThat(row.fieldsShapeOk()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"rows\":[]}",
        "{\"rows\":{}}",
        "{\"rows\":[5]}",
        "{\"rows\":[{}]}",
        "{\"rows\":[{\"line\":\"1\"}]}",
        "{\"rows\":[{\"line\":1.5}]}",
        "{\"rows\":[{\"line\":99999999999}]}"
      })
  void importRejectsBadRowEnvelopes(String body) {
    rejects(() -> QuestionRequests.importRows(caller, json(body)), "INB-V15");
  }

  @Test
  void importRejectsMoreThanFiveHundredRows() {
    var body = new StringBuilder("{\"rows\":[");
    for (int i = 0; i < 501; i++) {
      body.append(i == 0 ? "" : ",").append("{\"line\":").append(i + 1).append("}");
    }
    rejects(
        () -> QuestionRequests.importRows(caller, json(body.append("]}").toString())), "INB-V15");
  }
}

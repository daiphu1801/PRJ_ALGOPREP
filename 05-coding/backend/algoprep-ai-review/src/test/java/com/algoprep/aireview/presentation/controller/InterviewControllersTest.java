package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.interview.InterviewHint;
import com.algoprep.aireview.application.dto.interview.InterviewSessionDetail;
import com.algoprep.aireview.application.dto.interview.InterviewSummary;
import com.algoprep.aireview.application.dto.interview.OpenInterviewSessionCommand;
import com.algoprep.aireview.application.dto.interview.OpenedInterviewSession;
import com.algoprep.aireview.application.dto.interview.SendInterviewTurnCommand;
import com.algoprep.aireview.application.dto.interview.TurnAccepted;
import com.algoprep.aireview.application.dto.interview.TurnCompleted;
import com.algoprep.aireview.application.ports.in.interview.CompleteInterviewSessionUseCase;
import com.algoprep.aireview.application.ports.in.interview.GetInterviewSessionUseCase;
import com.algoprep.aireview.application.ports.in.interview.GetMyInterviewSummaryUseCase;
import com.algoprep.aireview.application.ports.in.interview.OpenInterviewSessionUseCase;
import com.algoprep.aireview.application.ports.in.interview.RequestInterviewHintUseCase;
import com.algoprep.aireview.application.ports.in.interview.SendInterviewTurnUseCase;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.exception.AiRateLimitedException;
import com.algoprep.aireview.domain.exception.interview.AiSessionNotFoundException;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.interview.InterviewEntryType;
import com.algoprep.aireview.domain.model.interview.InterviewResult;
import com.algoprep.aireview.domain.model.interview.InterviewSession;
import com.algoprep.aireview.domain.model.interview.InterviewSessionStage;
import com.algoprep.aireview.domain.model.interview.InterviewStage;
import com.algoprep.aireview.domain.model.interview.InterviewTurn;
import com.algoprep.aireview.domain.model.interview.InterviewerLevel;
import com.algoprep.aireview.domain.model.interview.ReplyState;
import com.algoprep.aireview.domain.model.interview.RubricCriterion;
import com.algoprep.aireview.domain.model.interview.RubricScore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Endpoints 8 to 13 of DD api F5: mapping, ownership source, validation and error shaping. */
class InterviewControllersTest {
  private static final Instant T0 = Instant.parse("2026-11-01T10:00:00Z");
  private final UUID user = UUID.randomUUID();
  private final UUID sessionId = UUID.randomUUID();

  private final OpenInterviewSessionUseCase open = mock(OpenInterviewSessionUseCase.class);
  private final GetInterviewSessionUseCase read = mock(GetInterviewSessionUseCase.class);
  private final RequestInterviewHintUseCase hint = mock(RequestInterviewHintUseCase.class);
  private final CompleteInterviewSessionUseCase complete =
      mock(CompleteInterviewSessionUseCase.class);
  private final InterviewSessionController sessions =
      new InterviewSessionController(open, read, hint, complete);

  private JwtAuthenticationToken auth(String... roles) {
    Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(user.toString()).build();
    return new JwtAuthenticationToken(
        jwt, Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
  }

  private InterviewSession session(boolean selfPractice) {
    return new InterviewSession(
        sessionId,
        user,
        selfPractice ? InterviewEntryType.TOPIC_SELECTION : InterviewEntryType.SUBMISSION,
        selfPractice ? null : UUID.randomUUID(),
        null,
        "q",
        selfPractice ? List.of("graphs") : List.of(),
        selfPractice ? InterviewerLevel.MIDDLE : null,
        selfPractice ? 12 : null,
        selfPractice ? true : null,
        0,
        InterviewSessionStage.EXPLAIN,
        1,
        T0,
        null,
        T0);
  }

  private InterviewTurn turn(int index, InterviewTurn.Role role) {
    return new InterviewTurn(
        UUID.randomUUID(), sessionId, index, role, InterviewStage.EXPLAIN, "text " + index, T0);
  }

  private InterviewResult result() {
    List<RubricScore> scores =
        Arrays.stream(RubricCriterion.values())
            .map(c -> new RubricScore(c, new BigDecimal("8.0"), "good " + c))
            .toList();
    return new InterviewResult(sessionId, scores, 6, T0);
  }

  @Test
  void startTakesTheCallerFromTheTokenAndMapsTheOpeningTurn() {
    when(open.open(any()))
        .thenReturn(new OpenedInterviewSession(session(true), turn(1, InterviewTurn.Role.AI)));
    var body =
        new InterviewRequests.StartRequest(
            "TOPIC_SELECTION", null, null, null, List.of("graphs"), "MIDDLE", 12, true);

    var response = sessions.start(auth("ROLE_STUDENT"), body);

    assertThat(response.sessionId()).isEqualTo(sessionId);
    assertThat(response.topics()).containsExactly("graphs");
    assertThat(response.interviewerLevel()).isEqualTo("MIDDLE");
    assertThat(response.openingMessage().role()).isEqualTo("AI");
    var command = org.mockito.ArgumentCaptor.forClass(OpenInterviewSessionCommand.class);
    verify(open).open(command.capture());
    assertThat(command.getValue().callerId()).isEqualTo(user);
    assertThat(command.getValue().category()).isEqualTo(CallerCategory.STUDENT);
  }

  @Test
  void startOfASubmissionSessionOmitsPracticeSettings() {
    when(open.open(any()))
        .thenReturn(new OpenedInterviewSession(session(false), turn(1, InterviewTurn.Role.AI)));
    var body =
        new InterviewRequests.StartRequest(
            "SUBMISSION", UUID.randomUUID().toString(), null, null, null, null, null, null);

    var response = sessions.start(auth("ROLE_INSTRUCTOR"), body);

    assertThat(response.topics()).isNull();
    assertThat(response.interviewerLevel()).isNull();
    assertThat(response.maxTurns()).isNull();
  }

  @Test
  void startWithAnInvalidBodyNeverReachesTheUseCase() {
    var body = new InterviewRequests.StartRequest("NOPE", null, null, null, null, null, null, null);

    assertThatThrownBy(() -> sessions.start(auth("ROLE_STUDENT"), body))
        .isInstanceOf(AiInvalidRequestException.class);
    verifyNoInteractions(open);
  }

  @Test
  void detailMapsSessionTurnsReplyStateAndResult() {
    var withResult =
        new InterviewSessionDetail(
            session(true),
            List.of(turn(1, InterviewTurn.Role.AI), turn(2, InterviewTurn.Role.USER)),
            ReplyState.GENERATING,
            result());
    var withoutResult =
        new InterviewSessionDetail(session(false), List.of(), ReplyState.IDLE, null);
    when(read.get(user, sessionId)).thenReturn(withResult, withoutResult);

    var first = sessions.detail(auth("ROLE_ADMIN"), sessionId.toString());
    var second = sessions.detail(auth("ROLE_ADMIN"), sessionId.toString());

    assertThat(first.replyState()).isEqualTo("GENERATING");
    assertThat(first.turns()).extracting(t -> t.role()).containsExactly("AI", "USER");
    assertThat(first.result().criteria()).hasSize(4);
    assertThat(first.result().isReferenceOnly()).isTrue();
    assertThat(first.result().overallScore()).isEqualByComparingTo("8.0");
    assertThat(first.result().criteria().get(0).weightPercent())
        .isEqualTo(RubricCriterion.CLARITY.weightPercent());
    assertThat(second.result()).isNull();
    assertThat(second.session().topics()).isNull();
  }

  @Test
  void aNonUuidSessionIdIsReportedAsNotFoundWithoutCallingTheUseCase() {
    assertThatThrownBy(() -> sessions.detail(auth("ROLE_STUDENT"), "not-a-uuid"))
        .isInstanceOf(AiSessionNotFoundException.class);
    assertThatThrownBy(() -> sessions.requestHint(auth("ROLE_STUDENT"), "x"))
        .isInstanceOf(AiSessionNotFoundException.class);
    assertThatThrownBy(() -> sessions.complete(auth("ROLE_STUDENT"), "x"))
        .isInstanceOf(AiSessionNotFoundException.class);
    verifyNoInteractions(read, hint, complete);
  }

  @Test
  void hintAndCompleteMapTheirResults() {
    when(hint.hint(user, sessionId))
        .thenReturn(new InterviewHint("think about hashing", InterviewSessionStage.CHALLENGE));
    when(complete.complete(user, sessionId)).thenReturn(result());

    var hintResponse = sessions.requestHint(auth("ROLE_STUDENT"), sessionId.toString());
    var completed = sessions.complete(auth("ROLE_STUDENT"), sessionId.toString());

    assertThat(hintResponse.hint()).isEqualTo("think about hashing");
    assertThat(hintResponse.stage()).isEqualTo("CHALLENGE");
    assertThat(completed.stage()).isEqualTo("COMPLETED");
    assertThat(completed.turnCount()).isEqualTo(6);
  }

  @Test
  void callerCategoryPrefersAdminThenInstructorElseStudent() {
    assertThat(InterviewSessionController.category(auth("ROLE_ADMIN", "ROLE_INSTRUCTOR")))
        .isEqualTo(CallerCategory.ADMIN);
    assertThat(InterviewSessionController.category(auth("ROLE_INSTRUCTOR")))
        .isEqualTo(CallerCategory.INSTRUCTOR);
    assertThat(InterviewSessionController.category(auth("ROLE_STUDENT")))
        .isEqualTo(CallerCategory.STUDENT);
    assertThat(InterviewSessionController.category(auth())).isEqualTo(CallerCategory.STUDENT);
  }

  @Test
  void summaryDefaultsRangeAndLimitAndMapsRubricByCode() {
    var rubric = new EnumMap<RubricCriterion, BigDecimal>(RubricCriterion.class);
    rubric.put(RubricCriterion.CLARITY, new BigDecimal("7.5"));
    var recent =
        new InterviewSummary.Recent(
            sessionId, InterviewEntryType.SUBMISSION, T0, T0, new BigDecimal("7.0"), rubric);
    var summaryUseCase = mock(GetMyInterviewSummaryUseCase.class);
    when(summaryUseCase.summary(user, "all", 5))
        .thenReturn(
            new InterviewSummary(
                1, new BigDecimal("7.0"), RubricCriterion.PUSHBACK_HANDLING, List.of(recent)));
    when(summaryUseCase.summary(user, "7d", 10))
        .thenReturn(new InterviewSummary(0, null, null, List.of()));
    var controller = new InterviewSummaryController(summaryUseCase);

    var defaults = controller.get(auth("ROLE_STUDENT"), null, null);
    var empty = controller.get(auth("ROLE_STUDENT"), "7d", "10");

    assertThat(defaults.weakestCriterionCode()).isEqualTo("PUSHBACK_HANDLING");
    assertThat(defaults.recentSessions().get(0).rubricScores())
        .containsEntry("CLARITY", new BigDecimal("7.5"));
    assertThat(empty.weakestCriterionCode()).isNull();
    assertThat(empty.recentSessions()).isEmpty();
  }

  @Test
  void summaryRejectsOutOfRangeAndNonNumericLimit() {
    var controller = new InterviewSummaryController(mock(GetMyInterviewSummaryUseCase.class));
    for (String bad : List.of("0", "11", "abc", "-1")) {
      assertThatThrownBy(() -> controller.get(auth("ROLE_STUDENT"), "all", bad))
          .as(bad)
          .isInstanceOfSatisfying(
              AiInvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo("AIR-V02"));
    }
  }

  @Test
  void errorAdviceAddsRetryAfterToRateLimitsAndMapsBadGateway() {
    var advice = new InterviewSessionErrorAdvice();

    ResponseEntity<org.springframework.http.ProblemDetail> limited =
        advice.rateLimited(AiRateLimitedException.interviewQuota(10, 120));
    var bad = advice.invalidResponse(new AiInvalidResponseException("schema mismatch"));

    assertThat(limited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(limited.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("120");
    assertThat(limited.getBody().getProperties())
        .containsEntry("errorCode", "AIR-403")
        .containsKey("details");
    assertThat(bad.getStatus()).isEqualTo(502);
    assertThat(bad.getProperties()).containsEntry("errorCode", "AIR-502");
  }

  @Test
  void budgetAdviceShapesForbiddenAndPicksTheUnreadableBodyCodeByUri() {
    var advice = new AiBudgetErrorAdvice();

    assertThat(advice.denied().getStatus()).isEqualTo(403);
    assertThat(advice.denied().getProperties()).containsEntry("errorCode", "AIR-301");
    assertThat(codeFor(advice, "/api/v1/ai/admin/token-budget/limit")).isEqualTo("AIR-V15");
    assertThat(codeFor(advice, "/api/v1/ai/admin/limits-config")).isEqualTo("AIR-V16");
    assertThat(codeFor(advice, "/api/v1/ai/admin/model-prices")).isEqualTo("AIR-V17");
  }

  private static Object codeFor(AiBudgetErrorAdvice advice, String uri) {
    var request = new org.springframework.mock.web.MockHttpServletRequest("PUT", uri);
    return advice.unreadable(request).getProperties().get("errorCode");
  }

  @Test
  void promptAdviceChoosesPublishOrDraftCodeForUnreadableBody() {
    var advice = new PromptTemplateErrorAdvice();
    var publish =
        new org.springframework.mock.web.MockHttpServletRequest(
            "POST", "/api/v1/ai/admin/prompt-templates/publish");
    var draft =
        new org.springframework.mock.web.MockHttpServletRequest(
            "PUT", "/api/v1/ai/admin/prompt-templates/MOCK_INTERVIEW/draft");

    assertThat(advice.unreadable(publish).getProperties()).containsEntry("errorCode", "AIR-V14");
    assertThat(advice.unreadable(draft).getProperties()).containsEntry("errorCode", "AIR-V12");
    assertThat(advice.unreadable(draft).getStatus()).isEqualTo(400);
  }

  // Interview turn stream (endpoint 10)

  private InterviewTurnController turns(SendInterviewTurnUseCase useCase) {
    return new InterviewTurnController(useCase, 60_000L);
  }

  @Test
  void turnStreamsAcceptedThenCompletedAndPassesTheCallerFromTheToken() {
    var useCase = mock(SendInterviewTurnUseCase.class);
    AtomicReference<SendInterviewTurnCommand> seen = new AtomicReference<>();
    doAnswer(
            call -> {
              seen.set(call.getArgument(0));
              var sink =
                  (com.algoprep.aireview.application.ports.out.InterviewTurnSink)
                      call.getArgument(1);
              sink.accepted(new TurnAccepted(sessionId, 3, 4, "EXPLAIN", 3, 8));
              sink.completed(new TurnCompleted(4, "ok", "EXPLAIN", false, 4, 8, false));
              return null;
            })
        .when(useCase)
        .send(any(), any());
    var controller = turns(useCase);

    SseEmitter emitter =
        controller.turn(
            auth("ROLE_STUDENT"), sessionId.toString(), new InterviewTurnRequest("my answer", 2));

    assertThat(emitter).isNotNull();
    assertThat(seen.get()).isNotNull();
    assertThat(seen.get().callerId()).isEqualTo(user);
    assertThat(seen.get().sessionId()).isEqualTo(sessionId);
    assertThat(seen.get().content()).isEqualTo("my answer");
    assertThat(seen.get().expectedTurnCount()).isEqualTo(2);
    controller.destroy();
  }

  @Test
  void aFailureBeforeAcceptIsRethrownAsAnOrdinaryError() {
    var useCase = mock(SendInterviewTurnUseCase.class);
    doThrow(AiRateLimitedException.coolingDown(5)).when(useCase).send(any(), any());
    var controller = turns(useCase);

    assertThatThrownBy(
            () ->
                controller.turn(
                    auth("ROLE_STUDENT"), sessionId.toString(), new InterviewTurnRequest("a", 0)))
        .isInstanceOf(AiRateLimitedException.class);
    controller.destroy();
  }

  @Test
  void aProviderOutageBeforeAcceptStaysA503NotAHungStream() {
    var useCase = mock(SendInterviewTurnUseCase.class);
    doThrow(
            new AiProviderUnavailableException(
                AiProviderUnavailableException.Reason.PROVIDER_ERROR, null))
        .when(useCase)
        .send(any(), any());
    var controller = turns(useCase);

    assertThatThrownBy(
            () ->
                controller.turn(
                    auth("ROLE_STUDENT"), sessionId.toString(), new InterviewTurnRequest("a", 0)))
        .isInstanceOf(AiProviderUnavailableException.class);
    controller.destroy();
  }

  @Test
  void aUseCaseThatEndsWithoutAcceptingFailsInsteadOfWaitingForever() {
    var controller = turns(mock(SendInterviewTurnUseCase.class));

    assertThatThrownBy(
            () ->
                controller.turn(
                    auth("ROLE_STUDENT"), sessionId.toString(), new InterviewTurnRequest("a", 0)))
        .isInstanceOf(IllegalStateException.class);
    controller.destroy();
  }

  @Test
  void turnRejectsABadSessionIdAndABadBodyBeforeStarting() {
    var useCase = mock(SendInterviewTurnUseCase.class);
    var controller = turns(useCase);

    assertThatThrownBy(
            () -> controller.turn(auth("ROLE_STUDENT"), "x", new InterviewTurnRequest("a", 0)))
        .isInstanceOf(AiSessionNotFoundException.class);
    assertThatThrownBy(
            () ->
                controller.turn(
                    auth("ROLE_STUDENT"), sessionId.toString(), new InterviewTurnRequest(" ", 0)))
        .isInstanceOf(AiInvalidRequestException.class);
    verifyNoInteractions(useCase);
    controller.destroy();
  }
}

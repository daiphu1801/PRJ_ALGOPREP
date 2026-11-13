package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.application.dto.SendInterviewTurnCommand;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.exception.AiSessionNotRunningException;
import com.algoprep.aireview.domain.exception.AiTurnCountMismatchException;
import com.algoprep.aireview.domain.model.CallerCategory;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InterviewTurnRequestTest {
  private static final UUID CALLER = UUID.randomUUID();
  private static final UUID SESSION = UUID.randomUUID();

  private static String codeOf(InterviewTurnRequest request) {
    return ((AiInvalidRequestException)
            org.assertj.core.api.Assertions.catchThrowable(
                () -> request.toCommand(CALLER, CallerCategory.STUDENT, SESSION)))
        .errorCode();
  }

  @Test
  void contentIsRequiredNonBlankAndAtMost4000CharactersAsSent() {
    assertThat(codeOf(new InterviewTurnRequest(null, 0))).isEqualTo("AIR-V08");
    assertThat(codeOf(new InterviewTurnRequest("   \n ", 0))).isEqualTo("AIR-V08");
    assertThat(codeOf(new InterviewTurnRequest("x".repeat(4001), 0))).isEqualTo("AIR-V08");
    SendInterviewTurnCommand ok =
        new InterviewTurnRequest(" " + "x".repeat(3999), 0)
            .toCommand(CALLER, CallerCategory.STUDENT, SESSION);
    assertThat(ok.content()).hasSize(4000).startsWith(" ");
  }

  @Test
  void expectedTurnCountIsRequiredAndNotNegative() {
    assertThat(codeOf(new InterviewTurnRequest("a", null))).isEqualTo("AIR-V09");
    assertThat(codeOf(new InterviewTurnRequest("a", -1))).isEqualTo("AIR-V09");
    SendInterviewTurnCommand ok =
        new InterviewTurnRequest("a", 0).toCommand(CALLER, CallerCategory.STUDENT, SESSION);
    assertThat(ok.callerId()).isEqualTo(CALLER);
    assertThat(ok.sessionId()).isEqualTo(SESSION);
    assertThat(ok.expectedTurnCount()).isZero();
  }

  @Test
  void theErrorEnvelopeMapsEachFailureAfterOpenToItsStatusAndCode() {
    var timeout =
        SseTurnSink.envelope(
            new AiProviderUnavailableException(
                AiProviderUnavailableException.Reason.TIMEOUT, null));
    assertThat(timeout).containsEntry("status", 503).containsEntry("errorCode", "AIR-501");
    assertThat(timeout.get("details")).isEqualTo(java.util.Map.of("reason", "TIMEOUT"));
    var invalid = SseTurnSink.envelope(new AiInvalidResponseException("bad"));
    assertThat(invalid).containsEntry("status", 502).containsEntry("errorCode", "AIR-502");
    assertThat(SseTurnSink.envelope(new AiSessionNotRunningException()))
        .containsEntry("status", 409)
        .containsEntry("errorCode", "AIR-111");
    assertThat(SseTurnSink.envelope(new AiTurnCountMismatchException(3)))
        .containsEntry("errorCode", "AIR-112");
    assertThat(SseTurnSink.envelope(new IllegalStateException("boom")))
        .containsEntry("status", 503)
        .containsEntry("errorCode", "AIR-501");
    assertThatThrownBy(
            () ->
                new InterviewTurnRequest("a", null)
                    .toCommand(CALLER, CallerCategory.STUDENT, SESSION))
        .isInstanceOf(AiInvalidRequestException.class);
  }
}

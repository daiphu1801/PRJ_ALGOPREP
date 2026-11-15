package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.dto.InterviewTranscriptExport;
import com.algoprep.identity.application.ports.out.ExportRateLimitPort;
import com.algoprep.identity.application.ports.out.InterviewTranscriptExportPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExportMyInterviewTranscriptsQueryHandlerTest {
  private final ExportRateLimitPort limit = mock(ExportRateLimitPort.class);
  private final InterviewTranscriptExportPort port = mock(InterviewTranscriptExportPort.class);
  private final ExportMyInterviewTranscriptsQueryHandler handler =
      new ExportMyInterviewTranscriptsQueryHandler(limit, port);
  private final UUID user = UUID.randomUUID();

  @Test
  void aFreeSlotReadsTheCallerConversationsAndKeepsTheSlot() {
    var export = new InterviewTranscriptExport(List.of());
    when(limit.tryAcquire(user, "interview-transcripts")).thenReturn(true);
    when(port.readAll(user)).thenReturn(export);

    assertThat(handler.export(user)).isSameAs(export);
    verify(limit, never()).release(user, "interview-transcripts");
  }

  @Test
  void aTakenSlotIsRateLimitedBeforeAnyRead() {
    when(limit.tryAcquire(user, "interview-transcripts")).thenReturn(false);

    assertThatThrownBy(() -> handler.export(user))
        .isInstanceOfSatisfying(
            RateLimitExceededException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-V06"));
    verifyNoInteractions(port);
  }

  @Test
  void aFailureIs503WithoutDetailAndTheSlotIsReleased() {
    when(limit.tryAcquire(user, "interview-transcripts")).thenReturn(true);
    when(port.readAll(user)).thenThrow(new IllegalStateException("ai secret detail"));

    assertThatThrownBy(() -> handler.export(user))
        .isInstanceOfSatisfying(
            ServiceUnavailableException.class,
            e -> {
              assertThat(e.errorCode()).isEqualTo("IDT-509");
              assertThat(e.getMessage()).doesNotContain("secret");
              assertThat(e.getCause()).isNull();
            });
    verify(limit).release(user, "interview-transcripts");
  }
}

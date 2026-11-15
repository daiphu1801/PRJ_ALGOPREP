package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.dto.SubmissionExport;
import com.algoprep.identity.application.ports.out.ExportRateLimitPort;
import com.algoprep.identity.application.ports.out.SubmissionExportPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExportMySubmissionsQueryHandlerTest {
  private final ExportRateLimitPort limit = mock(ExportRateLimitPort.class);
  private final SubmissionExportPort port = mock(SubmissionExportPort.class);
  private final ExportMySubmissionsQueryHandler handler =
      new ExportMySubmissionsQueryHandler(limit, port);
  private final UUID user = UUID.randomUUID();

  @Test
  void aFreeSlotReadsTheCallerRowsWithTheCapAndKeepsTheSlot() {
    var export = new SubmissionExport(List.of(), false);
    when(limit.tryAcquire(user, "submissions")).thenReturn(true);
    when(port.readAll(user, ExportMySubmissionsQueryHandler.MAX_ROWS)).thenReturn(export);

    assertThat(handler.export(user)).isSameAs(export);
    verify(limit, never()).release(user, "submissions");
  }

  @Test
  void aTakenSlotIsRateLimitedBeforeAnyRead() {
    when(limit.tryAcquire(user, "submissions")).thenReturn(false);

    assertThatThrownBy(() -> handler.export(user))
        .isInstanceOfSatisfying(
            RateLimitExceededException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-V06"));
    verify(port, never()).readAll(eq(user), anyInt());
  }

  @Test
  void aJudgeFailureIs503WithoutDetailAndTheSlotIsReleased() {
    when(limit.tryAcquire(user, "submissions")).thenReturn(true);
    when(port.readAll(user, ExportMySubmissionsQueryHandler.MAX_ROWS))
        .thenThrow(new IllegalStateException("judge secret detail"));

    assertThatThrownBy(() -> handler.export(user))
        .isInstanceOfSatisfying(
            ServiceUnavailableException.class,
            e -> {
              assertThat(e.errorCode()).isEqualTo("IDT-508");
              assertThat(e.getMessage()).doesNotContain("secret");
              assertThat(e.getCause()).isNull();
            });
    verify(limit).release(user, "submissions");
  }
}

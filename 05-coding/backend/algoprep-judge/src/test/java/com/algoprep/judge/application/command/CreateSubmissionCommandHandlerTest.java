package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.PendingSubmissionTicket;
import com.algoprep.judge.application.dto.QueueFailureResolution;
import com.algoprep.judge.application.dto.QueuedSubmissionResult;
import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.application.dto.SubmissionFinishedProgress;
import com.algoprep.judge.application.ports.in.QueueSubmissionUseCase;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.PendingSubmission.Program;
import com.algoprep.judge.domain.model.SubmissionLifecycleView;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CreateSubmissionCommandHandlerTest {
  private final QueueSubmissionUseCase queue = mock(QueueSubmissionUseCase.class);
  private final SubmissionProgressPort progress = mock(SubmissionProgressPort.class);
  private final CreateSubmissionCommandHandler handler =
      new CreateSubmissionCommandHandler(queue, progress);
  private final Instant at = Instant.parse("2026-11-03T00:00:00Z");
  private final Owner owner = new Owner(UUID.randomUUID(), UUID.randomUUID());
  private final PendingSubmissionTicket ticket =
      new PendingSubmissionTicket(
          UUID.randomUUID(),
          new PendingSubmissionTicket.ProgramType("PYTHON", "STANDARD_IO"),
          at,
          10);
  private final RecordPendingSubmissionCommand command =
      new RecordPendingSubmissionCommand(
          owner, new Program("PYTHON", "STANDARD_IO", "private", "EDITOR"), null);

  @Test
  void confirmedReturnsThePersistedTicketWithoutAnUncommittedProgressPush() {
    when(queue.queue(command)).thenReturn(new QueuedSubmissionResult(ticket, null));
    assertThat(handler.create(command).ticket()).isSameAs(ticket);
    verifyNoInteractions(progress);
  }

  @ParameterizedTest
  @ValueSource(strings = {"COMPILING", "JUDGING", "ACCEPTED", "SYSTEM_ERROR"})
  void anotherWriterWinsWithoutAFalseFinalNotification(String status) {
    when(queue.queue(command)).thenReturn(result(status, false));
    var actual = handler.create(command);
    assertThat(actual.ticket()).isSameAs(ticket);
    assertThat(actual.currentStatus()).isEqualTo(status);
    verifyNoInteractions(progress);
  }

  @Test
  void onlyTheCommittedQueueFailureWinnerPushesToThePersistedOwnerAndReturns503() {
    UUID persistedOwner = UUID.randomUUID();
    var view =
        new SubmissionLifecycleView(
            ticket.submissionId(),
            new Owner(persistedOwner, owner.problemId()),
            "PYTHON",
            "SYSTEM_ERROR",
            new SubmissionLifecycleView.Timing(at, at.plusSeconds(1)));
    when(queue.queue(command))
        .thenReturn(new QueuedSubmissionResult(ticket, new QueueFailureResolution(view, true)));
    assertThatThrownBy(() -> handler.create(command))
        .isInstanceOfSatisfying(
            SubmissionAdmissionException.class,
            failure -> {
              assertThat(failure.errorCode()).isEqualTo("JDG-503");
              assertThat(failure.clientErrorDetails())
                  .containsEntry("submissionId", ticket.submissionId());
            });
    verify(progress)
        .afterCommit(
            persistedOwner,
            new SubmissionFinishedProgress(
                ticket.submissionId(),
                at.plusSeconds(1),
                "SYSTEM_ERROR",
                null,
                null,
                null,
                null,
                null,
                null,
                at.plusSeconds(1),
                null));
  }

  @Test
  void admissionFailureDoesNotEmitProgress() {
    when(queue.queue(any())).thenThrow(new SubmissionAdmissionException("JDG-104"));
    assertThatThrownBy(() -> handler.create(command))
        .isInstanceOf(SubmissionAdmissionException.class);
    verifyNoInteractions(progress);
  }

  private QueuedSubmissionResult result(String status, boolean finalized) {
    return new QueuedSubmissionResult(
        ticket,
        new QueueFailureResolution(
            new SubmissionLifecycleView(
                ticket.submissionId(),
                owner,
                "PYTHON",
                status,
                new SubmissionLifecycleView.Timing(at, at.plusSeconds(1))),
            finalized));
  }
}

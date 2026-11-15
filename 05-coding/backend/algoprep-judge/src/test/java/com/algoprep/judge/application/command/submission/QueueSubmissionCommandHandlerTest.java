package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.PendingSubmissionTicket;
import com.algoprep.judge.application.dto.PendingSubmissionTicket.ProgramType;
import com.algoprep.judge.application.dto.QueueFailureResolution;
import com.algoprep.judge.application.ports.in.RecordPendingSubmissionUseCase;
import com.algoprep.judge.application.ports.in.ResolveQueueFailureUseCase;
import com.algoprep.judge.application.ports.out.SubmissionQueuePort;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.model.SubmissionLifecycleView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QueueSubmissionCommandHandlerTest {
  @ParameterizedTest
  @ValueSource(strings = {"CONFIRMED", "FAILED", "WORKER", "THREW"})
  void dispatchUsesOnlyTheCommittedIdAndPreservesTheWorkerWinner(String branch) {
    var f = new QueueFailureFixture();
    var admission = mock(RecordPendingSubmissionUseCase.class);
    var queue = mock(SubmissionQueuePort.class);
    var failure = mock(ResolveQueueFailureUseCase.class);
    var command = new SubmissionAdmissionFixture().command();
    var ticket =
        new PendingSubmissionTicket(
            f.id, new ProgramType("PYTHON", "STANDARD_IO"), f.submitted, 10);
    when(admission.recordPending(command)).thenReturn(ticket);
    when(queue.enqueue(f.id)).thenReturn(branch.equals("CONFIRMED"));
    if (branch.equals("THREW")) {
      when(queue.enqueue(f.id)).thenThrow(new IllegalStateException("raw broker data"));
    }
    var current =
        branch.equals("WORKER")
            ? new SubmissionLifecycleView(f.id, f.owner, "PYTHON", "COMPILING", f.view.timing())
            : f.view;
    when(failure.resolve(f.id))
        .thenReturn(new QueueFailureResolution(current, !branch.equals("WORKER")));
    var result = new QueueSubmissionCommandHandler(admission, queue, failure).queue(command);
    assertThat(result.ticket()).isSameAs(ticket);
    var order = inOrder(admission, queue, failure);
    order.verify(admission).recordPending(command);
    order.verify(queue).enqueue(f.id);
    if (branch.equals("CONFIRMED")) {
      assertThat(result.queueUnavailable()).isFalse();
      assertThat(result.currentStatus()).isEqualTo("PENDING");
      verifyNoInteractions(failure);
    } else {
      order.verify(failure).resolve(f.id);
      assertThat(result.queueUnavailable()).isEqualTo(!branch.equals("WORKER"));
      assertThat(result.currentStatus())
          .isEqualTo(branch.equals("WORKER") ? "COMPILING" : "SYSTEM_ERROR");
    }
  }

  @Test
  void rejectedAdmissionDoesNotSendOrFinalizeAnything() {
    var admission = mock(RecordPendingSubmissionUseCase.class);
    var queue = mock(SubmissionQueuePort.class);
    var failure = mock(ResolveQueueFailureUseCase.class);
    var command = new SubmissionAdmissionFixture().command();
    var rejected = new SubmissionAdmissionException("JDG-106");
    when(admission.recordPending(command)).thenThrow(rejected);
    assertThatThrownBy(
            () -> new QueueSubmissionCommandHandler(admission, queue, failure).queue(command))
        .isSameAs(rejected);
    verifyNoInteractions(queue, failure);
  }
}

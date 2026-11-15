package com.algoprep.judge.application.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.SubmissionLifecycleView;
import com.algoprep.judge.domain.model.SubmissionLifecycleView.Timing;
import com.algoprep.judge.domain.ports.out.QueueFailureRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.PlatformTransactionManager;

class QueueFailureFixture {
  final QueueFailureRepository repository = mock(QueueFailureRepository.class);
  final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
  final UUID id = UUID.randomUUID();
  final UUID eventId = UUID.randomUUID();
  final Owner owner = new Owner(UUID.randomUUID(), UUID.randomUUID());
  final Instant submitted = Instant.parse("2026-11-03T03:00:00Z");
  final Instant finished = submitted.plusSeconds(10);
  final SubmissionLifecycleView view =
      new SubmissionLifecycleView(
          id, owner, "PYTHON", "SYSTEM_ERROR", new Timing(submitted, finished));

  QueueFailureFixture() {
    when(repository.finishPending(id)).thenReturn(Optional.of(view));
    when(repository.firstAttempt(view)).thenReturn(true);
    when(ids.newId()).thenReturn(eventId);
  }

  ResolveQueueFailureCommandHandler handler() {
    return new ResolveQueueFailureCommandHandler(repository, ids, manager);
  }
}

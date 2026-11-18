package com.algoprep.judge.application.command.queue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.judge.domain.model.queue.SubmissionLifecycleView;
import com.algoprep.judge.domain.model.queue.SubmissionLifecycleView.Timing;
import com.algoprep.judge.domain.model.submission.PendingSubmission.Owner;
import com.algoprep.judge.domain.ports.out.QueueFailureRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.PlatformTransactionManager;

public class QueueFailureFixture {
  public final QueueFailureRepository repository = mock(QueueFailureRepository.class);
  public final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  public final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
  public final UUID id = UUID.randomUUID();
  public final UUID eventId = UUID.randomUUID();
  public final Owner owner = new Owner(UUID.randomUUID(), UUID.randomUUID());
  public final Instant submitted = Instant.parse("2026-11-03T03:00:00Z");
  public final Instant finished = submitted.plusSeconds(10);
  public final SubmissionLifecycleView view =
      new SubmissionLifecycleView(
          id, owner, "PYTHON", "SYSTEM_ERROR", new Timing(submitted, finished));

  public QueueFailureFixture() {
    when(repository.finishPending(id)).thenReturn(Optional.of(view));
    when(repository.firstAttempt(view)).thenReturn(true);
    when(ids.newId()).thenReturn(eventId);
  }

  ResolveQueueFailureCommandHandler handler() {
    return new ResolveQueueFailureCommandHandler(repository, ids, manager);
  }
}

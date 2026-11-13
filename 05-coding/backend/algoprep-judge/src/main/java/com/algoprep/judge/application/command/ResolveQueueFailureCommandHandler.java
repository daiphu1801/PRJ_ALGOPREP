package com.algoprep.judge.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.judge.application.dto.QueueFailureResolution;
import com.algoprep.judge.application.ports.in.ResolveQueueFailureUseCase;
import com.algoprep.judge.domain.exception.QueueFailureFinalizationUnavailableException;
import com.algoprep.judge.domain.model.SubmissionLifecycleView;
import com.algoprep.judge.domain.ports.out.QueueFailureRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@Service
class ResolveQueueFailureCommandHandler implements ResolveQueueFailureUseCase {
  private final QueueFailureRepository repository;
  private final IdGeneratorPort ids;
  private final TransactionTemplate transaction;

  ResolveQueueFailureCommandHandler(
      QueueFailureRepository repository,
      IdGeneratorPort ids,
      @Lazy PlatformTransactionManager manager) {
    this.repository = repository;
    this.ids = ids;
    transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  @Override
  public QueueFailureResolution resolve(UUID submissionId) {
    Objects.requireNonNull(submissionId);
    try {
      return transaction.execute(status -> finalizePending(submissionId));
    } catch (RuntimeException error) {
      throw new QueueFailureFinalizationUnavailableException();
    }
  }

  private QueueFailureResolution finalizePending(UUID submissionId) {
    var changed = repository.finishPending(submissionId);
    if (changed.isEmpty()) {
      return new QueueFailureResolution(repository.find(submissionId).orElseThrow(), false);
    }
    var submission = changed.orElseThrow();
    UUID eventId = Objects.requireNonNull(ids.newId());
    repository.appendGraded(
        eventId, submissionId, envelope(eventId, submission), submission.timing().finishedAt());
    return new QueueFailureResolution(submission, true);
  }

  private String envelope(UUID eventId, SubmissionLifecycleView submission) {
    var json = JsonMapper.builder().build();
    var payload = json.createObjectNode();
    payload.put("submissionId", submission.submissionId().toString());
    payload.put("userId", submission.owner().userId().toString());
    payload.put("problemId", submission.owner().problemId().toString());
    payload.put("language", submission.language());
    payload.put("verdict", "SYSTEM_ERROR");
    payload.putNull("passedCount");
    payload.putNull("totalCount");
    payload.putNull("passedTestcaseRatio");
    payload.put("submittedAt", submission.timing().submittedAt().toString());
    payload.put("firstAttemptForProblem", repository.firstAttempt(submission));
    var event = json.createObjectNode();
    event.put("eventId", eventId.toString());
    event.put("eventType", "SubmissionGraded");
    event.put("occurredAt", submission.timing().finishedAt().toString());
    event.put("schemaVersion", 1);
    event.set("payload", payload);
    return json.writeValueAsString(event);
  }
}

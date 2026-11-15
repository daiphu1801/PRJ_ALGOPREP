package com.algoprep.judge.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.judge.application.ports.in.ProcessQueuedSubmissionUseCase;
import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import com.algoprep.judge.domain.exception.WorkerPersistenceException;
import com.algoprep.judge.domain.model.WorkerJob;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort.Lease;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProcessQueuedSubmissionCommandHandler implements ProcessQueuedSubmissionUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(ProcessQueuedSubmissionCommandHandler.class);
  private final WorkerJobRepository jobs;
  private final WorkerLeasePort leases;
  private final WorkerAttemptProcessor attempts;
  private final WorkerPersistence persistence;

  ProcessQueuedSubmissionCommandHandler(
      WorkerJobRepository jobs,
      WorkerLeasePort leases,
      WorkerAttemptProcessor attempts,
      WorkerPersistence persistence) {
    this.jobs = jobs;
    this.leases = leases;
    this.attempts = attempts;
    this.persistence = persistence;
  }

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void process(UUID id) {
    var acquired = leases.acquire("judge:sub:" + id);
    if (acquired.isEmpty()) {
      return;
    }
    try (var lease = acquired.orElseThrow()) {
      var found = jobs.find(id);
      if (found.isEmpty()) {
        LOG.error("Queued submission does not exist");
        return;
      }
      var job = found.orElseThrow();
      if (terminal(job.status())) {
        return;
      }
      if (job.hasResults()) {
        persistence.finish(id, FinishKind.SYSTEM_ERROR, "WORKER_TIMEOUT", lease);
      } else {
        attempt(job, lease);
      }
      if (!jobs.find(id).map(s -> terminal(s.status())).orElse(false)) {
        throw new WorkerPersistenceException();
      }
    }
  }

  private void attempt(WorkerJob job, Lease lease) {
    try {
      attempts.process(job, jobs.settings(job.language()), lease);
    } catch (WorkerPersistenceException e) {
      throw e;
    } catch (ProblemGradingReadException e) {
      fail(job, lease, e.reason().name());
    } catch (DomainRuleViolationException e) {
      fail(
          job,
          lease,
          Set.of("HRN-001", "HRN-002", "HRN-004", "HRN-005", "HRN-006", "HRN-050")
                  .contains(e.errorCode())
              ? "SPEC_UNAVAILABLE"
              : "SANDBOX_FAULT");
    } catch (RuntimeException e) {
      fail(job, lease, "SANDBOX_FAULT");
    }
  }

  private void fail(WorkerJob job, Lease lease, String reason) {
    LOG.error("Submission grading ended with a system error");
    persistence.finish(job.id(), FinishKind.SYSTEM_ERROR, reason, lease);
  }

  private boolean terminal(String status) {
    return !Set.of("PENDING", "COMPILING", "JUDGING").contains(status);
  }
}

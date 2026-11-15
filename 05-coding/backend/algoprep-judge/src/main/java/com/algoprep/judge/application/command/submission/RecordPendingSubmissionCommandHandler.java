package com.algoprep.judge.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.judge.application.dto.GradingSpecification;
import com.algoprep.judge.application.dto.PendingSubmissionTicket;
import com.algoprep.judge.application.dto.PendingSubmissionTicket.ProgramType;
import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.application.ports.in.RecordPendingSubmissionUseCase;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.exception.SubmissionAdmissionUnavailableException;
import com.algoprep.judge.domain.model.PendingSubmission;
import com.algoprep.judge.domain.model.PendingSubmission.Program;
import com.algoprep.judge.domain.ports.out.SubmissionAdmissionRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class RecordPendingSubmissionCommandHandler implements RecordPendingSubmissionUseCase {
  private final SubmissionAdmissionRepository repository;
  private final ProblemGradingSpecPort problems;
  private final IdGeneratorPort ids;
  private final int defaultHourlyLimit;
  private final TransactionTemplate transaction;

  RecordPendingSubmissionCommandHandler(
      SubmissionAdmissionRepository repository,
      ProblemGradingSpecPort problems,
      IdGeneratorPort ids,
      @Lazy PlatformTransactionManager manager,
      @Value("${judge.submission.default-hourly-limit:60}") int defaultHourlyLimit) {
    this.repository = repository;
    this.problems = problems;
    this.ids = ids;
    this.defaultHourlyLimit = defaultHourlyLimit;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  @Override
  public PendingSubmissionTicket recordPending(RecordPendingSubmissionCommand command) {
    Program program = SubmissionAdmissionValidation.validate(command);
    try {
      return transaction.execute(status -> insertPending(command, program));
    } catch (SubmissionAdmissionException | ProblemGradingReadException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new SubmissionAdmissionUnavailableException();
    }
  }

  private PendingSubmissionTicket insertPending(
      RecordPendingSubmissionCommand command, Program program) {
    GradingSpecification spec =
        problems
            .loadGradingSpec(command.owner().problemId())
            .orElseThrow(() -> new SubmissionAdmissionException("JDG-101"));
    checkProblem(spec, program);
    int limit =
        spec.limits().maxSubmissionsPerHour() == null
            ? defaultHourlyLimit
            : spec.limits().maxSubmissionsPerHour();
    if (limit <= 0 || spec.versions().currentTestcaseSetVersion() <= 0) {
      throw new SubmissionAdmissionUnavailableException();
    }
    repository.lockPair(command.owner());
    Instant at = repository.databaseNow();
    if (repository.countRecent(command.owner(), at) >= limit) {
      throw new SubmissionAdmissionException(
          "JDG-106", Map.of("limit", limit, "windowMinutes", 60));
    }
    PendingSubmission pending =
        new PendingSubmission(
            Objects.requireNonNull(ids.newId()),
            command.owner(),
            program,
            spec.versions().currentTestcaseSetVersion(),
            at);
    repository.insert(pending);
    return new PendingSubmissionTicket(
        pending.submissionId(),
        new ProgramType(program.language(), program.submissionModel()),
        at,
        spec.testcases().size());
  }

  private void checkProblem(GradingSpecification spec, Program program) {
    if (!spec.problem().published()) {
      throw new SubmissionAdmissionException("JDG-101");
    }
    if (spec.testcases().isEmpty()) {
      throw new SubmissionAdmissionException("JDG-104");
    }
    if (!spec.problem().snapshotReady()) {
      throw new SubmissionAdmissionException("JDG-504");
    }
    if (!repository.languageEnabled(program.language())) {
      throw new SubmissionAdmissionException("JDG-102");
    }
    if (program.submissionModel().equals("FUNCTION_WRAPPER")
        && !spec.problem().functionWrapperSupported()) {
      throw new SubmissionAdmissionException("JDG-103");
    }
  }
}

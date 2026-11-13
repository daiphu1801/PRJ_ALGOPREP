package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.CreateProblemCommand;
import com.algoprep.problembank.application.dto.SaveProblemContentCommand;
import com.algoprep.problembank.application.ports.in.CreateProblemUseCase;
import com.algoprep.problembank.application.ports.in.SaveProblemContentUseCase;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import com.algoprep.problembank.domain.model.CatalogAuditEntry;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * The caller that resolves the limits the write core expects to find explicit (see {@code
 * SaveProblemContentCommandHandler}); it joins the same transaction as the core. With a source
 * problem it also copies the spec and testcases and audits {@code PROBLEM_DUPLICATED}.
 */
@Service
class CreateProblemCommandHandler implements CreateProblemUseCase {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final SaveProblemContentUseCase save;
  private final JudgeConfigReadPort defaults;
  private final ProblemDuplicator duplicator;
  private final ProblemAuditPort audit;

  CreateProblemCommandHandler(
      SaveProblemContentUseCase save,
      JudgeConfigReadPort defaults,
      ProblemDuplicator duplicator,
      ProblemAuditPort audit) {
    this.save = save;
    this.defaults = defaults;
    this.duplicator = duplicator;
    this.audit = audit;
  }

  @Override
  @Transactional
  public ProblemContentSnapshot create(CreateProblemCommand command) {
    var sourceId = command.duplicatedFromProblemId();
    var source =
        sourceId == null ? null : duplicator.source(sourceId, command.actorId(), command.admin());
    var typed = command.limits();
    var base = defaults.getGradingDefaults();
    var limits =
        new ProblemContentData.Limits(
            typed.timeLimitMs() != null ? typed.timeLimitMs() : base.timeLimitMs(),
            typed.memoryLimitMb() != null ? typed.memoryLimitMb() : base.memoryLimitMb(),
            typed.maxOutputSizeKb() != null ? typed.maxOutputSizeKb() : base.maxOutputSizeKb(),
            typed.maxSubmissionsPerHour());
    var c = command.content();
    var content =
        new ProblemContentData(
            c.title(),
            c.statementMd(),
            c.constraintsMd(),
            c.levelId(),
            limits,
            c.topicIds(),
            c.tags(),
            c.workedExamples(),
            c.referenceSolution());
    var created =
        save.save(
            new SaveProblemContentCommand(
                null, command.actorId(), command.admin(), true, null, content));
    if (source != null) {
      duplicator.copy(source, created.id(), command.actorId());
      try {
        audit.record(
            new CatalogAuditEntry(
                command.actorId(),
                "PROBLEM_DUPLICATED",
                "PROBLEM",
                created.id().toString(),
                null,
                JSON.createObjectNode()
                    .put("duplicatedFromProblemId", sourceId.toString())
                    .toString()));
      } catch (RuntimeException unavailable) {
        // Same contract as the status change: a lost audit entry is 503 PRB-050 and a rollback.
        throw new ServiceUnavailableException("PRB-050", "System audit unavailable", null);
      }
    }
    return created;
  }
}

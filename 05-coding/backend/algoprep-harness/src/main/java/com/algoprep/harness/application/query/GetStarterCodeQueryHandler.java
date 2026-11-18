package com.algoprep.harness.application.query;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.harness.application.dto.GetStarterCodeQuery;
import com.algoprep.harness.application.dto.StarterCodeResult;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.GetStarterCodeUseCase;
import com.algoprep.harness.application.ports.out.StarterCodeAccessPort;
import com.algoprep.harness.application.starter.StarterCodeRenderer;
import com.algoprep.harness.infrastructure.template.ClasspathStarterCodeTemplates;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public class GetStarterCodeQueryHandler implements GetStarterCodeUseCase {
  private final StarterCodeAccessPort access;
  private final CurrentProblemSpecification specs;
  private final StarterCodeRenderer renderer = ClasspathStarterCodeTemplates.create();

  public GetStarterCodeQueryHandler(
      StarterCodeAccessPort access, CurrentProblemSpecification specs) {
    this.access = access;
    this.specs = specs;
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public StarterCodeResult getStarterCode(GetStarterCodeQuery query) {
    if (query == null || query.submissionModel() == null) {
      throw new DomainRuleViolationException("HRN-031", "Invalid starter request");
    }
    if (query.language() == null) {
      throw new DomainRuleViolationException("HRN-010", "Missing starter language");
    }
    boolean readable;
    try {
      readable = access.canRead(query.problemId(), query.callerUserId(), query.baseCategory());
    } catch (RuntimeException failure) {
      throw CurrentProblemSpecification.unavailable();
    }
    if (!readable) {
      throw CurrentProblemSpecification.missing();
    }
    var spec = specs.load(query.problemId());
    if (query.submissionModel() == SubmissionModel.FUNCTION_WRAPPER) {
      specs.validateWrapper(spec);
    }
    return new StarterCodeResult(
        query.language(),
        query.submissionModel(),
        renderer.render(query.language(), query.submissionModel(), spec.signature()),
        spec.specVersion());
  }
}

package com.algoprep.harness.application.query;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.common.functionspec.FunctionSpecEvaluator;
import com.algoprep.harness.application.dto.ProblemSpecView;
import com.algoprep.harness.application.ports.out.ProblemSpecReaderPort;
import java.util.Optional;
import java.util.UUID;

/** Shared read policy for P1/P2. No cache, source data or upstream causes in error responses. */
public final class CurrentProblemSpecification {
  private final ProblemSpecReaderPort reader;

  public CurrentProblemSpecification(ProblemSpecReaderPort reader) {
    this.reader = reader;
  }

  public ProblemSpecView load(UUID problemId) {
    if (problemId == null) {
      throw missing();
    }
    Optional<ProblemSpecView> result;
    try {
      result = reader.findSpec(problemId);
    } catch (AlgoPrepException failure) {
      if ("HRN-006".equals(failure.errorCode())) {
        throw invalid();
      }
      throw unavailable();
    } catch (RuntimeException failure) {
      throw unavailable();
    }
    var value = result.orElseThrow(CurrentProblemSpecification::missing);
    if (!problemId.equals(value.problemId())
        || value.specVersion() < 1
        || value.signature() == null) {
      throw invalid();
    }
    return value;
  }

  public void validateWrapper(ProblemSpecView spec) {
    var draft = spec.signature();
    if (draft.functionName() == null || draft.returnType() == null || draft.parameters() == null) {
      throw new DomainRuleViolationException("HRN-005", "Function signature is missing");
    }
    if (!spec.functionWrapperSupported()) {
      throw new DomainRuleViolationException("HRN-004", "Only Standard I/O is supported");
    }
    var evaluation = FunctionSpecEvaluator.evaluate(draft);
    if (!evaluation.violations().isEmpty() || !evaluation.wrapperSupported()) {
      throw invalid();
    }
  }

  public static ResourceNotFoundException missing() {
    return new ResourceNotFoundException("HRN-001", "Problem specification not found");
  }

  public static ServiceUnavailableException unavailable() {
    return new ServiceUnavailableException("HRN-003", "Problem specification is unavailable", null);
  }

  private static DomainRuleViolationException invalid() {
    System.getLogger(CurrentProblemSpecification.class.getName())
        .log(System.Logger.Level.ERROR, "Stored function specification is invalid");
    return new DomainRuleViolationException("HRN-006", "Stored function specification is invalid");
  }
}

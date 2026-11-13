package com.algoprep.bootstrap.problembank;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.harness.application.dto.ProblemSpecView;
import com.algoprep.harness.application.ports.out.ProblemSpecReaderPort;
import com.algoprep.harness.application.ports.out.StarterCodeAccessPort;
import com.algoprep.problembank.application.dto.ProblemSpecReadResult;
import com.algoprep.problembank.application.ports.in.ProblemSpecQuery;
import com.algoprep.problembank.application.ports.in.StarterCodeAccessQuery;
import com.algoprep.problembank.domain.exception.UnreadableProblemSpecException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** F2/harness types meet here. SQL/JSON errors never expose raw source data. */
@Component
public final class HarnessProblemReadBridge
    implements ProblemSpecReaderPort, StarterCodeAccessPort {
  private final ProblemSpecQuery specs;
  private final StarterCodeAccessQuery access;

  public HarnessProblemReadBridge(ProblemSpecQuery specs, StarterCodeAccessQuery access) {
    this.specs = specs;
    this.access = access;
  }

  @Override
  public Optional<ProblemSpecView> findSpec(UUID problemId) {
    Optional<ProblemSpecReadResult> result;
    try {
      result = specs.findSpec(problemId);
    } catch (UnreadableProblemSpecException failure) {
      throw new DomainRuleViolationException("HRN-006", "Stored function specification is invalid");
    } catch (RuntimeException failure) {
      throw unavailable();
    }
    return result.map(this::translate);
  }

  private ProblemSpecView translate(ProblemSpecReadResult value) {
    return new ProblemSpecView(
        value.problemId(),
        value.specVersion(),
        value.signature(),
        value.stdinFormatMd(),
        value.stdoutFormatMd(),
        value.functionWrapperSupported());
  }

  @Override
  public boolean canRead(UUID problemId, UUID callerUserId, String baseCategory) {
    try {
      return access.canRead(problemId, callerUserId, baseCategory);
    } catch (RuntimeException failure) {
      throw unavailable();
    }
  }

  private ServiceUnavailableException unavailable() {
    return new ServiceUnavailableException("HRN-003", "Problem specification is unavailable", null);
  }
}

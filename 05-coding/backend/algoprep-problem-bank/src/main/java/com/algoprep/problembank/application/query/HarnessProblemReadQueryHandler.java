package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.command.ProblemSpecJson;
import com.algoprep.problembank.application.dto.ProblemSpecReadResult;
import com.algoprep.problembank.application.ports.in.ProblemSpecQuery;
import com.algoprep.problembank.application.ports.in.StarterCodeAccessQuery;
import com.algoprep.problembank.domain.exception.UnreadableProblemSpecException;
import com.algoprep.problembank.domain.model.ProblemSpecReadView;
import com.algoprep.problembank.domain.ports.out.HarnessProblemReadRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Direct DB baseline: no stale-cache authorization or version decisions. */
@Service
class HarnessProblemReadQueryHandler implements ProblemSpecQuery, StarterCodeAccessQuery {
  private final HarnessProblemReadRepository repository;

  HarnessProblemReadQueryHandler(HarnessProblemReadRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ProblemSpecReadResult> findSpec(UUID problemId) {
    return problemId == null ? Optional.empty() : repository.findSpec(problemId).map(this::decode);
  }

  private ProblemSpecReadResult decode(ProblemSpecReadView value) {
    try {
      return new ProblemSpecReadResult(
          value.problemId(),
          value.specVersion(),
          ProblemSpecJson.draft(value.data()),
          value.data().stdinFormatMd(),
          value.data().stdoutFormatMd(),
          value.functionWrapperSupported());
    } catch (RuntimeException failure) {
      throw new UnreadableProblemSpecException();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canRead(UUID problemId, UUID callerUserId, String baseCategory) {
    if (problemId == null
        || callerUserId == null
        || baseCategory == null
        || !Set.of("STUDENT", "INSTRUCTOR", "ADMIN").contains(baseCategory)) {
      return false;
    }
    return repository.canRead(problemId, callerUserId, baseCategory);
  }
}

package com.algoprep.problembank.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.ports.in.ReadProblemContextUseCase;
import com.algoprep.problembank.domain.model.ProblemContextView;
import com.algoprep.problembank.domain.ports.out.ProblemContextReadRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Trusted caller (ai-review), so no actor check: visibility is the only gate. */
@Service
class ReadProblemContextQueryHandler implements ReadProblemContextUseCase {
  private final ProblemContextReadRepository repository;

  ReadProblemContextQueryHandler(ProblemContextReadRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public ProblemContextView getContext(UUID problemId, String language) {
    if (problemId == null) {
      throw missing();
    }
    var lang =
        language == null || language.isBlank() ? null : language.trim().toUpperCase(Locale.ROOT);
    return repository
        .findVisible(problemId, lang)
        .orElseThrow(ReadProblemContextQueryHandler::missing);
  }

  private static ResourceNotFoundException missing() {
    return new ResourceNotFoundException("PRB-010", "Problem not found");
  }
}

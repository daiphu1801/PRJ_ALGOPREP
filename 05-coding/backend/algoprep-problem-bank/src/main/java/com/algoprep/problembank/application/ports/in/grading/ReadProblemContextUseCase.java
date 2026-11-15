package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.domain.model.ProblemContextView;
import java.util.UUID;

/** In-process read behind {@code ProblemContextPort.getContext}, DD F2 section 58.1 (AC-83). */
public interface ReadProblemContextUseCase {
  /** Throws {@code PRB-010} when the problem is missing, unpublished or soft-deleted. */
  ProblemContextView getContext(UUID problemId, String language);
}

package com.algoprep.problembank.application.ports.in.problem;

import com.algoprep.problembank.application.dto.problem.ProblemSpecReadResult;
import java.util.Optional;
import java.util.UUID;

/** Trusted internal read for harness; no HTTP route or implicit authorization. */
public interface ProblemSpecQuery {
  Optional<ProblemSpecReadResult> findSpec(UUID problemId);
}

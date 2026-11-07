package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import java.util.Optional;
import java.util.UUID;

/** The caller's solve state from the identity module; empty when it cannot answer in time. */
public interface SolveStatePort {
  Optional<SolveStates> mine(UUID userId);
}

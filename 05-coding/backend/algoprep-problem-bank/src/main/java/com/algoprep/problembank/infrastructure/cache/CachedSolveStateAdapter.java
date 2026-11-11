package com.algoprep.problembank.infrastructure.cache;

import com.algoprep.problembank.application.ports.out.SolveStatePort;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Per-user key; a failed origin read is never cached as an empty successful state. */
@Component
@Primary
class CachedSolveStateAdapter implements SolveStatePort {
  private final ObjectProvider<SolveStatePort> origin;
  private final CatalogReadCache cache;

  CachedSolveStateAdapter(
      @Qualifier("solveStateBridge") ObjectProvider<SolveStatePort> origin,
      CatalogReadCache cache) {
    this.origin = origin;
    this.cache = cache;
  }

  @Override
  public Optional<SolveStates> mine(UUID userId) {
    var value =
        cache.read(
            "problem:solvestate:" + userId,
            SolveStates.class,
            () -> origin.getObject().mine(userId).orElse(null));
    if (value != null && (value.solved() == null || value.attempted() == null)) {
      return origin.getObject().mine(userId);
    }
    return Optional.ofNullable(value);
  }
}

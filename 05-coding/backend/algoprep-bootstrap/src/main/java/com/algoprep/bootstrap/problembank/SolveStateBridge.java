package com.algoprep.bootstrap.problembank;

import com.algoprep.identity.application.ports.in.ListAllMySolveStatesUseCase;
import com.algoprep.identity.domain.model.SolveState;
import com.algoprep.problembank.application.ports.out.SolveStatePort;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reads the caller's solve state from identity in its own transaction and gives up after 2 seconds,
 * so a slow or failing identity read costs the problem list only its solve-state column (DD F2
 * 58.2). ponytail: no Redis cache yet; add the 60 second cache of the DD when this read shows up in
 * a profile.
 */
@Component
public final class SolveStateBridge implements SolveStatePort {
  private final ListAllMySolveStatesUseCase identity;
  private final Duration timeout;

  @Autowired
  public SolveStateBridge(ListAllMySolveStatesUseCase identity) {
    this(identity, Duration.ofSeconds(2));
  }

  SolveStateBridge(ListAllMySolveStatesUseCase identity, Duration timeout) {
    this.identity = identity;
    this.timeout = timeout;
  }

  @Override
  public Optional<SolveStates> mine(UUID userId) {
    var task = new FutureTask<>(() -> identity.listAll(userId));
    Thread.ofVirtual().name("solve-state-read").start(task);
    try {
      var solved = new HashSet<UUID>();
      var attempted = new HashSet<UUID>();
      task.get(timeout.toNanos(), TimeUnit.NANOSECONDS)
          .forEach(
              (problem, state) -> {
                if (state == SolveState.SOLVED) {
                  solved.add(problem);
                } else {
                  attempted.add(problem);
                }
              });
      return Optional.of(new SolveStates(solved, attempted));
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    } catch (ExecutionException | TimeoutException failure) {
      return Optional.empty();
    } finally {
      task.cancel(true);
    }
  }
}

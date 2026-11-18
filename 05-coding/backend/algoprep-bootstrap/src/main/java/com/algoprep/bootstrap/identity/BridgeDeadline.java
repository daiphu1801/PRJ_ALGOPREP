package com.algoprep.bootstrap.identity;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Runs one read of a neighbour module on its own virtual thread with a deadline, handing the
 * security context of the request to the worker. A failure or a missed deadline is one {@link
 * IllegalStateException} without cause, which the identity handler turns into a 503 for that block.
 */
public final class BridgeDeadline {
  private BridgeDeadline() {}

  public static <T> T call(String name, Duration limit, Callable<T> work) {
    var task =
        new FutureTask<>(
            DelegatingSecurityContextCallable.create(work, SecurityContextHolder.getContext()));
    Thread.ofVirtual().name(name).start(task);
    try {
      return task.get(limit.toNanos(), TimeUnit.NANOSECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(name + " was interrupted");
    } catch (ExecutionException | TimeoutException failure) {
      throw new IllegalStateException(name + " is unavailable");
    } finally {
      task.cancel(true);
    }
  }
}

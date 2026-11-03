package com.algoprep.judge.application.command;

import com.algoprep.judge.domain.exception.SampleRunException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class SampleRunBudget {
  private record Cancellation(AtomicBoolean flag, AtomicReference<Runnable> action) {}

  private static final ThreadLocal<Cancellation> CANCELLED = new ThreadLocal<>();
  private final Semaphore slots;
  private final long timeoutMs;

  SampleRunBudget(
      @Value("${judge.run-sample.max-concurrent:8}") int concurrent,
      @Value("${judge.run-sample.timeout-ms:60000}") long timeoutMs) {
    if (concurrent < 1 || concurrent > 8 || timeoutMs < 1 || timeoutMs > 60000) {
      throw new IllegalArgumentException("Invalid sample execution budget");
    }
    // ponytail: one API JVM only; multi-instance deployment requires a shared capacity guard.
    this.slots = new Semaphore(concurrent);
    this.timeoutMs = timeoutMs;
  }

  <T> T call(Callable<T> work) {
    var cancelled = new Cancellation(new AtomicBoolean(), new AtomicReference<>());
    var task =
        new FutureTask<>(
            () -> {
              CANCELLED.set(cancelled);
              try {
                return work.call();
              } finally {
                CANCELLED.remove();
              }
            });
    Thread.startVirtualThread(task);
    try {
      return task.get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException interrupted) {
      cancel(cancelled);
      task.cancel(true);
      Thread.currentThread().interrupt();
      throw new SampleRunException("JDG-501");
    } catch (TimeoutException timeout) {
      cancel(cancelled);
      task.cancel(true);
      throw new SampleRunException("JDG-501");
    } catch (ExecutionException failure) {
      if (failure.getCause() instanceof RuntimeException runtime) {
        throw runtime;
      }
      throw new SampleRunException("JDG-501");
    }
  }

  boolean acquire() {
    check();
    return slots.tryAcquire();
  }

  void release() {
    slots.release();
  }

  static void onCancel(Runnable action) {
    var state = CANCELLED.get();
    if (state != null) {
      state.action().set(action);
      if (state.flag().get()) {
        action.run();
      }
    }
  }

  private static void cancel(Cancellation state) {
    state.flag().set(true);
    var action = state.action().get();
    if (action != null) {
      Thread.startVirtualThread(action);
    }
  }

  static void check() {
    var cancelled = CANCELLED.get();
    if (Thread.currentThread().isInterrupted() || (cancelled != null && cancelled.flag().get())) {
      throw new SampleRunException("JDG-501");
    }
  }
}

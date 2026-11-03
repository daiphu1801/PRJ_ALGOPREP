package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class UserDirectoryBridgeTest {
  private final UUID id = UUID.randomUUID();

  @Test
  void returnsNamesThroughTheF2PortOnAnIsolatedThread() {
    Thread caller = Thread.currentThread();
    var bridge =
        new UserDirectoryBridge(
            ids -> {
              assertThat(Thread.currentThread()).isNotSameAs(caller);
              assertThat(Thread.currentThread().isVirtual()).isTrue();
              assertThat(ids).containsExactly(id);
              return Map.of(id, "Tên hiển thị");
            });
    assertThat(bridge.getDisplayNames(List.of(id, id))).containsEntry(id, "Tên hiển thị");
  }

  @Test
  void emptyAndNullBatchesNeverCallIdentity() {
    var called = new AtomicBoolean();
    var bridge =
        new UserDirectoryBridge(
            ids -> {
              called.set(true);
              return Map.of();
            });
    assertThat(bridge.getDisplayNames(List.of())).isEmpty();
    assertThat(bridge.getDisplayNames(null)).isEmpty();
    assertThat(called).isFalse();
  }

  @Test
  void failureDoesNotLeakSensitiveExceptionOrPreventReadingTheProblem() {
    var bridge =
        new UserDirectoryBridge(
            ids -> {
              throw new IllegalStateException("secret email / jdbc credentials");
            });
    assertThat(bridge.getDisplayNames(List.of(id)).get(id)).isNull();
  }

  @Test
  void timeoutCancelsTheLookupWithoutWaitingForTaskShutdown() throws Exception {
    var entered = new CountDownLatch(1);
    var interrupted = new CountDownLatch(1);
    var bridge =
        new UserDirectoryBridge(
            ids -> {
              entered.countDown();
              try {
                new CountDownLatch(1).await();
              } catch (InterruptedException failure) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
              }
              return Map.of(id, "Late name");
            },
            Duration.ofMillis(100));
    long start = System.nanoTime();
    assertThat(bridge.getDisplayNames(List.of(id))).isEmpty();
    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
    assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(interrupted.await(1, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void callerInterruptionIsPreserved() {
    var bridge = new UserDirectoryBridge(ids -> Map.of(id, "Name"));
    Thread.currentThread().interrupt();
    try {
      assertThat(bridge.getDisplayNames(List.of(id))).isEmpty();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void aBrokenNullResultAlsoDegradesToMissingNames() {
    var bridge = new UserDirectoryBridge(ids -> null);
    assertThat(bridge.getDisplayNames(List.of(id))).isEmpty();
  }
}

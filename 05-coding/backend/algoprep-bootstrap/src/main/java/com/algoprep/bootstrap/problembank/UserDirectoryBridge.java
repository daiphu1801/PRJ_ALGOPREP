package com.algoprep.bootstrap.problembank;

import com.algoprep.identity.application.ports.in.ReadUserDisplayNamesUseCase;
import com.algoprep.problembank.application.ports.out.UserDirectoryPort;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Isolates optional attribution reads from the caller's transaction and caps the wait at 2s. */
@Component
public final class UserDirectoryBridge implements UserDirectoryPort {
  private final ReadUserDisplayNamesUseCase users;
  private final Duration timeout;

  @Autowired
  public UserDirectoryBridge(ReadUserDisplayNamesUseCase users) {
    this(users, Duration.ofSeconds(2));
  }

  UserDirectoryBridge(ReadUserDisplayNamesUseCase users, Duration timeout) {
    this.users = users;
    this.timeout = timeout;
  }

  @Override
  public Map<UUID, String> getDisplayNames(Collection<UUID> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }
    Set<UUID> requested = Set.copyOf(userIds);
    var task = new FutureTask<>(() -> Map.copyOf(users.getDisplayNames(requested)));
    Thread.ofVirtual().name("user-directory-read").start(task);
    try {
      return task.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      return Map.of();
    } catch (ExecutionException | TimeoutException failure) {
      return Map.of();
    } finally {
      task.cancel(true);
    }
  }
}

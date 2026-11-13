package com.algoprep.bootstrap.aireview;

import com.algoprep.aireview.application.ports.out.UserDirectoryPort;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.bootstrap.identity.BridgeDeadline;
import com.algoprep.identity.application.ports.in.ReadUserBaseCategoriesUseCase;
import com.algoprep.identity.application.ports.in.ReadUserDisplayNamesUseCase;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Type translation only: ai-review asks identity for display names and base categories. Each read
 * runs under a 2 second deadline; a failure is one {@link IllegalStateException}, which the admin
 * usage handlers turn into null names and the anomaly job turns into a skipped run.
 */
@Component("aiReviewUserDirectoryBridge")
final class AiUserDirectoryBridge implements UserDirectoryPort {
  private final ReadUserDisplayNamesUseCase names;
  private final ReadUserBaseCategoriesUseCase categories;
  private final Duration limit;

  @Autowired
  AiUserDirectoryBridge(
      ReadUserDisplayNamesUseCase names, ReadUserBaseCategoriesUseCase categories) {
    this(names, categories, Duration.ofSeconds(2));
  }

  AiUserDirectoryBridge(
      ReadUserDisplayNamesUseCase names, ReadUserBaseCategoriesUseCase categories, Duration limit) {
    this.names = names;
    this.categories = categories;
    this.limit = limit;
  }

  @Override
  public Map<UUID, String> getDisplayNames(Collection<UUID> userIds) {
    Set<UUID> ids = Set.copyOf(userIds);
    return BridgeDeadline.call(
        "ai-review-user-names-bridge", limit, () -> names.getDisplayNames(ids));
  }

  @Override
  public Map<UUID, CallerCategory> getBaseCategories(Collection<UUID> userIds) {
    Set<UUID> ids = Set.copyOf(userIds);
    return BridgeDeadline.call(
        "ai-review-user-category-bridge",
        limit,
        () -> {
          Map<UUID, CallerCategory> result = new HashMap<>();
          categories
              .getBaseCategories(ids)
              .forEach((id, category) -> result.put(id, CallerCategory.valueOf(category.name())));
          return result;
        });
  }
}

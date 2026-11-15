package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.ReadUserBaseCategoriesUseCase;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.ports.out.UserBaseCategoriesRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReadUserBaseCategoriesQueryHandler implements ReadUserBaseCategoriesUseCase {
  private final UserBaseCategoriesRepository users;

  ReadUserBaseCategoriesQueryHandler(UserBaseCategoriesRepository users) {
    this.users = users;
  }

  @Override
  @Transactional(readOnly = true, timeout = 2)
  public Map<UUID, BaseCategory> getBaseCategories(Collection<UUID> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }
    Set<UUID> requested = Set.copyOf(userIds);
    return Map.copyOf(users.findBaseCategories(requested));
  }
}

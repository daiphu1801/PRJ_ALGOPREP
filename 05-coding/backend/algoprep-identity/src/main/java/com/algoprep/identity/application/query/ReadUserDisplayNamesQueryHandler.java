package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.ReadUserDisplayNamesUseCase;
import com.algoprep.identity.domain.ports.out.UserDisplayNamesRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReadUserDisplayNamesQueryHandler implements ReadUserDisplayNamesUseCase {
  private final UserDisplayNamesRepository users;

  ReadUserDisplayNamesQueryHandler(UserDisplayNamesRepository users) {
    this.users = users;
  }

  @Override
  @Transactional(readOnly = true, timeout = 2)
  public Map<UUID, String> getDisplayNames(Collection<UUID> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }
    Set<UUID> requested = Set.copyOf(userIds);
    return Map.copyOf(users.findDisplayNames(requested));
  }
}

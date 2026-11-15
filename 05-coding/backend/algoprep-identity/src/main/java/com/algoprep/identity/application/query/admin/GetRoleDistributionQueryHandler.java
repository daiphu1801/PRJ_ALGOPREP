package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.GetRoleDistributionUseCase;
import com.algoprep.identity.domain.model.UserGroupShare;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Four exclusive groups of #63 in fixed order. {@code percent} is rounded here to one decimal (half
 * up) so the client never has to repair rounding; a zero total gives four zeros.
 */
@Service
class GetRoleDistributionQueryHandler implements GetRoleDistributionUseCase {
  private final AdminUserRepository users;

  GetRoleDistributionQueryHandler(AdminUserRepository users) {
    this.users = users;
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserGroupShare> distribution() {
    Map<String, Long> counts = users.groupCounts();
    long total = counts.values().stream().mapToLong(Long::longValue).sum();
    return counts.entrySet().stream()
        .map(e -> new UserGroupShare(e.getKey(), e.getValue(), percent(e.getValue(), total)))
        .toList();
  }

  private static double percent(long count, long total) {
    if (total == 0) {
      return 0.0;
    }
    return BigDecimal.valueOf(count * 100.0)
        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
        .doubleValue();
  }
}

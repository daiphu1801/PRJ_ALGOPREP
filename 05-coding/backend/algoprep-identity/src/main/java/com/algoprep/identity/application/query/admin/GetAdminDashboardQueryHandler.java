package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.GetAdminDashboardUseCase;
import com.algoprep.identity.domain.model.StatSummary;
import com.algoprep.identity.domain.model.UserRetentionGroup;
import com.algoprep.identity.domain.ports.out.AdminDashboardRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Windows are fixed by the BD (ADM0101 sheet 5): 30 days against the 30 before for the active
 * users, 24 hours against the 24 before for password resets, seven daily points for both.
 *
 * <p>ponytail: no 60-second cache (DD 0.8.5); the queries are indexed and only an admin calls them.
 * Add one when the overview is opened by many admins at once.
 */
@Service
class GetAdminDashboardQueryHandler implements GetAdminDashboardUseCase {
  private static final int SPARK_DAYS = 7;
  private static final int RETENTION_MONTHS = 6;

  private final AdminDashboardRepository dashboard;

  GetAdminDashboardQueryHandler(AdminDashboardRepository dashboard) {
    this.dashboard = dashboard;
  }

  @Override
  @Transactional(readOnly = true)
  public StatSummary activeUsers() {
    List<Long> spark = new ArrayList<>(SPARK_DAYS);
    for (int daysAgo = SPARK_DAYS - 1; daysAgo >= 0; daysAgo--) {
      spark.add(dashboard.activeUsersAtDayEnd(daysAgo));
    }
    return StatSummary.of(dashboard.activeUsers(30, 0), dashboard.activeUsers(60, 30), spark);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserRetentionGroup> userRetention() {
    return dashboard.userRetention(RETENTION_MONTHS);
  }

  @Override
  @Transactional(readOnly = true)
  public StatSummary passwordResets() {
    return StatSummary.of(
        dashboard.passwordResets(24, 0),
        dashboard.passwordResets(48, 24),
        dashboard.passwordResetsPerDay(SPARK_DAYS));
  }
}

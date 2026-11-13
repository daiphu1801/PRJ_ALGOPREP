package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.UserRetentionGroup;
import java.util.List;

/** Read-only numbers behind the identity cards of the admin overview (#41, #42, #73). */
public interface AdminDashboardRepository {

  /**
   * Accounts, not deactivated, that were issued a refresh token in the window that starts {@code
   * fromDaysAgo} days before now and ends {@code toDaysAgo} days before now.
   */
  long activeUsers(int fromDaysAgo, int toDaysAgo);

  /** The same count over the 30 days that end with the last moment of the day {@code daysAgo}. */
  long activeUsersAtDayEnd(int daysAgo);

  /** The last {@code months} calendar months, oldest first. */
  List<UserRetentionGroup> userRetention(int months);

  /** Password-reset codes issued between {@code fromHoursAgo} and {@code toHoursAgo} hours ago. */
  long passwordResets(int fromHoursAgo, int toHoursAgo);

  /** Codes issued on each of the last {@code days} calendar days, oldest first, zeros kept. */
  List<Long> passwordResetsPerDay(int days);
}

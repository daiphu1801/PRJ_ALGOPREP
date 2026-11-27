package com.algoprep.identity.application.ports.in.admin;

import com.algoprep.identity.domain.model.admin.StatSummary;
import com.algoprep.identity.domain.model.admin.UserRetentionGroup;
import java.util.List;

/** The identity cards of the admin overview (DD api F1 section 0.8.5): #41, #42 and #73. */
public interface GetAdminDashboardUseCase {

  /** #41: accounts that signed in during the last 30 days. */
  StatSummary activeUsers();

  /** #42: six months of new and returning accounts. */
  List<UserRetentionGroup> userRetention();

  /** #73: reset codes issued in the last 24 hours. */
  StatSummary passwordResets();
}

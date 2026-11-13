package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.ports.in.GetAdminDashboardUseCase;
import com.algoprep.identity.domain.model.StatSummary;
import com.algoprep.identity.domain.model.UserRetentionGroup;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The identity cards of the admin overview, #41, #42 and #73 (DD api F1 section 0.8.5). Any admin
 * may read them; the other blocks of the overview come from the judge and problem-bank modules.
 */
@RestController
@RequestMapping("/api/v1/identity/admin/dashboard")
class AdminDashboardController {
  private final GetAdminDashboardUseCase dashboard;

  AdminDashboardController(GetAdminDashboardUseCase dashboard) {
    this.dashboard = dashboard;
  }

  record RetentionResponse(List<UserRetentionGroup> groups) {}

  @GetMapping("/summary")
  @PreAuthorize("hasRole('ADMIN')")
  StatSummary activeUsers() {
    return dashboard.activeUsers();
  }

  @GetMapping("/user-growth")
  @PreAuthorize("hasRole('ADMIN')")
  RetentionResponse userRetention() {
    return new RetentionResponse(dashboard.userRetention());
  }

  @GetMapping("/password-resets")
  @PreAuthorize("hasRole('ADMIN')")
  StatSummary passwordResets() {
    return dashboard.passwordResets();
  }
}

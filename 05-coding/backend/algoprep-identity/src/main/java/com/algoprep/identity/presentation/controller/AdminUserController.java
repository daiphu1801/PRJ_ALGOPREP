package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.admin.CreateUserAccountCommand;
import com.algoprep.identity.application.dto.admin.ListUsersQuery;
import com.algoprep.identity.application.ports.in.admin.CreateUserAccountUseCase;
import com.algoprep.identity.application.ports.in.admin.GetRoleDistributionUseCase;
import com.algoprep.identity.application.ports.in.admin.GetUserManagementStatsUseCase;
import com.algoprep.identity.application.ports.in.admin.ListUsersUseCase;
import com.algoprep.identity.domain.model.admin.AdminUserItem;
import com.algoprep.identity.domain.model.admin.AdminUserPage;
import com.algoprep.identity.domain.model.admin.UserGroupShare;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin user list, counts and creation, #35, #50, #62 and #63 (DD api F1 section 0.6); the actions
 * on one account, #36 to #38, are in {@link AdminUserActionController}. Layer 1 is {@code
 * ROLE_ADMIN}; the matrix cell is {@code USER_MANAGEMENT:READ} for reads and {@code CREATE} for #50
 * (BD ADM0201, "Quyen su dung"). Denial is {@code 403 IDT-445}.
 */
@RestController
@RequestMapping("/api/v1/identity/admin/users")
class AdminUserController {
  private final ListUsersUseCase list;
  private final GetUserManagementStatsUseCase stats;
  private final GetRoleDistributionUseCase distribution;
  private final CreateUserAccountUseCase create;

  AdminUserController(
      ListUsersUseCase list,
      GetUserManagementStatsUseCase stats,
      GetRoleDistributionUseCase distribution,
      CreateUserAccountUseCase create) {
    this.list = list;
    this.stats = stats;
    this.distribution = distribution;
    this.create = create;
  }

  record CreateRequest(String displayName, String email, String roleId) {}

  record StatsResponse(long pendingVerificationCount) {}

  record DistributionResponse(List<UserGroupShare> items) {}

  @GetMapping
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_MANAGEMENT:READ')")
  AdminUserPage users(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String role,
      @RequestParam(name = "q", required = false) String keyword,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return list.list(new ListUsersQuery(status, role, keyword, page, pageSize));
  }

  @GetMapping("/management-stats")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_MANAGEMENT:READ')")
  StatsResponse stats() {
    return new StatsResponse(stats.pendingVerificationCount());
  }

  @GetMapping("/role-distribution")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_MANAGEMENT:READ')")
  DistributionResponse distribution() {
    return new DistributionResponse(distribution.distribution());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_MANAGEMENT:CREATE')")
  AdminUserItem create(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateRequest body) {
    return create.create(
        new CreateUserAccountCommand(
            UUID.fromString(jwt.getSubject()), body.displayName(), body.email(), body.roleId()));
  }
}

package com.algoprep.identity.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.dto.AuditExport;
import com.algoprep.identity.application.dto.AuditLogPageResult;
import com.algoprep.identity.application.dto.CategoryCount;
import com.algoprep.identity.application.dto.ChangeUserRoleCommand;
import com.algoprep.identity.application.dto.ChangeUserStatusCommand;
import com.algoprep.identity.application.dto.CreateUserAccountCommand;
import com.algoprep.identity.application.dto.ExportAuditLogsQuery;
import com.algoprep.identity.application.dto.ListUsersQuery;
import com.algoprep.identity.application.dto.RestoreAccountCommand;
import com.algoprep.identity.application.dto.SearchAuditLogsQuery;
import com.algoprep.identity.application.dto.UpdateLogRetentionCommand;
import com.algoprep.identity.application.ports.in.ChangeUserRoleUseCase;
import com.algoprep.identity.application.ports.in.ChangeUserStatusUseCase;
import com.algoprep.identity.application.ports.in.CreateUserAccountUseCase;
import com.algoprep.identity.application.ports.in.ExportSystemAuditLogsUseCase;
import com.algoprep.identity.application.ports.in.GetAdminDashboardUseCase;
import com.algoprep.identity.application.ports.in.GetAdminSubmissionBlocksUseCase;
import com.algoprep.identity.application.ports.in.GetAuditCategoryBreakdownUseCase;
import com.algoprep.identity.application.ports.in.GetLogRetentionSettingsUseCase;
import com.algoprep.identity.application.ports.in.GetRoleDistributionUseCase;
import com.algoprep.identity.application.ports.in.GetUserManagementStatsUseCase;
import com.algoprep.identity.application.ports.in.ListActiveAdminsUseCase;
import com.algoprep.identity.application.ports.in.ListUsersUseCase;
import com.algoprep.identity.application.ports.in.ResetUserPasswordUseCase;
import com.algoprep.identity.application.ports.in.RestoreDeactivatedAccountUseCase;
import com.algoprep.identity.application.ports.in.SearchSystemAuditLogsUseCase;
import com.algoprep.identity.application.ports.in.UpdateLogRetentionSettingsUseCase;
import com.algoprep.identity.domain.model.ActiveAdmin;
import com.algoprep.identity.domain.model.AdminUserItem;
import com.algoprep.identity.domain.model.AdminUserPage;
import com.algoprep.identity.domain.model.ExpiredLogPolicy;
import com.algoprep.identity.domain.model.LogRetentionSettings;
import com.algoprep.identity.domain.model.UserGroupShare;
import com.algoprep.identity.domain.model.UserRetentionGroup;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/** F1 admin endpoints: delegation, id parsing, ADMIN-only gate (IDT-447) and CSV export headers. */
class AdminControllersTest {
  private final UUID actor = UUID.randomUUID();
  private final UUID target = UUID.randomUUID();
  private final Jwt jwt =
      Jwt.withTokenValue("t").header("alg", "none").subject(actor.toString()).build();

  private Authentication admin() {
    return new TestingAuthenticationToken(actor.toString(), "n", "ROLE_ADMIN");
  }

  private Authentication student() {
    return new TestingAuthenticationToken(actor.toString(), "n", "ROLE_STUDENT");
  }

  @Test
  void adminOnlyRejectsNullAndNonAdmin() {
    AdminOnly.require(admin());
    assertThatThrownBy(() -> AdminOnly.require(null)).isInstanceOf(PermissionDeniedException.class);
    assertThatThrownBy(() -> AdminOnly.require(student()))
        .isInstanceOf(PermissionDeniedException.class)
        .hasMessage("Admin only");
  }

  @Test
  void classPathParsesUuidAndMapsMalformedTo404() {
    UUID id = UUID.randomUUID();
    assertThat(ClassPath.id(id.toString())).isEqualTo(id);
    assertThatThrownBy(() -> ClassPath.id("not-a-uuid"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void restorationReturnsRestoredTrue() {
    RestoreDeactivatedAccountUseCase uc = mock(RestoreDeactivatedAccountUseCase.class);
    var response =
        new AccountRestorationController(uc).restore(new RestoreAccountCommand("ticket"));
    assertThat(response.restored()).isTrue();
    verify(uc).restore(new RestoreAccountCommand("ticket"));
  }

  @Test
  void dashboardEndpointsWrapUseCaseResults() {
    GetAdminDashboardUseCase uc = mock(GetAdminDashboardUseCase.class);
    var group = new UserRetentionGroup("2026-04", 3, 1);
    when(uc.userRetention()).thenReturn(List.of(group));
    var c = new AdminDashboardController(uc);
    assertThat(c.userRetention().groups()).containsExactly(group);
    c.activeUsers();
    c.passwordResets();
    verify(uc).activeUsers();
    verify(uc).passwordResets();
  }

  @Test
  void submissionDashboardDelegatesEveryBlock() {
    GetAdminSubmissionBlocksUseCase uc = mock(GetAdminSubmissionBlocksUseCase.class);
    var c = new AdminSubmissionDashboardController(uc);
    c.submissionsSummary();
    c.byLanguage();
    c.verdicts();
    c.byDifficulty();
    c.byDay();
    c.byMonth();
    c.topProblems();
    verify(uc).submissionsSummary();
    verify(uc).byLanguage();
    verify(uc).verdicts();
    verify(uc).byDifficulty();
    verify(uc).byDay();
    verify(uc).byMonth();
    verify(uc).topProblems();
  }

  @Test
  void logRetentionReadAndUpdateNormaliseTheDayCount() {
    GetLogRetentionSettingsUseCase get = mock(GetLogRetentionSettingsUseCase.class);
    UpdateLogRetentionSettingsUseCase update = mock(UpdateLogRetentionSettingsUseCase.class);
    when(get.get()).thenReturn(new LogRetentionSettings(90, ExpiredLogPolicy.DELETE));
    when(update.update(any())).thenReturn(new LogRetentionSettings(30, ExpiredLogPolicy.ARCHIVE));
    var c = new AdminLogRetentionController(get, update);
    assertThat(c.read().retentionDays()).isEqualTo(90);
    assertThat(c.read().expiredPolicy()).isEqualTo("DELETE");
    var out = c.edit(jwt, new AdminLogRetentionController.UpdateRequest(30, "ARCHIVE"));
    assertThat(out.expiredPolicy()).isEqualTo("ARCHIVE");

    ArgumentCaptor<UpdateLogRetentionCommand> cap =
        ArgumentCaptor.forClass(UpdateLogRetentionCommand.class);
    c.edit(jwt, new AdminLogRetentionController.UpdateRequest(30L, "DELETE"));
    c.edit(
        jwt,
        new AdminLogRetentionController.UpdateRequest(
            new BigInteger("99999999999999999999"), "DELETE"));
    c.edit(jwt, new AdminLogRetentionController.UpdateRequest("thirty", "DELETE"));
    verify(update, org.mockito.Mockito.times(4)).update(cap.capture());
    assertThat(cap.getAllValues().stream().map(UpdateLogRetentionCommand::retentionDays).toList())
        .containsExactly(30L, 30L, Long.MAX_VALUE, null);
    assertThat(cap.getValue().actorUserId()).isEqualTo(actor);
  }

  @Test
  void userActionsParseIdsAndRejectMalformedUserId() {
    ChangeUserRoleUseCase role = mock(ChangeUserRoleUseCase.class);
    ChangeUserStatusUseCase status = mock(ChangeUserStatusUseCase.class);
    ResetUserPasswordUseCase reset = mock(ResetUserPasswordUseCase.class);
    var c = new AdminUserActionController(role, status, reset);
    UUID roleId = UUID.randomUUID();
    c.role(
        jwt,
        target.toString(),
        new AdminUserActionController.ChangeRoleRequest(roleId.toString(), true));
    verify(role).change(new ChangeUserRoleCommand(actor, target, roleId, true));
    c.role(
        jwt, target.toString(), new AdminUserActionController.ChangeRoleRequest("garbage", null));
    c.role(jwt, target.toString(), new AdminUserActionController.ChangeRoleRequest(null, null));
    verify(role, org.mockito.Mockito.times(2))
        .change(new ChangeUserRoleCommand(actor, target, null, null));
    c.status(
        jwt,
        target.toString(),
        new AdminUserActionController.ChangeStatusRequest("DEACTIVATED", "r", true, false));
    verify(status)
        .change(new ChangeUserStatusCommand(actor, target, "DEACTIVATED", "r", true, false));
    c.resetPassword(jwt, target.toString());
    verify(reset).reset(actor, target);
    assertThatThrownBy(() -> c.resetPassword(jwt, "nope"))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(
            () ->
                c.status(
                    jwt,
                    "nope",
                    new AdminUserActionController.ChangeStatusRequest(null, null, null, null)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void userListingStatsDistributionAndCreate() {
    ListUsersUseCase list = mock(ListUsersUseCase.class);
    GetUserManagementStatsUseCase stats = mock(GetUserManagementStatsUseCase.class);
    GetRoleDistributionUseCase dist = mock(GetRoleDistributionUseCase.class);
    CreateUserAccountUseCase create = mock(CreateUserAccountUseCase.class);
    var page = new AdminUserPage(List.of(), 2, 10, 0);
    when(list.list(any())).thenReturn(page);
    when(stats.pendingVerificationCount()).thenReturn(4L);
    var share = new UserGroupShare("STUDENT", 3, 75.0);
    when(dist.distribution()).thenReturn(List.of(share));
    var c = new AdminUserController(list, stats, dist, create);
    assertThat(c.users("ACTIVE", "STUDENT", "an", 2, 10)).isSameAs(page);
    verify(list).list(new ListUsersQuery("ACTIVE", "STUDENT", "an", 2, 10));
    assertThat(c.stats().pendingVerificationCount()).isEqualTo(4);
    assertThat(c.distribution().items()).containsExactly(share);
    AdminUserItem item = mock(AdminUserItem.class);
    when(create.create(any())).thenReturn(item);
    assertThat(c.create(jwt, new AdminUserController.CreateRequest("An", "a@b.c", "r")))
        .isSameAs(item);
    verify(create).create(new CreateUserAccountCommand(actor, "An", "a@b.c", "r"));
  }

  @Test
  void auditLogEndpointsRequireAdminAndDelegate() {
    SearchSystemAuditLogsUseCase search = mock(SearchSystemAuditLogsUseCase.class);
    ExportSystemAuditLogsUseCase export = mock(ExportSystemAuditLogsUseCase.class);
    ListActiveAdminsUseCase active = mock(ListActiveAdminsUseCase.class);
    GetAuditCategoryBreakdownUseCase breakdown = mock(GetAuditCategoryBreakdownUseCase.class);
    var c = new AuditLogController(search, export, active, breakdown);
    var result = new AuditLogPageResult(List.of(), null);
    var query = new SearchAuditLogsQuery("q", null, null, null, null);
    when(search.search(query)).thenReturn(result);
    assertThat(c.search(admin(), query)).isSameAs(result);
    assertThatThrownBy(() -> c.search(student(), query))
        .isInstanceOf(PermissionDeniedException.class);
    var who = new ActiveAdmin(actor, "A", "Admin", 3, Instant.EPOCH);
    when(active.list()).thenReturn(List.of(who));
    assertThat(c.activeAdmins(admin()).items()).containsExactly(who);
    var cat = new CategoryCount("AUTH", 2);
    when(breakdown.get()).thenReturn(List.of(cat));
    assertThat(c.categoryBreakdown(admin()).items()).containsExactly(cat);
    assertThatThrownBy(() -> c.activeAdmins(null)).isInstanceOf(PermissionDeniedException.class);
    assertThatThrownBy(() -> c.categoryBreakdown(student()))
        .isInstanceOf(PermissionDeniedException.class);
  }

  @Test
  void auditExportStreamsCsvWithAttachmentHeaders() throws Exception {
    SearchSystemAuditLogsUseCase search = mock(SearchSystemAuditLogsUseCase.class);
    ExportSystemAuditLogsUseCase export = mock(ExportSystemAuditLogsUseCase.class);
    Iterator<List<String>> rows = List.of(List.of("a", "b")).iterator();
    when(export.open(any())).thenReturn(new AuditExport(List.of("H1", "H2"), rows));
    var c =
        new AuditLogController(
            search,
            export,
            mock(ListActiveAdminsUseCase.class),
            mock(GetAuditCategoryBreakdownUseCase.class));
    var params =
        new AuditLogController.ExportParams(
            "q", "AUTH", "2026-01-01", "2026-01-31", List.of("H1"), "csv");
    var response = c.export(admin(), params, "vi");
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .startsWith("attachment; filename=\"audit-logs-")
        .endsWith(".csv\"");
    assertThat(response.getHeaders().getContentType().toString()).startsWith("text/csv");
    assertThat(response.getHeaders().getCacheControl()).contains("no-store");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    response.getBody().writeTo(out);
    assertThat(out.toString(java.nio.charset.StandardCharsets.UTF_8)).contains("H1", "a");
    ArgumentCaptor<ExportAuditLogsQuery> cap = ArgumentCaptor.forClass(ExportAuditLogsQuery.class);
    verify(export).open(cap.capture());
    assertThat(cap.getValue().actorUserId()).isEqualTo(actor);
    assertThat(cap.getValue().acceptLanguage()).isEqualTo("vi");
    assertThatThrownBy(() -> c.export(student(), params, null))
        .isInstanceOf(PermissionDeniedException.class);
  }

  @Test
  void auditExportRethrowsWriterFailure() {
    ExportSystemAuditLogsUseCase export = mock(ExportSystemAuditLogsUseCase.class);
    Iterator<List<String>> broken =
        new Iterator<>() {
          public boolean hasNext() {
            throw new IllegalStateException("db down");
          }

          public List<String> next() {
            return List.of();
          }
        };
    when(export.open(any())).thenReturn(new AuditExport(List.of("H"), broken));
    var c =
        new AuditLogController(
            mock(SearchSystemAuditLogsUseCase.class),
            export,
            mock(ListActiveAdminsUseCase.class),
            mock(GetAuditCategoryBreakdownUseCase.class));
    var response =
        c.export(
            admin(), new AuditLogController.ExportParams(null, null, null, null, null, null), null);
    assertThatThrownBy(() -> response.getBody().writeTo(new ByteArrayOutputStream()))
        .isInstanceOf(IllegalStateException.class);
  }
}

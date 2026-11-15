package com.algoprep.identity.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.dto.UserSummary;
import com.algoprep.identity.application.dto.account.ConfirmEmailChangeCommand;
import com.algoprep.identity.application.dto.account.InterviewTranscriptExport;
import com.algoprep.identity.application.dto.account.PendingEmailChange;
import com.algoprep.identity.application.dto.account.PersonalDataExport;
import com.algoprep.identity.application.dto.account.RequestEmailChangeCommand;
import com.algoprep.identity.application.dto.account.SubmissionExport;
import com.algoprep.identity.application.dto.account.UpdateMyProfileCommand;
import com.algoprep.identity.application.dto.account.UpdateMySettingsCommand;
import com.algoprep.identity.application.dto.auth.LoginCommand;
import com.algoprep.identity.application.dto.auth.LoginResult;
import com.algoprep.identity.application.dto.auth.OAuthStart;
import com.algoprep.identity.application.dto.auth.RefreshResult;
import com.algoprep.identity.application.dto.classes.CreatedClass;
import com.algoprep.identity.application.dto.classes.InstructorStudentsQuery;
import com.algoprep.identity.application.dto.classes.JoinClassByCodeCommand;
import com.algoprep.identity.application.dto.classes.UpdateClassCommand;
import com.algoprep.identity.application.dto.password.ChangeMyPasswordCommand;
import com.algoprep.identity.application.dto.password.ResetPasswordCommand;
import com.algoprep.identity.application.dto.password.VerifyPasswordResetOtpCommand;
import com.algoprep.identity.application.dto.rbac.CreateRoleCommand;
import com.algoprep.identity.application.dto.rbac.UpdateRolePermissionCommand;
import com.algoprep.identity.application.dto.registration.RegisterCommand;
import com.algoprep.identity.application.dto.registration.RegistrationResponse;
import com.algoprep.identity.application.dto.registration.VerifyEmailCommand;
import com.algoprep.identity.application.ports.in.account.ConfirmEmailChangeUseCase;
import com.algoprep.identity.application.ports.in.account.DeleteMyAccountUseCase;
import com.algoprep.identity.application.ports.in.account.ExportMyInterviewTranscriptsUseCase;
import com.algoprep.identity.application.ports.in.account.ExportMyProfileDataUseCase;
import com.algoprep.identity.application.ports.in.account.ExportMySubmissionsUseCase;
import com.algoprep.identity.application.ports.in.account.GetMyProfileUseCase;
import com.algoprep.identity.application.ports.in.account.GetMySettingsUseCase;
import com.algoprep.identity.application.ports.in.account.RequestEmailChangeUseCase;
import com.algoprep.identity.application.ports.in.account.UpdateMyProfileUseCase;
import com.algoprep.identity.application.ports.in.account.UpdateMySettingsUseCase;
import com.algoprep.identity.application.ports.in.auth.CompleteOAuthLoginUseCase;
import com.algoprep.identity.application.ports.in.auth.LoginUseCase;
import com.algoprep.identity.application.ports.in.auth.LogoutUseCase;
import com.algoprep.identity.application.ports.in.auth.RefreshSessionUseCase;
import com.algoprep.identity.application.ports.in.auth.StartOAuthLoginUseCase;
import com.algoprep.identity.application.ports.in.classes.CreateClassUseCase;
import com.algoprep.identity.application.ports.in.classes.DeleteClassUseCase;
import com.algoprep.identity.application.ports.in.classes.GenerateClassInviteCodeUseCase;
import com.algoprep.identity.application.ports.in.classes.GetClassProgressUseCase;
import com.algoprep.identity.application.ports.in.classes.GetClassScoreTrendUseCase;
import com.algoprep.identity.application.ports.in.classes.GetClassStudentProfileUseCase;
import com.algoprep.identity.application.ports.in.classes.GetMyClassUseCase;
import com.algoprep.identity.application.ports.in.classes.JoinClassByCodeUseCase;
import com.algoprep.identity.application.ports.in.classes.ListClassAttentionStudentsUseCase;
import com.algoprep.identity.application.ports.in.classes.ListClassInviteCodesUseCase;
import com.algoprep.identity.application.ports.in.classes.ListClassRosterUseCase;
import com.algoprep.identity.application.ports.in.classes.ListClassesSummaryUseCase;
import com.algoprep.identity.application.ports.in.classes.ListInstructorClassesUseCase;
import com.algoprep.identity.application.ports.in.classes.ListInstructorRecentActivityUseCase;
import com.algoprep.identity.application.ports.in.classes.ListInstructorStudentsUseCase;
import com.algoprep.identity.application.ports.in.classes.ListMyClassesUseCase;
import com.algoprep.identity.application.ports.in.classes.RemoveStudentFromClassUseCase;
import com.algoprep.identity.application.ports.in.classes.UpdateClassUseCase;
import com.algoprep.identity.application.ports.in.password.ChangeMyPasswordUseCase;
import com.algoprep.identity.application.ports.in.password.ForgotPasswordUseCase;
import com.algoprep.identity.application.ports.in.password.ResetPasswordUseCase;
import com.algoprep.identity.application.ports.in.password.VerifyPasswordResetOtpUseCase;
import com.algoprep.identity.application.ports.in.progress.GetMyActivityCalendarUseCase;
import com.algoprep.identity.application.ports.in.progress.GetMyProgressSummaryUseCase;
import com.algoprep.identity.application.ports.in.progress.ListMyProblemSolveStatesUseCase;
import com.algoprep.identity.application.ports.in.progress.ListMyRecentActivityUseCase;
import com.algoprep.identity.application.ports.in.rbac.CreateRoleUseCase;
import com.algoprep.identity.application.ports.in.rbac.DeleteRoleUseCase;
import com.algoprep.identity.application.ports.in.rbac.GetPermissionMatrixUseCase;
import com.algoprep.identity.application.ports.in.rbac.ListRolesUseCase;
import com.algoprep.identity.application.ports.in.rbac.UpdateRolePermissionUseCase;
import com.algoprep.identity.application.ports.in.registration.RegisterUseCase;
import com.algoprep.identity.application.ports.in.registration.ResendEmailVerificationUseCase;
import com.algoprep.identity.application.ports.in.registration.VerifyEmailUseCase;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.account.MyProfile;
import com.algoprep.identity.domain.model.account.MySettings;
import com.algoprep.identity.domain.model.classes.ClassActivityItem;
import com.algoprep.identity.domain.model.classes.ClassInviteCode;
import com.algoprep.identity.domain.model.classes.ClassMetricViews.AttentionStudent;
import com.algoprep.identity.domain.model.classes.ClassMetricViews.ClassCard;
import com.algoprep.identity.domain.model.classes.ClassMetricViews.ClassProgress;
import com.algoprep.identity.domain.model.classes.ClassMetricViews.ClassesSummary;
import com.algoprep.identity.domain.model.classes.ClassMetricViews.ScoreTrend;
import com.algoprep.identity.domain.model.classes.ClassMetricViews.StudentPage;
import com.algoprep.identity.domain.model.classes.ClassMetricViews.TrendPoint;
import com.algoprep.identity.domain.model.classes.ClassRef;
import com.algoprep.identity.domain.model.classes.ClassRosterEntry;
import com.algoprep.identity.domain.model.classes.ClassStudentRow;
import com.algoprep.identity.domain.model.classes.ClassStudentStatus;
import com.algoprep.identity.domain.model.classes.InstructorClass;
import com.algoprep.identity.domain.model.classes.MyClass;
import com.algoprep.identity.domain.model.classes.ScheduleSpec;
import com.algoprep.identity.domain.model.classes.StudentMetrics;
import com.algoprep.identity.domain.model.progress.ActivityCalendar;
import com.algoprep.identity.domain.model.progress.ActivityEntry;
import com.algoprep.identity.domain.model.progress.MyProgressSummary;
import com.algoprep.identity.domain.model.progress.SolveState;
import com.algoprep.identity.domain.model.rbac.PermissionMatrix;
import com.algoprep.identity.domain.model.rbac.RoleListItem;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/** F1 student / instructor / self-service endpoints: contract mapping, cookies, id handling. */
class UserControllersTest {
  private final UUID me = UUID.randomUUID();
  private final UUID other = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-05-01T00:00:00Z");
  private final Jwt jwt =
      Jwt.withTokenValue("t").header("alg", "none").subject(me.toString()).build();
  private final Jwt jwtWithSid =
      Jwt.withTokenValue("t")
          .header("alg", "none")
          .subject(me.toString())
          .claim("sid", other.toString())
          .build();
  private final ScheduleSpec schedule =
      new ScheduleSpec(List.of(2), "08:00", "09:00", "A1", null, null, null);
  private final InstructorClass klass = new InstructorClass(other, "Algo", "d", schedule, 5, now);
  private final MyProfile profile =
      new MyProfile(
          me,
          "An",
          "a@b.c",
          "S",
          "C",
          "T",
          "JAVA",
          "09",
          true,
          null,
          List.of("GITHUB"),
          now,
          "Student",
          false);

  // ------------------------------------------------------------ session

  @Test
  void loginSetsHardenedRefreshCookieAndBody() {
    LoginUseCase login = mock(LoginUseCase.class);
    var user = new UserSummary(me, "An", BaseCategory.STUDENT, false);
    when(login.login(any())).thenReturn(new LoginResult("jwt", 900, user, "raw"));
    var c =
        new AuthController(
            login, mock(RefreshSessionUseCase.class), mock(LogoutUseCase.class), true, 30);
    var http = new MockHttpServletRequest();
    http.setRemoteAddr("1.2.3.4");
    ResponseEntity<AuthController.LoginResponse> response =
        c.login(new AuthController.LoginRequest("a@b.c", "pw"), http);
    String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertThat(cookie)
        .contains(
            "refresh_token=raw",
            "HttpOnly",
            "Secure",
            "SameSite=Strict",
            "Path=/api/v1/identity/auth");
    assertThat(response.getBody().accessToken()).isEqualTo("jwt");
    ArgumentCaptor<LoginCommand> cap = ArgumentCaptor.forClass(LoginCommand.class);
    verify(login).login(cap.capture());
    assertThat(cap.getValue().instructorPortal()).isFalse();
    assertThat(cap.getValue().sourceIp()).isEqualTo("1.2.3.4");
    c.instructorLogin(new AuthController.LoginRequest("a@b.c", "pw"), http);
    verify(login, org.mockito.Mockito.times(2)).login(cap.capture());
    assertThat(cap.getValue().instructorPortal()).isTrue();
  }

  @Test
  void refreshDemandsCustomHeaderAndRotatesCookie() {
    RefreshSessionUseCase refresh = mock(RefreshSessionUseCase.class);
    when(refresh.refresh("old")).thenReturn(new RefreshResult("jwt2", 900, "new"));
    var c =
        new AuthController(mock(LoginUseCase.class), refresh, mock(LogoutUseCase.class), false, 7);
    assertThatThrownBy(() -> c.refresh(null, "old")).isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> c.refresh("  ", "old")).isInstanceOf(InvalidRequestException.class);
    var ok = c.refresh("XMLHttpRequest", "old");
    assertThat(ok.getBody().accessToken()).isEqualTo("jwt2");
    assertThat(ok.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
        .contains("refresh_token=new", "Max-Age=604800")
        .doesNotContain("Secure");
  }

  @Test
  void logoutClearsCookie() {
    LogoutUseCase logout = mock(LogoutUseCase.class);
    var c =
        new AuthController(
            mock(LoginUseCase.class), mock(RefreshSessionUseCase.class), logout, true, 30);
    var response = c.logout("tok");
    assertThat(response.getStatusCode().value()).isEqualTo(204);
    assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
        .contains("refresh_token=;", "Max-Age=0");
    verify(logout).logout("tok");
  }

  @Test
  void oauthRedirectAndCallback() {
    StartOAuthLoginUseCase start = mock(StartOAuthLoginUseCase.class);
    CompleteOAuthLoginUseCase complete = mock(CompleteOAuthLoginUseCase.class);
    var begin = new OAuthStart("https://idp/auth", "st");
    when(start.start("github")).thenReturn(begin);
    var user = new UserSummary(me, "An", BaseCategory.STUDENT, false);
    when(complete.complete("github", "code", "st"))
        .thenReturn(new LoginResult("jwt", 900, user, "raw"));
    var c = new OAuthController(start, complete, true, 30);
    assertThat(c.redirect("github")).isSameAs(begin);
    var response = c.callback("github", "code", "st");
    assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
        .contains("refresh_token=raw", "HttpOnly");
    assertThat(response.getBody().user()).isEqualTo(user);
  }

  @Test
  void registrationAndPasswordResetFlows() {
    RegisterUseCase register = mock(RegisterUseCase.class);
    VerifyEmailUseCase verify = mock(VerifyEmailUseCase.class);
    ResendEmailVerificationUseCase resend = mock(ResendEmailVerificationUseCase.class);
    var reg = new RegistrationController(register, verify, resend);
    var command = new RegisterCommand("a@b.c", "pw", "An");
    when(register.register(command)).thenReturn(new RegistrationResponse(me, "a@b.c"));
    assertThat(reg.register(command).userId()).isEqualTo(me);
    assertThat(reg.verify(new VerifyEmailCommand("a@b.c", "123456")).message())
        .contains("Xác thực");
    assertThat(reg.resend(new RegistrationController.ResendRequest("a@b.c")).message())
        .isNotBlank();
    verify(resend).resend("a@b.c");

    ForgotPasswordUseCase forgot = mock(ForgotPasswordUseCase.class);
    VerifyPasswordResetOtpUseCase otp = mock(VerifyPasswordResetOtpUseCase.class);
    ResetPasswordUseCase reset = mock(ResetPasswordUseCase.class);
    var pw = new PasswordResetController(forgot, otp, reset);
    org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("smtp down"))
        .when(forgot)
        .forgot("a@b.c");
    assertThat(pw.forgot(new PasswordResetController.ForgotRequest("a@b.c")).message())
        .contains("Nếu email");
    assertThat(pw.verify(new VerifyPasswordResetOtpCommand("a@b.c", "1")).verified()).isTrue();
    assertThat(pw.reset(new ResetPasswordCommand("a@b.c", "1", "new")).message())
        .contains("thành công");
    verify(reset).reset(new ResetPasswordCommand("a@b.c", "1", "new"));
  }

  // ------------------------------------------------------------ me

  @Test
  void profileGetPatchAndPasswordChange() {
    GetMyProfileUseCase get = mock(GetMyProfileUseCase.class);
    UpdateMyProfileUseCase update = mock(UpdateMyProfileUseCase.class);
    when(get.get(me)).thenReturn(profile);
    when(update.update(any())).thenReturn(profile);
    var c = new MyProfileController(get, update);
    var response = c.get(jwt);
    assertThat(response.passwordChangedAt()).isNull();
    assertThat(response.joinedAt()).isEqualTo(now.toString());
    assertThat(response.linkedProviders()).containsExactly("GITHUB");
    c.patch(jwt, new MyProfileController.UpdateProfileRequest("An", "S", "C", "T", "09"));
    verify(update).update(new UpdateMyProfileCommand(me, "An", "S", "C", "T", "09"));
    var withChange =
        new MyProfile(
            me, "An", "a@b.c", null, null, null, "JAVA", null, true, now, List.of(), now, "Student",
            true);
    assertThat(MyProfileController.ProfileResponse.of(withChange).passwordChangedAt())
        .isEqualTo(now.toString());

    ChangeMyPasswordUseCase change = mock(ChangeMyPasswordUseCase.class);
    when(change.change(any())).thenReturn(true);
    var pw = new MyPasswordController(change);
    var out = pw.change(jwtWithSid, new MyPasswordController.ChangePasswordRequest("old", "new"));
    assertThat(out.noticeEmailFailed()).isTrue();
    verify(change).change(new ChangeMyPasswordCommand(me, other, "old", "new"));
    assertThatThrownBy(
            () -> pw.change(jwt, new MyPasswordController.ChangePasswordRequest("o", "n")))
        .isInstanceOf(InvalidBearerTokenException.class);
  }

  @Test
  void settingsEmailAndAccountDeletion() {
    GetMySettingsUseCase get = mock(GetMySettingsUseCase.class);
    UpdateMySettingsUseCase update = mock(UpdateMySettingsUseCase.class);
    var settings = new MySettings(true, false, "MIDDLE", 12, true, "JAVA");
    when(get.get(me)).thenReturn(settings);
    when(update.update(any())).thenReturn(settings);
    var c = new MySettingsController(get, update);
    assertThat(c.get(jwt)).isSameAs(settings);
    c.patch(
        jwt,
        new MySettingsController.UpdateSettingsRequest(true, false, "MIDDLE", 12, true, "JAVA"));
    verify(update).update(new UpdateMySettingsCommand(me, true, false, "MIDDLE", 12, true, "JAVA"));

    RequestEmailChangeUseCase request = mock(RequestEmailChangeUseCase.class);
    ConfirmEmailChangeUseCase confirm = mock(ConfirmEmailChangeUseCase.class);
    var pending = new PendingEmailChange("n@b.c", 600);
    when(request.request(new RequestEmailChangeCommand(me, "n@b.c"))).thenReturn(pending);
    when(confirm.confirm(new ConfirmEmailChangeCommand(me, "123456"))).thenReturn("n@b.c");
    var email = new MyEmailController(request, confirm);
    assertThat(email.request(jwt, new MyEmailController.RequestChangeBody("n@b.c")))
        .isSameAs(pending);
    assertThat(email.confirm(jwt, new MyEmailController.ConfirmBody("123456")).email())
        .isEqualTo("n@b.c");

    DeleteMyAccountUseCase delete = mock(DeleteMyAccountUseCase.class);
    var account = new MyAccountController(delete);
    assertThat(account.delete(jwt, new MyAccountController.DeleteRequest("a@b.c")).message())
        .contains("30 ngày");
    verify(delete).delete(me, "a@b.c");
  }

  @Test
  void progressEndpointsMapDatesAndStates() {
    GetMyProgressSummaryUseCase summary = mock(GetMyProgressSummaryUseCase.class);
    ListMyRecentActivityUseCase activity = mock(ListMyRecentActivityUseCase.class);
    GetMyActivityCalendarUseCase calendar = mock(GetMyActivityCalendarUseCase.class);
    ListMyProblemSolveStatesUseCase states = mock(ListMyProblemSolveStatesUseCase.class);
    var overall = new MyProgressSummary(3, 10, 5, 50, 2, "JAVA");
    when(summary.get(me)).thenReturn(overall);
    when(activity.list(me))
        .thenReturn(
            List.of(new ActivityEntry(other, me, null, "SUBMITTED", "PROBLEM", other, now)));
    when(calendar.get(me))
        .thenReturn(
            new ActivityCalendar(
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-02"),
                List.of(new ActivityCalendar.Day(LocalDate.parse("2026-04-01"), 2))));
    when(states.list(me, "p1")).thenReturn(Map.of(other, SolveState.SOLVED));
    var c = new MyProgressController(summary, activity, calendar, states);
    assertThat(c.summary(jwt)).isSameAs(overall);
    assertThat(c.recentActivity(jwt).items())
        .singleElement()
        .extracting("occurredAt")
        .isEqualTo(now.toString());
    var cal = c.activityCalendar(jwt);
    assertThat(cal.from()).isEqualTo("2026-04-01");
    assertThat(cal.days()).singleElement().extracting("submissionCount").isEqualTo(2);
    assertThat(c.problemSolveStates(jwt, "p1").states()).containsEntry(other, SolveState.SOLVED);
  }

  @Test
  void exportScopesAndHeaders() {
    ExportMyProfileDataUseCase exportProfile = mock(ExportMyProfileDataUseCase.class);
    ExportMySubmissionsUseCase exportSubs = mock(ExportMySubmissionsUseCase.class);
    ExportMyInterviewTranscriptsUseCase exportTr = mock(ExportMyInterviewTranscriptsUseCase.class);
    var settings = new MySettings(true, false, "MIDDLE", 12, true, "JAVA");
    when(exportProfile.export(me)).thenReturn(new PersonalDataExport(profile, settings));
    var row = new SubmissionExport.Row(other, now, "P1", "Two sum", "JAVA", "ACCEPTED", 3, 3, null);
    when(exportSubs.export(me)).thenReturn(new SubmissionExport(List.of(row), true));
    when(exportTr.export(me)).thenReturn(new InterviewTranscriptExport(List.of()));
    var c = new MyExportController(exportProfile, exportSubs, exportTr);

    var json = c.export(jwt, "profile");
    assertThat(json.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("algoprep-profile.json");
    assertThat(((MyExportController.PersonalDataResponse) json.getBody()).settings())
        .isSameAs(settings);

    var csv = c.export(jwt, "submissions");
    assertThat(csv.getHeaders().getFirst("X-Export-Truncated")).isEqualTo("true");
    assertThat(csv.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("algoprep-submissions.csv");
    String body = new String((byte[]) csv.getBody(), StandardCharsets.UTF_8);
    assertThat(body).contains("submission_id", "Two sum", "ACCEPTED");

    when(exportSubs.export(me)).thenReturn(new SubmissionExport(List.of(), false));
    assertThat(c.export(jwt, "submissions").getHeaders().getFirst("X-Export-Truncated")).isNull();

    assertThatThrownBy(() -> c.export(jwt, "other")).isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> c.export(jwt, null)).isInstanceOf(InvalidRequestException.class);

    var transcripts = c.interviewTranscripts(jwt);
    assertThat(transcripts.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("transcripts.json");
    assertThat(transcripts.getHeaders().getCacheControl()).contains("no-store");
    assertThat(transcripts.getBody()).isEmpty();
  }

  // ------------------------------------------------------------ classes

  @Test
  void studentClassEndpoints() {
    JoinClassByCodeUseCase join = mock(JoinClassByCodeUseCase.class);
    ListMyClassesUseCase list = mock(ListMyClassesUseCase.class);
    GetMyClassUseCase get = mock(GetMyClassUseCase.class);
    when(join.join(new JoinClassByCodeCommand(me, "ABC"))).thenReturn(new ClassRef(other, "Algo"));
    var mine = new MyClass(other, "Algo", "", null, "T", now);
    when(list.list(me)).thenReturn(List.of(mine));
    when(get.get(me, other.toString())).thenReturn(mine);
    var c = new StudentClassController(join, list, get);
    assertThat(c.join(jwt, new StudentClassController.JoinRequest("ABC")).className())
        .isEqualTo("Algo");
    assertThat(c.list(jwt).items()).containsExactly(mine);
    assertThat(c.get(jwt, other.toString())).isSameAs(mine);
  }

  @Test
  void instructorClassCrud() {
    ListInstructorClassesUseCase list = mock(ListInstructorClassesUseCase.class);
    CreateClassUseCase create = mock(CreateClassUseCase.class);
    UpdateClassUseCase update = mock(UpdateClassUseCase.class);
    DeleteClassUseCase delete = mock(DeleteClassUseCase.class);
    var code = new ClassInviteCode(me, "CODE", now.plusSeconds(60));
    when(list.list(me)).thenReturn(List.of(klass));
    when(create.create(any())).thenReturn(new CreatedClass(klass, code));
    when(update.update(any())).thenReturn(klass);
    var c = new InstructorClassController(list, create, update, delete);
    assertThat(c.mine(jwt).items())
        .singleElement()
        .extracting("createdAt")
        .isEqualTo(now.toString());
    var created = c.create(jwt, new InstructorClassController.ClassRequest("Algo", "d", schedule));
    assertThat(created.classId()).isEqualTo(other);
    assertThat(created.inviteCode().code()).isEqualTo("CODE");
    assertThat(
            c.patch(
                    jwt,
                    other.toString(),
                    new InstructorClassController.ClassRequest("Algo", "d", null))
                .studentCount())
        .isEqualTo(5);
    verify(update).update(new UpdateClassCommand(me, other, "Algo", "d", null));
    c.delete(jwt, other.toString());
    verify(delete).delete(me, other);
    assertThatThrownBy(() -> c.delete(jwt, "bad")).isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(
            () -> c.patch(jwt, "bad", new InstructorClassController.ClassRequest(null, null, null)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private ClassStudentRow row(Double completion, boolean withMetrics, Instant lastSubmitted) {
    var entry = new ClassRosterEntry(other, "An", "a@b.c", "09", now);
    StudentMetrics metrics =
        withMetrics
            ? new StudentMetrics(
                other,
                lastSubmitted,
                0.5,
                4,
                List.of(new StudentMetrics.WeekPoint(LocalDate.parse("2026-04-20"), 0.8)),
                List.of(
                    new StudentMetrics.AssignmentOutcome(
                        UUID.randomUUID(), now, BigDecimal.ONE, "ACCEPTED", true),
                    new StudentMetrics.AssignmentOutcome(
                        UUID.randomUUID(), now.plusSeconds(10), null, null, false)))
            : null;
    return new ClassStudentRow(
        me, "Algo", entry, metrics, ClassStudentStatus.values()[0], completion, 7.5);
  }

  @Test
  void membershipEndpointsMapRosterFigures() {
    GenerateClassInviteCodeUseCase generate = mock(GenerateClassInviteCodeUseCase.class);
    ListClassInviteCodesUseCase codes = mock(ListClassInviteCodesUseCase.class);
    ListClassRosterUseCase roster = mock(ListClassRosterUseCase.class);
    RemoveStudentFromClassUseCase remove = mock(RemoveStudentFromClassUseCase.class);
    var code = new ClassInviteCode(me, "CODE", now);
    when(generate.generate(me, other)).thenReturn(code);
    when(codes.list(me, other)).thenReturn(List.of(code));
    when(roster.list(me, other))
        .thenReturn(List.of(row(0.5, true, now), row(null, true, null), row(0.2, false, null)));
    when(remove.remove(me, other, me)).thenReturn(now);
    var c = new ClassMembershipController(generate, codes, roster, remove);
    assertThat(c.generate(jwt, other.toString()).code()).isEqualTo("CODE");
    assertThat(c.codes(jwt, other.toString()).items()).hasSize(1);
    var students = c.students(jwt, other.toString()).items();
    assertThat(students).hasSize(3);
    assertThat(students.get(0).completionPercent()).isEqualTo(50);
    assertThat(students.get(0).lastSubmittedAt()).isEqualTo(now.toString());
    assertThat(students.get(1).completionPercent()).isNull();
    assertThat(students.get(1).lastSubmittedAt()).isNull();
    assertThat(students.get(2).completionPercent()).isNull();
    var left = c.leave(jwt, other.toString(), me.toString());
    assertThat(left.leftAt()).isEqualTo(now.toString());
    assertThatThrownBy(() -> c.leave(jwt, other.toString(), "bad"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void studentMetricsProfileSortsAssignmentsNewestFirstAndProgressFillsEveryStatus() {
    GetClassStudentProfileUseCase profileUc = mock(GetClassStudentProfileUseCase.class);
    GetClassProgressUseCase progressUc = mock(GetClassProgressUseCase.class);
    var r = row(0.5, true, now);
    when(profileUc.get(me, other, me)).thenReturn(r);
    Map<ClassStudentStatus, Integer> counts = new EnumMap<>(ClassStudentStatus.class);
    for (ClassStudentStatus s : ClassStudentStatus.values()) {
      counts.put(s, 1);
    }
    when(progressUc.get(me, other))
        .thenReturn(new ClassProgress(other, "Algo", 5, 40, 6.5, counts));
    var c = new ClassStudentMetricsController(profileUc, progressUc);
    var detail = c.profile(jwt, other.toString(), me.toString());
    assertThat(detail.assignmentProgress()).hasSize(2);
    assertThat(detail.assignmentProgress().get(0).bestVerdict()).isNull();
    assertThat(detail.assignmentProgress().get(1).bestVerdict()).isEqualTo("ACCEPTED");
    assertThat(detail.className()).isEqualTo("Algo");
    var progress = c.progress(jwt, other.toString());
    assertThat(progress.statusCounts())
        .hasSize(ClassStudentStatus.values().length)
        .containsValue(1);
    assertThat(progress.completionPercent()).isEqualTo(40);
  }

  @Test
  void overviewEndpointsMapCardsTrendAttentionAndStudentPage() {
    ListClassesSummaryUseCase summary = mock(ListClassesSummaryUseCase.class);
    GetClassScoreTrendUseCase trend = mock(GetClassScoreTrendUseCase.class);
    ListClassAttentionStudentsUseCase attention = mock(ListClassAttentionStudentsUseCase.class);
    ListInstructorStudentsUseCase students = mock(ListInstructorStudentsUseCase.class);
    when(summary.summary(me))
        .thenReturn(new ClassesSummary(1, 5, 60, 1, List.of(new ClassCard(klass, 60, 7.0, 1))));
    when(trend.trend(me, null))
        .thenReturn(
            new ScoreTrend(
                null, null, List.of(new TrendPoint(LocalDate.parse("2026-04-20"), 7.5))));
    var needs =
        new AttentionStudent(other, me, "An", ClassStudentStatus.values()[0], "NO_SUBMISSION", 7);
    when(attention.list(me, other)).thenReturn(List.of(needs));
    when(students.list(any()))
        .thenReturn(
            new StudentPage(List.of(row(0.5, true, now), row(0.5, true, null)), 2, 10, 12, 2));
    var c = new InstructorClassOverviewController(summary, trend, attention, students);
    var s = c.summary(jwt);
    assertThat(s.items()).singleElement().extracting("absentCount").isEqualTo(1);
    assertThat(c.scoreTrend(jwt, null).points())
        .singleElement()
        .extracting("weekStart")
        .isEqualTo("2026-04-20");
    assertThat(c.attention(jwt, other.toString()).items()).containsExactly(needs);
    var page = c.students(jwt, other.toString(), "an", 2, 10);
    assertThat(page.page().totalCount()).isEqualTo(12);
    assertThat(page.items().get(0).lastSubmittedAt()).isEqualTo(now.toString());
    assertThat(page.items().get(1).lastSubmittedAt()).isNull();
    ArgumentCaptor<InstructorStudentsQuery> cap =
        ArgumentCaptor.forClass(InstructorStudentsQuery.class);
    verify(students).list(cap.capture());
    assertThat(cap.getValue().classId()).isEqualTo(other);
    assertThat(cap.getValue().keyword()).isEqualTo("an");
    assertThatThrownBy(() -> c.attention(jwt, "bad")).isInstanceOf(ResourceNotFoundException.class);
    when(trend.trend(me, other)).thenReturn(new ScoreTrend(other, "Algo", List.of()));
    assertThat(c.scoreTrend(jwt, other.toString()).className()).isEqualTo("Algo");
    verify(trend).trend(eq(me), eq(other));
  }

  @Test
  void instructorActivityMapsItemsAndScopesByClass() {
    ListInstructorRecentActivityUseCase uc = mock(ListInstructorRecentActivityUseCase.class);
    var item = new ClassActivityItem(me, "SUBMITTED", other, "An", other, "PROBLEM", me, now);
    when(uc.list(me, other)).thenReturn(List.of(item));
    var c = new InstructorActivityController(uc);
    assertThat(c.recentActivity(jwt, other.toString()).items())
        .singleElement()
        .extracting("occurredAt")
        .isEqualTo(now.toString());
    assertThat(c.recentActivity(jwt, null).items()).isEmpty();
    verify(uc).list(me, null);
  }

  // ------------------------------------------------------------ rbac

  @Test
  void rbacEndpointsParseIds() {
    GetPermissionMatrixUseCase matrix = mock(GetPermissionMatrixUseCase.class);
    UpdateRolePermissionUseCase update = mock(UpdateRolePermissionUseCase.class);
    ListRolesUseCase roles = mock(ListRolesUseCase.class);
    CreateRoleUseCase create = mock(CreateRoleUseCase.class);
    DeleteRoleUseCase delete = mock(DeleteRoleUseCase.class);
    var m = new PermissionMatrix(List.of(), List.of(), List.of(), List.of());
    when(matrix.get()).thenReturn(m);
    var item = new RoleListItem(other, "C", "N", "STUDENT", false, 0);
    when(roles.list()).thenReturn(List.of(item));
    when(create.create(any())).thenReturn(item);
    var c = new RbacController(matrix, update, roles, create, delete);
    assertThat(c.matrix()).isSameAs(m);
    assertThat(c.roles().items()).containsExactly(item);
    assertThat(c.createRole(jwt, new RbacController.CreateRoleRequest("N", "STUDENT")))
        .isSameAs(item);
    verify(create).create(new CreateRoleCommand(me, "N", "STUDENT"));
    c.update(
        jwt,
        other.toString(),
        me.toString(),
        other.toString(),
        new RbacController.UpdateCellRequest(true));
    verify(update).update(new UpdateRolePermissionCommand(me, other, me, other, true));
    c.deleteRole(jwt, other.toString());
    verify(delete).delete(me, other);
    assertThatThrownBy(() -> c.deleteRole(jwt, "bad"))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}

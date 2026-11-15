package com.algoprep.judge.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.judge.application.dto.AdminQueueFlag;
import com.algoprep.judge.application.dto.ExecutionLimitsView;
import com.algoprep.judge.application.dto.MySubmissionMetricQuery;
import com.algoprep.judge.application.dto.MySubmissionMetrics;
import com.algoprep.judge.application.dto.QueueSettingsChange;
import com.algoprep.judge.application.dto.QueuedSubmissionResult;
import com.algoprep.judge.application.dto.SampleRun;
import com.algoprep.judge.application.dto.SubmissionReads;
import com.algoprep.judge.application.ports.in.CreateSubmissionUseCase;
import com.algoprep.judge.application.ports.in.GetAdminLanguageConfigUseCase;
import com.algoprep.judge.application.ports.in.GetAdminQueueSettingsUseCase;
import com.algoprep.judge.application.ports.in.GetEffectiveExecutionLimitsUseCase;
import com.algoprep.judge.application.ports.in.GetInfraSummaryUseCase;
import com.algoprep.judge.application.ports.in.GetInstructorSubmissionDetailUseCase;
import com.algoprep.judge.application.ports.in.GetMySubmissionMetricsUseCase;
import com.algoprep.judge.application.ports.in.GetOwnedSubmissionDetailUseCase;
import com.algoprep.judge.application.ports.in.GetQueueSnapshotUseCase;
import com.algoprep.judge.application.ports.in.GetSubmissionProgressUseCase;
import com.algoprep.judge.application.ports.in.ListInfraEventsUseCase;
import com.algoprep.judge.application.ports.in.ListInstructorSubmissionsUseCase;
import com.algoprep.judge.application.ports.in.ListMyProblemSubmissionsUseCase;
import com.algoprep.judge.application.ports.in.ListMySubmissionHistoryUseCase;
import com.algoprep.judge.application.ports.in.ListQueueJobsUseCase;
import com.algoprep.judge.application.ports.in.ListStudentClassSubmissionsUseCase;
import com.algoprep.judge.application.ports.in.RunSampleUseCase;
import com.algoprep.judge.application.ports.in.SetAdminQueueAutoscaleUseCase;
import com.algoprep.judge.application.ports.in.SetAdminQueuePausedUseCase;
import com.algoprep.judge.application.ports.in.UpdateAdminLanguageConfigUseCase;
import com.algoprep.judge.application.ports.in.UpdateAdminQueueSettingsUseCase;
import com.algoprep.judge.domain.exception.AdminConfigException;
import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import com.algoprep.judge.domain.exception.MySubmissionMetricsUnavailableException;
import com.algoprep.judge.domain.exception.SampleRunException;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.model.AdminLanguageConfig;
import com.algoprep.judge.domain.model.AdminQueueSettings;
import com.algoprep.judge.domain.model.SubmissionMetricRange;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Controller and error-advice contracts of the judge API: RBAC, validation and error mapping. */
class JudgeControllersTest {
  private final UUID subject = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final Jwt jwt =
      Jwt.withTokenValue("t").header("alg", "none").subject(subject.toString()).build();
  private final JwtAuthenticationToken caller = new JwtAuthenticationToken(jwt);
  private final tools.jackson.databind.json.JsonMapper json =
      tools.jackson.databind.json.JsonMapper.builder().build();

  private static String preAuthorize(Class<?> type, String method) {
    for (var m : type.getDeclaredMethods()) {
      if (m.getName().equals(method) && m.isAnnotationPresent(PreAuthorize.class)) {
        return m.getAnnotation(PreAuthorize.class).value();
      }
    }
    return type.getAnnotation(PreAuthorize.class).value();
  }

  @Test
  void rbacExpressionsMatchTheContract() {
    assertThat(preAuthorize(CreateSubmissionController.class, "create"))
        .isEqualTo("hasRole('STUDENT')");
    assertThat(preAuthorize(RunSampleController.class, "run")).isEqualTo("hasRole('STUDENT')");
    assertThat(preAuthorize(MySubmissionReadController.class, "detail"))
        .isEqualTo("hasRole('STUDENT')");
    assertThat(preAuthorize(MySubmissionMetricsController.class, "read"))
        .isEqualTo("hasRole('STUDENT')");
    assertThat(preAuthorize(InstructorSubmissionReadController.class, "list"))
        .isEqualTo("hasAuthority('CLASS_MANAGEMENT:READ')");
    assertThat(preAuthorize(AdminQueueControlController.class, "update"))
        .isEqualTo("hasRole('ADMIN')");
    assertThat(preAuthorize(AdminQueueControlController.class, "read"))
        .isEqualTo("hasAuthority('JUDGE_QUEUE_MONITOR:READ')");
    assertThat(preAuthorize(AdminLanguageConfigController.class, "update"))
        .isEqualTo("hasRole('ADMIN') and hasAuthority('LANGUAGE_CONFIG:UPDATE')");
    assertThat(preAuthorize(AdminLanguageConfigController.class, "read"))
        .isEqualTo("hasRole('ADMIN') and hasAuthority('LANGUAGE_CONFIG:READ')");
    assertThat(preAuthorize(QueueMonitorController.class, "jobs"))
        .isEqualTo("hasAuthority('JUDGE_QUEUE_MONITOR:READ')");
    assertThat(preAuthorize(InfraEventsController.class, "events"))
        .isEqualTo("hasAuthority('JUDGE_QUEUE_MONITOR:READ')");
  }

  // ---- create submission ----

  @Test
  void createSubmissionMapsBodyToCommandAndTicket() {
    var useCase = mock(CreateSubmissionUseCase.class);
    var result = mock(QueuedSubmissionResult.class, RETURNS_DEEP_STUBS);
    var id = UUID.randomUUID();
    var at = Instant.parse("2026-11-01T10:00:00Z");
    when(result.currentStatus()).thenReturn("PENDING");
    when(result.ticket().submissionId()).thenReturn(id);
    when(result.ticket().programType().language()).thenReturn("JAVA");
    when(result.ticket().programType().submissionModel()).thenReturn("STANDARD_IO");
    when(result.ticket().submittedAt()).thenReturn(at);
    when(result.ticket().totalCountHint()).thenReturn(5);
    when(useCase.create(any())).thenReturn(result);
    var body =
        json.readTree(
            "{\"language\":\"JAVA\",\"submissionModel\":\"STANDARD_IO\",\"sourceCode\":\"x\","
                + "\"sourceInputMethod\":7,\"fileName\":\"a.java\"}");
    var ticket = new CreateSubmissionController(useCase).create(caller, problem.toString(), body);
    assertThat(ticket.submissionId()).isEqualTo(id);
    assertThat(ticket.status()).isEqualTo("PENDING");
    assertThat(ticket.totalCountHint()).isEqualTo(5);
    var captor =
        org.mockito.ArgumentCaptor.forClass(
            com.algoprep.judge.application.dto.RecordPendingSubmissionCommand.class);
    verify(useCase).create(captor.capture());
    assertThat(captor.getValue().owner().userId()).isEqualTo(subject);
    assertThat(captor.getValue().owner().problemId()).isEqualTo(problem);
    assertThat(captor.getValue().program().language()).isEqualTo("JAVA");
    assertThat(captor.getValue().program().sourceInputMethod()).isNull();
  }

  @Test
  void createSubmissionRejectsMalformedProblemId() {
    var controller = new CreateSubmissionController(mock(CreateSubmissionUseCase.class));
    assertThatThrownBy(() -> controller.create(caller, "not-a-uuid", json.readTree("{}")))
        .isInstanceOfSatisfying(
            SubmissionAdmissionException.class,
            e -> assertThat(e.errorCode()).isEqualTo("JDG-101"));
  }

  // ---- run sample ----

  @Test
  void runSampleDelegatesAndLimitsValidateProblemId() {
    var runs = mock(RunSampleUseCase.class);
    var limits = mock(GetEffectiveExecutionLimitsUseCase.class);
    var result = mock(SampleRun.Result.class);
    var view = mock(ExecutionLimitsView.class);
    when(runs.run(any())).thenReturn(result);
    when(limits.get(problem, "JAVA")).thenReturn(view);
    var controller = new RunSampleController(runs, limits);
    assertThat(
            controller.run(
                caller,
                problem.toString(),
                json.readTree("{\"language\":\"JAVA\",\"sourceCode\":1}")))
        .isSameAs(result);
    assertThat(controller.limits(problem.toString(), "JAVA")).isSameAs(view);
    assertThatThrownBy(() -> controller.limits("bad", null))
        .isInstanceOf(SubmissionAdmissionException.class);
  }

  @Test
  void runSampleAdviceMapsAdmissionAndQuotaErrors() {
    var advice = new RunSampleErrorAdvice();
    assertThat(advice.invalid(new SubmissionAdmissionException("JDG-101")).getStatus())
        .isEqualTo(404);
    assertThat(advice.invalid(new SubmissionAdmissionException("JDG-102")).getStatus())
        .isEqualTo(422);
    assertThat(advice.invalid(new SubmissionAdmissionException("JDG-105")).getStatus())
        .isEqualTo(422);
    var other = advice.invalid(new SubmissionAdmissionException("JDG-104"));
    assertThat(other.getStatus()).isEqualTo(400);
    assertThat(other.getProperties()).containsEntry("errorCode", "JDG-104");
    var malformed = advice.malformed();
    assertThat(malformed.getStatus()).isEqualTo(400);
    assertThat(malformed.getProperties()).containsEntry("errorCode", "JDG-V01");
    var unavailable = mock(SampleRunException.class);
    when(unavailable.errorCode()).thenReturn("JDG-501");
    when(unavailable.clientErrorDetails()).thenReturn(java.util.Map.of());
    var down = advice.unavailable(unavailable);
    assertThat(down.getStatus()).isEqualTo(503);
    assertThat(down.getProperties()).doesNotContainKey("details");
    var limited = mock(SampleRunException.class);
    when(limited.errorCode()).thenReturn("JDG-502");
    when(limited.clientErrorDetails()).thenReturn(java.util.Map.of("retryAfter", 5));
    var rate = advice.unavailable(limited);
    assertThat(rate.getStatus()).isEqualTo(429);
    assertThat(rate.getProperties()).containsKey("details");
  }

  // ---- my submission read ----

  @Test
  void studentReadsDelegateWithOwnerAndValidatedIds() {
    var details = mock(GetOwnedSubmissionDetailUseCase.class);
    var progress = mock(GetSubmissionProgressUseCase.class);
    var recent = mock(ListMyProblemSubmissionsUseCase.class);
    var history = mock(ListMySubmissionHistoryUseCase.class);
    var controller = new MySubmissionReadController(details, progress, recent, history);
    var id = UUID.randomUUID();
    var detail = mock(SubmissionReads.Detail.class);
    when(details.get(subject, id)).thenReturn(detail);
    assertThat(controller.detail(caller, id.toString())).isSameAs(detail);
    var prog = mock(SubmissionReads.Progress.class);
    when(progress.getProgress(subject, id)).thenReturn(prog);
    assertThat(controller.progress(caller, id.toString())).isSameAs(prog);
    when(recent.recent(subject, problem, 10)).thenReturn(List.of());
    assertThat(controller.recent(caller, problem.toString(), null).items()).isEmpty();
    controller.recent(caller, problem.toString(), "3");
    verify(recent).recent(subject, problem, 3);
    controller.history(caller, "q", "ACCEPTED", "JAVA", "2");
    verify(history)
        .history(subject, new SubmissionReads.Filters("q", "ACCEPTED", "JAVA", "2", null));
    assertThatThrownBy(() -> controller.detail(caller, "x"))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-110"));
    assertThatThrownBy(() -> controller.recent(caller, "x", null))
        .isInstanceOf(ResourceNotFoundException.class);
    for (String bad : new String[] {"0", "11", "abc"}) {
      assertThatThrownBy(() -> controller.recent(caller, problem.toString(), bad))
          .isInstanceOf(InvalidRequestException.class);
    }
  }

  @Test
  void instructorReadsDelegateWithOwnerAndValidatedIds() {
    var list = mock(ListInstructorSubmissionsUseCase.class);
    var detail = mock(GetInstructorSubmissionDetailUseCase.class);
    var history = mock(ListStudentClassSubmissionsUseCase.class);
    var controller = new InstructorSubmissionReadController(list, detail, history);
    var clazz = UUID.randomUUID();
    var student = UUID.randomUUID();
    controller.list(caller, clazz.toString(), "failed", "q", "1");
    verify(list).list(subject, new SubmissionReads.Filters("q", "failed", null, "1", clazz));
    controller.list(caller, null, null, null, null);
    verify(list).list(subject, new SubmissionReads.Filters(null, null, null, null, null));
    controller.history(caller, clazz.toString(), student.toString(), "2");
    verify(history).classHistory(subject, clazz, student, "2");
    assertThatThrownBy(() -> controller.list(caller, "bad", null, null, null))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-111"));
    assertThatThrownBy(() -> controller.history(caller, clazz.toString(), "bad", null))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-112"));
    assertThatThrownBy(() -> controller.detail(caller, "bad"))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-110"));
  }

  @Test
  void instructorResponseFlattensMembershipAndRedactsToString() {
    var result = mock(SubmissionReads.InstructorDetail.class, RETURNS_DEEP_STUBS);
    var student = UUID.randomUUID();
    var left = Instant.parse("2026-11-02T00:00:00Z");
    when(result.detail().status()).thenReturn("ACCEPTED");
    when(result.detail().sourceCode()).thenReturn("secret");
    when(result.member().studentId()).thenReturn(student);
    when(result.member().leftAt()).thenReturn(left);
    var response = InstructorSubmissionReadController.InstructorResponse.of(result);
    assertThat(response.status()).isEqualTo("ACCEPTED");
    assertThat(response.studentId()).isEqualTo(student);
    assertThat(response.leftClass()).isTrue();
    assertThat(response.toString()).doesNotContain("secret").contains("redacted");
    when(result.member().leftAt()).thenReturn(null);
    assertThat(InstructorSubmissionReadController.InstructorResponse.of(result).leftClass())
        .isFalse();
  }

  @Test
  void readAdviceNeverLeaksInfrastructureMessages() {
    var advice = new SubmissionReadErrorAdvice();
    assertThat(advice.forbidden().getStatus()).isEqualTo(403);
    assertThat(advice.forbidden().getProperties()).containsEntry("errorCode", "JDG-301");
    var down = advice.unavailable();
    assertThat(down.getStatus()).isEqualTo(503);
    assertThat(down.getDetail()).isEqualTo("Submission read unavailable");
  }

  // ---- metrics ----

  @Test
  void metricsParsesRangeAndTopicIds() {
    var useCase = mock(GetMySubmissionMetricsUseCase.class);
    var result = mock(MySubmissionMetrics.class);
    when(result.streakDays()).thenReturn(4);
    when(result.dailyCounts()).thenReturn(List.of());
    when(useCase.read(any())).thenReturn(result);
    var controller = new MySubmissionMetricsController(useCase);
    var topic = UUID.randomUUID();
    var response = controller.read("7d", List.of(topic.toString(), topic.toString()));
    assertThat(response.streakDays()).isEqualTo(4);
    verify(useCase)
        .read(new MySubmissionMetricQuery(SubmissionMetricRange.SEVEN_DAYS, Set.of(topic)));
    controller.read("30d", null);
    verify(useCase).read(new MySubmissionMetricQuery(SubmissionMetricRange.THIRTY_DAYS, Set.of()));
    controller.read("all", List.of());
    verify(useCase).read(new MySubmissionMetricQuery(SubmissionMetricRange.ALL, Set.of()));
  }

  @Test
  void metricsRejectsBadRangeBadTopicAndTooManyTopics() {
    var controller = new MySubmissionMetricsController(mock(GetMySubmissionMetricsUseCase.class));
    assertThatThrownBy(() -> controller.read(null, null))
        .isInstanceOfSatisfying(
            InvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-V07"));
    assertThatThrownBy(() -> controller.read("1y", null))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> controller.read("all", List.of("nope")))
        .isInstanceOfSatisfying(
            InvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-V15"));
    var many =
        java.util.stream.IntStream.range(0, 51)
            .mapToObj(i -> UUID.randomUUID().toString())
            .toList();
    assertThatThrownBy(() -> controller.read("all", many))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void metricsAdviceMapsErrors() {
    var advice = new MySubmissionMetricsErrorAdvice();
    var unavailable = advice.unavailable();
    assertThat(unavailable.getStatus()).isEqualTo(503);
    assertThat(unavailable.getProperties()).containsEntry("errorCode", "JDG-507");
    var invalid = advice.invalid(new InvalidRequestException("JDG-V07", "x"));
    assertThat(invalid.getStatus()).isEqualTo(400);
    assertThat(invalid.getProperties()).containsEntry("errorCode", "JDG-V07");
    assertThat(advice.denied().getStatus()).isEqualTo(403);
    assertThat(new MySubmissionMetricsUnavailableException()).isInstanceOf(RuntimeException.class);
  }

  // ---- admin queue ----

  private AdminQueueControlController queueController(
      GetAdminQueueSettingsUseCase reader,
      SetAdminQueuePausedUseCase pause,
      SetAdminQueueAutoscaleUseCase auto,
      UpdateAdminQueueSettingsUseCase settings) {
    return new AdminQueueControlController(reader, pause, auto, settings);
  }

  @Test
  void adminQueueEndpointsParseStrictBodies() {
    var reader = mock(GetAdminQueueSettingsUseCase.class);
    var pause = mock(SetAdminQueuePausedUseCase.class);
    var auto = mock(SetAdminQueueAutoscaleUseCase.class);
    var settings = mock(UpdateAdminQueueSettingsUseCase.class);
    var at = Instant.parse("2026-11-01T10:00:00Z");
    var current = new AdminQueueSettings(1, 2, 3, 4, at);
    when(reader.read()).thenReturn(current);
    when(settings.update(eq(subject), any())).thenReturn(current);
    when(pause.setPaused(subject, false, true)).thenReturn(new AdminQueueFlag(true, at));
    when(auto.setAutoscale(subject, true, false)).thenReturn(new AdminQueueFlag(false, at));
    var controller = queueController(reader, pause, auto, settings);
    assertThat(controller.read()).isSameAs(current);
    assertThat(
            controller.update(
                jwt,
                "{\"autoscaleJobThreshold\":1,\"jobsPerPage\":2,\"errorRetentionHours\":3,\"latencyCapSeconds\":4}"))
        .isSameAs(current);
    verify(settings).update(subject, new QueueSettingsChange(1, 2, 3, 4));
    var paused = controller.paused(jwt, "{\"expectedPaused\":false,\"paused\":true}");
    assertThat(paused.paused()).isTrue();
    assertThat(paused.updatedAt()).isEqualTo(at);
    var enabled = controller.autoscale(jwt, "{\"expectedEnabled\":true,\"enabled\":false}");
    assertThat(enabled.enabled()).isFalse();
  }

  @Test
  void adminQueueRejectsMalformedBodies() {
    var controller =
        queueController(
            mock(GetAdminQueueSettingsUseCase.class),
            mock(SetAdminQueuePausedUseCase.class),
            mock(SetAdminQueueAutoscaleUseCase.class),
            mock(UpdateAdminQueueSettingsUseCase.class));
    for (String bad :
        new String[] {"{\"expectedPaused\":\"no\",\"paused\":true}", "[]", "{", "{} {}"}) {
      assertThatThrownBy(() -> controller.paused(jwt, bad))
          .isInstanceOfSatisfying(
              AdminConfigException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-V16"));
    }
    assertThatThrownBy(() -> controller.paused(jwt, null)).isInstanceOf(AdminConfigException.class);
    assertThatThrownBy(() -> controller.autoscale(jwt, "{\"expectedEnabled\":true}"))
        .isInstanceOf(AdminConfigException.class);
    assertThatThrownBy(() -> controller.update(jwt, "{\"autoscaleJobThreshold\":1.5}"))
        .isInstanceOfSatisfying(
            AdminConfigException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-V09"));
    assertThatThrownBy(() -> controller.update(jwt, "{\"autoscaleJobThreshold\":99999999999}"))
        .isInstanceOf(AdminConfigException.class);
    assertThatThrownBy(() -> controller.update(jwt, null)).isInstanceOf(AdminConfigException.class);
  }

  @Test
  void adminQueueAdviceMapsErrors() {
    var advice = new AdminQueueControlErrorAdvice();
    var invalid = advice.invalid(new AdminConfigException(409, "JDG-409"));
    assertThat(invalid.getStatus()).isEqualTo(409);
    assertThat(invalid.getProperties()).containsEntry("errorCode", "JDG-409");
    assertThat(advice.denied().getProperties()).containsEntry("errorCode", "JDG-301");
    var down = advice.unavailable();
    assertThat(down.getStatus()).isEqualTo(503);
    assertThat(down.getProperties()).isNull();
  }

  // ---- language config ----

  @Test
  void languageConfigReadAndUpdate() {
    var reader = mock(GetAdminLanguageConfigUseCase.class);
    var writer = mock(UpdateAdminLanguageConfigUseCase.class);
    var config = mock(AdminLanguageConfig.class);
    when(reader.read()).thenReturn(config);
    var controller = new AdminLanguageConfigController(reader, writer);
    assertThat(controller.read()).isSameAs(config);
    assertThatThrownBy(() -> controller.update(jwt, "{not json"))
        .isInstanceOfSatisfying(
            AdminConfigException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-V10"));
    assertThatThrownBy(() -> controller.update(jwt, "{}")).isInstanceOf(AdminConfigException.class);
  }

  @Test
  void languageConfigAdviceMapsErrors() {
    var advice = new AdminLanguageConfigErrorAdvice();
    assertThat(advice.invalid(new AdminConfigException(422, "JDG-V11")).getStatus()).isEqualTo(422);
    assertThat(advice.malformed().getProperties()).containsEntry("errorCode", "JDG-V10");
    assertThat(advice.denied().getStatus()).isEqualTo(403);
    var down = advice.unavailable();
    assertThat(down.getStatus()).isEqualTo(503);
    assertThat(down.getDetail()).isEqualTo("Judge configuration temporarily unavailable");
  }

  // ---- monitor and infra events ----

  @Test
  void queueMonitorAndInfraEventsDelegateAndMapErrors() {
    var snapshot = mock(GetQueueSnapshotUseCase.class);
    var jobs = mock(ListQueueJobsUseCase.class);
    var controller = new QueueMonitorController(snapshot, jobs);
    var snap = mock(com.algoprep.judge.domain.model.QueueMonitoring.Snapshot.class);
    when(snapshot.read()).thenReturn(snap);
    assertThat(controller.snapshot()).isSameAs(snap);
    controller.jobs("failed", "2");
    verify(jobs).list(com.algoprep.judge.application.dto.QueueJobsQuery.parse("failed", "2"));
    var monitorAdvice = new QueueMonitorErrorAdvice();
    assertThat(monitorAdvice.denied().getProperties()).containsEntry("errorCode", "JDG-301");
    assertThat(monitorAdvice.unavailable().getStatus()).isEqualTo(503);

    var summary = mock(GetInfraSummaryUseCase.class);
    var events = mock(ListInfraEventsUseCase.class);
    var infra = new InfraEventsController(summary, events);
    var sum = mock(com.algoprep.judge.domain.model.InfraEvents.Summary.class);
    when(summary.summary()).thenReturn(sum);
    assertThat(infra.summary()).isSameAs(sum);
    infra.events(
        new InfraEventsController.Filters(
            "q", "ERROR", "outbox", "2026-11-01", "2026-11-02", null));
    verify(events).list(any());
    assertThatThrownBy(
            () ->
                infra.events(
                    new InfraEventsController.Filters(
                        null, null, null, "2026-11-05", "2026-11-01", null)))
        .isInstanceOf(IllegalArgumentException.class);
    var infraAdvice = new InfraEventsErrorAdvice();
    assertThat(infraAdvice.denied().getProperties()).containsEntry("errorCode", "JDG-301");
    assertThat(infraAdvice.invalid().getProperties()).containsEntry("errorCode", "JDG-V13");
    assertThat(infraAdvice.unavailable().getStatus()).isEqualTo(503);
  }

  @Test
  void learnerListAdviceMapsErrors() {
    var advice = new LearnerListErrorAdvice();
    var invalid = advice.invalid(new InvalidRequestException("JDG-V14", "x"));
    assertThat(invalid.getStatus()).isEqualTo(400);
    assertThat(invalid.getProperties()).containsEntry("errorCode", "JDG-V14");
    assertThat(advice.denied().getStatus()).isEqualTo(403);
    assertThat(advice.unavailable().getStatus()).isEqualTo(503);
    assertThat(new JudgeReadUnavailableException()).isInstanceOf(RuntimeException.class);
    assertThat(new DataAccessResourceFailureException("db")).isInstanceOf(RuntimeException.class);
  }

  @Test
  void booleanFlagsAreNeverCoercedFromOtherTypes() {
    var pausedUseCase = mock(SetAdminQueuePausedUseCase.class);
    var controller =
        queueController(
            mock(GetAdminQueueSettingsUseCase.class),
            pausedUseCase,
            mock(SetAdminQueueAutoscaleUseCase.class),
            mock(UpdateAdminQueueSettingsUseCase.class));
    assertThatThrownBy(() -> controller.paused(jwt, "{\"expectedPaused\":1,\"paused\":0}"))
        .isInstanceOf(AdminConfigException.class);
    verify(pausedUseCase, org.mockito.Mockito.never()).setPaused(any(), anyBoolean(), anyBoolean());
  }
}

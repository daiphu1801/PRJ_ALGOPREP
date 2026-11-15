package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationRequest;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationResult;
import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.ExecutionModels.TestcaseResult;
import com.algoprep.judge.application.dto.ExecutionModels.Verdict;
import com.algoprep.judge.application.dto.GradingSpecification;
import com.algoprep.judge.application.dto.GradingTestcase;
import com.algoprep.judge.application.dto.SampleRun.Command;
import com.algoprep.judge.application.dto.SampleRun.Reservation;
import com.algoprep.judge.application.dto.WorkerPreparation.Prepared;
import com.algoprep.judge.application.ports.in.EvaluateProgramUseCase;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.application.ports.out.SampleProgramPort;
import com.algoprep.judge.application.ports.out.SampleQuotaPort;
import com.algoprep.judge.domain.model.SampleSettings;
import com.algoprep.judge.domain.model.WorkerSettings;
import com.algoprep.judge.domain.ports.out.SampleSettingsRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

class RunSampleCommandHandlerTest {
  private final ProblemGradingSpecPort problems = mock(ProblemGradingSpecPort.class);
  private final SampleSettingsRepository settings = mock(SampleSettingsRepository.class);
  private final SampleProgramPort programs = mock(SampleProgramPort.class);
  private final SampleQuotaPort quota = mock(SampleQuotaPort.class);
  private final EvaluateProgramUseCase evaluation = mock(EvaluateProgramUseCase.class);
  private final SampleRunBudget budget = new SampleRunBudget(8, 60000);
  private final SampleRunContext context = new SampleRunContext(problems, settings, programs);
  private final RunSampleCommandHandler handler =
      new RunSampleCommandHandler(context, evaluation, quota, budget);
  private final UUID user = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final UUID sample = UUID.randomUUID();

  @BeforeEach
  void setup() {
    when(problems.loadGradingSpec(problem)).thenReturn(Optional.of(spec(true, true)));
    when(problems.loadSampleTestcases(problem)).thenReturn(List.of(testcase("SAMPLE")));
    when(settings.read()).thenReturn(List.of(config(true, false)));
    when(quota.reserve(any(), any())).thenReturn(new Reservation(true, 0));
    when(programs.prepare(any())).thenReturn(new Prepared(null, null));
    when(evaluation.evaluate(any()))
        .thenReturn(
            new EvaluationResult(
                Verdict.AC,
                null,
                null,
                List.of(new TestcaseResult(1, Verdict.AC, 1, 2, "1", null, null))));
  }

  private Command command(String source) {
    return new Command(user, problem, "python", "standard_io", source);
  }

  private GradingTestcase testcase(String visibility) {
    return new GradingTestcase(
        sample,
        visibility,
        1,
        "1".getBytes(StandardCharsets.UTF_8),
        "1".getBytes(StandardCharsets.UTF_8));
  }

  private GradingSpecification spec(boolean published, boolean wrapper) {
    return new GradingSpecification(
        new GradingSpecification.Problem(problem, published, wrapper, false),
        new GradingSpecification.Limits(1001, 64, 1, null),
        new GradingSpecification.Versions(7, 1),
        new GradingSpecification.Matching("EXACT", null),
        List.of());
  }

  private SampleSettings config(boolean enabled, boolean network) {
    return new SampleSettings(
        "PYTHON",
        enabled,
        new WorkerSettings(
            new BigDecimal("1.5"), new BigDecimal("2"), 1000, 64, 1, 10, network, true, true));
  }

  private static void code(Throwable error, String expected) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(expected));
  }

  @Test
  void exactUtf8BoundaryNormalizesEnumsPinsSpecAndPassesRealLimits() {
    assertThat(handler.run(command("é".repeat(32768))).passedCount()).isEqualTo(1);
    var capture = ArgumentCaptor.forClass(EvaluationRequest.class);
    verify(evaluation).evaluate(capture.capture());
    var request = capture.getValue();
    assertThat(request.testcases().getFirst().limits())
        .isEqualTo(new Limits(1502, 134217728, 1024, 60));
    assertThat(request.compileLimits().timeMs()).isEqualTo(10000);
    verify(programs)
        .prepare(argThat(p -> p.specVersion() == 7 && p.command().language().equals("PYTHON")));
    verify(quota, never()).refund(any(), any());
  }

  @Test
  void inputErrorsNeverAcquireQuotaOrExecute() {
    assertThatThrownBy(() -> handler.run(command(" "))).satisfies(e -> code(e, "JDG-V01"));
    assertThatThrownBy(() -> handler.run(command("é".repeat(32768) + "x")))
        .satisfies(e -> code(e, "JDG-V02"));
    assertThatThrownBy(() -> handler.run(new Command(user, problem, "RUST", "STANDARD_IO", "x")))
        .satisfies(e -> code(e, "JDG-V03"));
    assertThatThrownBy(() -> handler.run(new Command(user, problem, "JAVA", "OTHER", "x")))
        .satisfies(e -> code(e, "JDG-V04"));
    verifyNoInteractions(quota, evaluation);
  }

  @Test
  void unavailablePublishedStateNoSamplesDisabledLanguageAndWrapperAreRejected() {
    when(problems.loadGradingSpec(problem)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> handler.run(command("x"))).satisfies(e -> code(e, "JDG-101"));
    when(problems.loadGradingSpec(problem)).thenReturn(Optional.of(spec(false, true)));
    assertThatThrownBy(() -> handler.run(command("x"))).satisfies(e -> code(e, "JDG-101"));
    when(problems.loadGradingSpec(problem)).thenReturn(Optional.of(spec(true, false)));
    assertThatThrownBy(
            () -> handler.run(new Command(user, problem, "PYTHON", "FUNCTION_WRAPPER", "x")))
        .satisfies(e -> code(e, "JDG-103"));
    when(settings.read()).thenReturn(List.of(config(false, false)));
    assertThatThrownBy(() -> handler.run(command("x"))).satisfies(e -> code(e, "JDG-102"));
    when(settings.read()).thenReturn(List.of(config(true, false)));
    when(problems.loadSampleTestcases(problem)).thenReturn(List.of());
    assertThatThrownBy(() -> handler.run(command("x"))).satisfies(e -> code(e, "JDG-105"));
    verifyNoInteractions(quota, evaluation);
  }

  @Test
  void hiddenFromABrokenPortAndDeferredNetworkAreFailClosed() {
    when(problems.loadSampleTestcases(problem)).thenReturn(List.of(testcase("HIDDEN")));
    assertThatThrownBy(() -> handler.run(command("x"))).satisfies(e -> code(e, "JDG-501"));
    when(settings.read()).thenReturn(List.of(config(true, true)));
    assertThatThrownBy(() -> handler.run(command("x"))).satisfies(e -> code(e, "JDG-501"));
    verifyNoInteractions(evaluation, quota);
  }

  @Test
  void quotaDenialPreservesRetryAndDoesNotRefundAnotherReservation() {
    when(quota.reserve(any(), any())).thenReturn(new Reservation(false, 37));
    assertThatThrownBy(() -> handler.run(command("x")))
        .isInstanceOfSatisfying(
            com.algoprep.judge.domain.exception.SampleRunException.class,
            e -> assertThat(e.clientErrorDetails()).containsEntry("retryAfterSeconds", 37L));
    verifyNoInteractions(programs, evaluation);
    verify(quota, never()).refund(any(), any());
  }

  @Test
  void busyRefundsOnlyTheNewRequestAndNeverCallsCompile() {
    for (int i = 0; i < 8; i++) {
      assertThat(budget.acquire()).isTrue();
    }
    try {
      assertThatThrownBy(() -> handler.run(command("x"))).satisfies(e -> code(e, "JDG-502"));
      verify(quota).refund(eq(user), any());
      verifyNoInteractions(evaluation, programs);
    } finally {
      for (int i = 0; i < 8; i++) {
        budget.release();
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"COMPILE_ERROR,false", "SYSTEM_ERROR,true"})
  void compilerErrorConsumesQuotaButInfrastructureFailureRefunds(String verdict, boolean refund) {
    when(evaluation.evaluate(any()))
        .thenReturn(
            new EvaluationResult(Verdict.valueOf(verdict), null, "learner error", List.of()));
    if (refund) {
      assertThatThrownBy(() -> handler.run(command("x"))).satisfies(e -> code(e, "JDG-501"));
      verify(quota).refund(eq(user), any());
    } else {
      assertThat(handler.run(command("x")).outcome()).isEqualTo("COMPILE_ERROR");
      verify(quota, never()).refund(any(), any());
    }
    assertThat(budget.acquire()).isTrue();
    budget.release();
  }

  @Test
  void genericDependencyErrorsAreRedactedAndRefunded() {
    when(programs.prepare(any())).thenThrow(new IllegalStateException("private source"));
    assertThatThrownBy(() -> handler.run(command("secret source")))
        .satisfies(e -> code(e, "JDG-501"))
        .hasMessage("Sample execution unavailable")
        .hasNoCause();
    verify(quota).refund(eq(user), any());
    assertThat(command("secret source").toString()).doesNotContain("secret source");
  }

  @Test
  void redisFailOpenDoesNotTryToRefundAnUnrecordedRequest() {
    when(quota.reserve(any(), any())).thenReturn(new Reservation(false, 0));
    when(evaluation.evaluate(any()))
        .thenReturn(new EvaluationResult(Verdict.SYSTEM_ERROR, null, null, List.of()));
    assertThatThrownBy(() -> handler.run(command("x"))).satisfies(e -> code(e, "JDG-501"));
    verify(quota, never()).refund(any(), any());
  }

  @Test
  void timeoutReturns503AndRetainsSlotUntilTaskActuallyStops() throws Exception {
    var stopped = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var oneSlot = new SampleRunBudget(1, 200);
    when(evaluation.evaluate(any()))
        .thenAnswer(
            invocation -> {
              try {
                release.await(3, TimeUnit.SECONDS);
              } catch (InterruptedException ignored) {
                release.await(3, TimeUnit.SECONDS);
              } finally {
                stopped.countDown();
              }
              return new EvaluationResult(Verdict.AC, null, null, List.of());
            });
    var limited = new RunSampleCommandHandler(context, evaluation, quota, oneSlot);
    try {
      assertThatThrownBy(() -> limited.run(command("x"))).satisfies(e -> code(e, "JDG-501"));
      assertThat(oneSlot.acquire()).isFalse();
      verify(quota, timeout(3000)).refund(eq(user), any());
    } finally {
      release.countDown();
    }
    assertThat(stopped.await(3, TimeUnit.SECONDS)).isTrue();
    verify(quota, timeout(3000)).refund(eq(user), any());
    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
        java.time.Duration.ofSeconds(3),
        () -> {
          while (!oneSlot.acquire()) {
            Thread.sleep(1);
          }
        });
    oneSlot.release();
  }

  @Test
  void cancellationBeforePreparationReturnsNeverStartsCompile() throws Exception {
    var release = new CountDownLatch(1);
    var returned = new CountDownLatch(1);
    when(programs.prepare(any()))
        .thenAnswer(
            call -> {
              try {
                release.await(3, TimeUnit.SECONDS);
              } catch (InterruptedException ignored) {
                release.await(3, TimeUnit.SECONDS);
              } finally {
                returned.countDown();
              }
              return new Prepared(null, null);
            });
    var limited =
        new RunSampleCommandHandler(context, evaluation, quota, new SampleRunBudget(1, 200));
    try {
      assertThatThrownBy(() -> limited.run(command("x"))).satisfies(e -> code(e, "JDG-501"));
    } finally {
      release.countDown();
    }
    assertThat(returned.await(3, TimeUnit.SECONDS)).isTrue();
    verify(evaluation, org.mockito.Mockito.after(500).never()).evaluate(any());
    verify(quota, timeout(3000)).refund(eq(user), any());
  }
}

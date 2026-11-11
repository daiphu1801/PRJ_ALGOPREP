package com.algoprep.aireview.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.RolloverOutcome;
import com.algoprep.aireview.application.ports.in.RolloverAiBudgetPeriodUseCase;
import java.lang.reflect.Method;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

class AiBudgetRolloverJobTest {
  private final RolloverAiBudgetPeriodUseCase rollover = mock(RolloverAiBudgetPeriodUseCase.class);
  private final AiBudgetRolloverJob job = new AiBudgetRolloverJob(rollover);

  @Test
  void theCronRunsAtFiveMinutesPastMidnightOnTheFirstInUtcPlus7() throws Exception {
    Scheduled scheduled = AiBudgetRolloverJob.class.getMethod("run").getAnnotation(Scheduled.class);
    assertThat(scheduled.cron()).isEqualTo("0 5 0 1 * *");
    assertThat(scheduled.zone()).isEqualTo("Asia/Ho_Chi_Minh");
  }

  @Test
  void theSameRolloverAlsoRunsOnceAtStartup() throws Exception {
    Method startup = AiBudgetRolloverJob.class.getDeclaredMethod("onStartup");
    assertThat(startup.getAnnotation(EventListener.class).value())
        .containsExactly(ApplicationReadyEvent.class);
    when(rollover.rollover())
        .thenReturn(new RolloverOutcome(false, false, LocalDate.of(2026, 11, 1), false));
    job.onStartup();
    job.run();
    verify(rollover, times(2)).rollover();
  }

  @Test
  void aFailureIsSwallowedSoTheNextRunCanRetry() {
    when(rollover.rollover()).thenThrow(new IllegalStateException("db down"));
    assertThat(job.execute()).isNull();
    RolloverOutcome ok = new RolloverOutcome(false, true, LocalDate.of(2026, 12, 1), true);
    doReturn(ok).when(rollover).rollover();
    assertThat(job.execute()).isSameAs(ok);
  }
}

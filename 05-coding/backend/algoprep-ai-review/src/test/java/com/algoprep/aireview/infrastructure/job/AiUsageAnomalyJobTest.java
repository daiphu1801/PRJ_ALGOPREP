package com.algoprep.aireview.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.AnomalyRunOutcome;
import com.algoprep.aireview.application.ports.in.DetectAiUsageAnomaliesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class AiUsageAnomalyJobTest {
  private final DetectAiUsageAnomaliesUseCase detect = mock(DetectAiUsageAnomaliesUseCase.class);
  private final AiUsageAnomalyJob job = new AiUsageAnomalyJob(detect);

  @Test
  void theCronRunsEveryHourOnTheHourInUtcPlus7() throws Exception {
    Scheduled scheduled = AiUsageAnomalyJob.class.getMethod("run").getAnnotation(Scheduled.class);
    assertThat(scheduled.cron()).isEqualTo("0 0 * * * *");
    assertThat(scheduled.zone()).isEqualTo("Asia/Ho_Chi_Minh");
  }

  @Test
  void aFailureIsSwallowedSoTheNextHourCanRetry() {
    when(detect.detect()).thenThrow(new IllegalStateException("db down"));
    assertThat(job.execute()).isNull();
    job.run();
    AnomalyRunOutcome ok = new AnomalyRunOutcome(false, 2);
    doReturn(ok).when(detect).detect();
    assertThat(job.execute()).isSameAs(ok);
  }
}

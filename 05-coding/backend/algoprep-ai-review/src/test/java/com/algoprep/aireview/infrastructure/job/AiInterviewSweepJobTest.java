package com.algoprep.aireview.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.interview.SweepOutcome;
import com.algoprep.aireview.application.ports.in.interview.SweepIdleInterviewSessionsUseCase;
import org.junit.jupiter.api.Test;

class AiInterviewSweepJobTest {
  private final SweepIdleInterviewSessionsUseCase sweep =
      mock(SweepIdleInterviewSessionsUseCase.class);
  private final AiInterviewSweepJob job = new AiInterviewSweepJob(sweep);

  @Test
  void runCallsTheSweepOnce() {
    when(sweep.sweep()).thenReturn(new SweepOutcome(1, 2, 3, 0));

    job.run();

    verify(sweep).sweep();
  }

  @Test
  void aFailingSweepIsLoggedAndNeverReachesTheScheduler() {
    when(sweep.sweep()).thenThrow(new IllegalStateException("llm down"));

    assertThatCode(job::run).doesNotThrowAnyException();
    verify(sweep).sweep();
  }

  @Test
  void theSchedulerIsASingleThreadNamedForTheSweep() {
    var scheduler = new AiInterviewSweepSchedulerConfiguration().aiInterviewSweepScheduler();

    assertThat(scheduler.getThreadNamePrefix()).isEqualTo("ai-interview-sweep-");
    assertThat(scheduler.getPoolSize()).isEqualTo(1);
  }
}

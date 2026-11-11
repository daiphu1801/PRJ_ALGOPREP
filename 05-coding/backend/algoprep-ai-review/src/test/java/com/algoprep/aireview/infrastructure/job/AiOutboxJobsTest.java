package com.algoprep.aireview.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.ports.in.CleanAiOutboxUseCase;
import com.algoprep.aireview.application.ports.in.RelayAiOutboxUseCase;
import org.junit.jupiter.api.Test;

class AiOutboxJobsTest {
  private final RelayAiOutboxUseCase relay = mock(RelayAiOutboxUseCase.class);
  private final CleanAiOutboxUseCase cleanup = mock(CleanAiOutboxUseCase.class);
  private final AiOutboxJobs jobs = new AiOutboxJobs(relay, cleanup);

  @Test
  void eachTickCallsItsUseCase() {
    jobs.relayTick();
    jobs.cleanupTick();
    verify(relay).relay();
    verify(cleanup).clean();
  }

  @Test
  void aFailureIsLoggedAndNeverReachesTheScheduler() {
    when(relay.relay()).thenThrow(new IllegalStateException("db down"));
    when(cleanup.clean()).thenThrow(new IllegalStateException("db down"));
    assertThatCode(jobs::relayTick).doesNotThrowAnyException();
    assertThatCode(jobs::cleanupTick).doesNotThrowAnyException();
  }

  @Test
  void theSchedulerBeanIsASingleThreadNamedForTheOutbox() {
    var scheduler = new AiOutboxSchedulerConfiguration().aiOutboxScheduler();
    org.assertj.core.api.Assertions.assertThat(scheduler.getThreadNamePrefix())
        .isEqualTo("ai-outbox-");
  }
}

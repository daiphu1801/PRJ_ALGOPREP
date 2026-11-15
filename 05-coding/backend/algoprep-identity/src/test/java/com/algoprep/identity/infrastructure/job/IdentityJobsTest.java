package com.algoprep.identity.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.identity.application.ports.in.AnonymizeDeletedAccountsUseCase;
import com.algoprep.identity.application.ports.in.BootstrapFirstAdminUseCase;
import com.algoprep.identity.application.ports.in.CleanIdentityOutboxUseCase;
import com.algoprep.identity.application.ports.in.PurgeExpiredAuditLogsUseCase;
import com.algoprep.identity.application.ports.in.PurgePendingAccountsUseCase;
import com.algoprep.identity.application.ports.in.PurgeRefreshTokensUseCase;
import com.algoprep.identity.application.ports.in.RelayIdentityOutboxUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Scheduled jobs JOB-ID-02..06, the outbox ticks and the first-admin runner. */
class IdentityJobsTest {

  @Test
  void anonymizationRunsOnceAndSwallowsFailures() {
    AnonymizeDeletedAccountsUseCase uc = mock(AnonymizeDeletedAccountsUseCase.class);
    when(uc.anonymize()).thenReturn(3).thenThrow(new IllegalStateException("db"));
    var job = new AccountAnonymizationJob(uc);
    job.run();
    assertThatCode(job::run).doesNotThrowAnyException();
    verify(uc, times(2)).anonymize();
  }

  @Test
  void refreshTokenPurgeRunsOnceAndSwallowsFailures() {
    PurgeRefreshTokensUseCase uc = mock(PurgeRefreshTokensUseCase.class);
    when(uc.purge()).thenReturn(5).thenThrow(new IllegalStateException("db"));
    var job = new RefreshTokenPurgeJob(uc);
    job.run();
    assertThatCode(job::run).doesNotThrowAnyException();
    verify(uc, times(2)).purge();
  }

  @Test
  void pendingAccountPurgeRunsOnceAndSwallowsFailures() {
    PurgePendingAccountsUseCase uc = mock(PurgePendingAccountsUseCase.class);
    when(uc.purge()).thenReturn(1).thenThrow(new IllegalStateException("db"));
    var job = new PendingAccountPurgeJob(uc);
    job.run();
    assertThatCode(job::run).doesNotThrowAnyException();
    verify(uc, times(2)).purge();
  }

  @Test
  void expiredAuditLogPurgeRunsOnceAndSwallowsFailures() {
    PurgeExpiredAuditLogsUseCase uc = mock(PurgeExpiredAuditLogsUseCase.class);
    when(uc.purge()).thenReturn(9).thenThrow(new IllegalStateException("db"));
    var job = new ExpiredAuditLogPurgeJob(uc);
    job.run();
    assertThatCode(job::run).doesNotThrowAnyException();
    verify(uc, times(2)).purge();
  }

  @Test
  void outboxTicksDelegateToRelayAndCleanup() {
    RelayIdentityOutboxUseCase relay = mock(RelayIdentityOutboxUseCase.class);
    CleanIdentityOutboxUseCase clean = mock(CleanIdentityOutboxUseCase.class);
    var jobs = new IdentityOutboxJobs(relay, clean);
    jobs.relayTick();
    jobs.cleanupTick();
    verify(relay).relay();
    verify(clean).clean();
  }

  @Test
  void outboxSchedulerIsASingleThreadedNamedPool() {
    ThreadPoolTaskScheduler scheduler =
        new IdentityOutboxSchedulerConfiguration().identityOutboxScheduler();
    assertThat(scheduler.getThreadNamePrefix()).isEqualTo("identity-outbox-");
    assertThat(scheduler.getPoolSize()).isEqualTo(1);
  }

  @Test
  void bootstrapRunnerPassesConfiguredCredentials() {
    BootstrapFirstAdminUseCase uc = mock(BootstrapFirstAdminUseCase.class);
    new AdminBootstrapRunner(uc, "admin@x.y", "secret").run(new DefaultApplicationArguments());
    verify(uc).bootstrap("admin@x.y", "secret");
  }

  @Test
  void bootstrapRunnerPropagatesUseCaseFailure() {
    BootstrapFirstAdminUseCase uc = mock(BootstrapFirstAdminUseCase.class);
    doThrow(new IllegalStateException("weak password")).when(uc).bootstrap("", "");
    var runner = new AdminBootstrapRunner(uc, "", "");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> runner.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalStateException.class);
  }
}

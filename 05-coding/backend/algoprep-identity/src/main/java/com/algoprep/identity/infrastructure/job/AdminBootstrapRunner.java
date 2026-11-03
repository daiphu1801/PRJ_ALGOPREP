package com.algoprep.identity.infrastructure.job;

import com.algoprep.identity.application.ports.in.BootstrapFirstAdminUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * JOB-ID-01: runs once at startup, after Flyway. A failure propagates so the application refuses to
 * start without an administrator. Skipped in the {@code test} profile, which has no database; tests
 * call {@link BootstrapFirstAdminUseCase} directly.
 */
@Component
@Profile("!test")
class AdminBootstrapRunner implements ApplicationRunner {
  private final BootstrapFirstAdminUseCase bootstrap;
  private final String email;
  private final String password;

  AdminBootstrapRunner(
      BootstrapFirstAdminUseCase bootstrap,
      @Value("${ALGOPREP_BOOTSTRAP_ADMIN_EMAIL:}") String email,
      @Value("${ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
    this.bootstrap = bootstrap;
    this.email = email;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments args) {
    bootstrap.bootstrap(email, password);
  }
}

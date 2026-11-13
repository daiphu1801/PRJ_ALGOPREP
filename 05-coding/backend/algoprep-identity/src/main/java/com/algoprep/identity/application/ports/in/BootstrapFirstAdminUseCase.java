package com.algoprep.identity.application.ports.in;

/** Creates the first ADMIN from startup configuration when none exists (DD BR-23). */
public interface BootstrapFirstAdminUseCase {

  /**
   * @param email value of {@code ALGOPREP_BOOTSTRAP_ADMIN_EMAIL}, may be blank
   * @param password value of {@code ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD}, may be blank
   * @throws IllegalStateException when no ADMIN exists and the configuration is missing or invalid
   */
  void bootstrap(String email, String password);
}

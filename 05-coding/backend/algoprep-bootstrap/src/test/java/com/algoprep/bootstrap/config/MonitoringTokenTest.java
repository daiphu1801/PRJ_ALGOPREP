package com.algoprep.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MonitoringTokenTest {
  @Test
  void onlyAnExactDedicatedNonemptyBearerCredentialMatches() {
    assertThat(MonitoringToken.matches("scrape-secret", "Bearer scrape-secret")).isTrue();
    for (String authorization :
        new String[] {
          null, "", "scrape-secret", "Basic scrape-secret", "Bearer other", "Bearer scrape-secret "
        }) {
      assertThat(MonitoringToken.matches("scrape-secret", authorization)).isFalse();
    }
    assertThat(MonitoringToken.matches(null, "Bearer anything")).isFalse();
    assertThat(MonitoringToken.matches("", "Bearer ")).isFalse();
    assertThat(MonitoringToken.matches("  ", "Bearer   ")).isFalse();
  }
}

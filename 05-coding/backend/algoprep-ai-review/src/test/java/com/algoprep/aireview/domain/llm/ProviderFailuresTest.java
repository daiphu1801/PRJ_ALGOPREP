package com.algoprep.aireview.domain.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.aireview.domain.exception.AiProviderUnavailableException.Reason;
import org.junit.jupiter.api.Test;

class ProviderFailuresTest {
  @Test
  void rateLimitServerErrorAndBadKeyAreProviderErrors() {
    assertThat(ProviderFailures.classify(429, false)).isEqualTo(Reason.PROVIDER_ERROR);
    assertThat(ProviderFailures.classify(500, false)).isEqualTo(Reason.PROVIDER_ERROR);
    assertThat(ProviderFailures.classify(503, false)).isEqualTo(Reason.PROVIDER_ERROR);
    assertThat(ProviderFailures.classify(401, false)).isEqualTo(Reason.PROVIDER_ERROR);
    assertThat(ProviderFailures.classify(403, false)).isEqualTo(Reason.PROVIDER_ERROR);
    assertThat(ProviderFailures.classify(null, false)).isEqualTo(Reason.PROVIDER_ERROR);
  }

  @Test
  void timeoutsAreTimeoutWhateverTheStatus() {
    assertThat(ProviderFailures.classify(null, true)).isEqualTo(Reason.TIMEOUT);
    assertThat(ProviderFailures.classify(500, true)).isEqualTo(Reason.TIMEOUT);
    assertThat(ProviderFailures.classify(408, false)).isEqualTo(Reason.TIMEOUT);
    assertThat(ProviderFailures.classify(504, false)).isEqualTo(Reason.TIMEOUT);
  }
}

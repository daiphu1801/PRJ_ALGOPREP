package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.model.ExpiredLogPolicy;
import com.algoprep.identity.domain.model.LogRetentionSettings;
import com.algoprep.identity.domain.ports.out.LogRetentionRepository;
import org.junit.jupiter.api.Test;

/** #51 returns what the repository stores. */
class GetLogRetentionSettingsQueryHandlerTest {
  @Test
  void returnsTheStoredSettings() {
    LogRetentionRepository retention = mock(LogRetentionRepository.class);
    LogRetentionSettings stored = new LogRetentionSettings(90, ExpiredLogPolicy.DELETE);
    when(retention.read()).thenReturn(stored);

    assertThat(new GetLogRetentionSettingsQueryHandler(retention).get()).isEqualTo(stored);
  }
}

package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.model.AdminQueueSettings;
import com.algoprep.judge.domain.model.AdminQueueState;
import com.algoprep.judge.domain.ports.out.AdminQueueRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GetAdminQueueSettingsQueryHandlerTest {
  @Test
  void everyReadUsesTheCurrentDatabaseValues() {
    var repository = mock(AdminQueueRepository.class);
    var first = new AdminQueueSettings(20, 20, 24, 10, Instant.EPOCH);
    var second = new AdminQueueSettings(1, 7, 100000, 10, Instant.EPOCH.plusSeconds(1));
    when(repository.read())
        .thenReturn(
            new AdminQueueState(false, true, first), new AdminQueueState(false, true, second));
    var handler = new GetAdminQueueSettingsQueryHandler(repository);
    assertThat(handler.read()).isEqualTo(first);
    assertThat(handler.read()).isEqualTo(second);
  }
}

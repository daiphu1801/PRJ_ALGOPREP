package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.model.AdminLanguageConfig;
import com.algoprep.judge.domain.ports.out.AdminLanguageConfigRepository;
import org.junit.jupiter.api.Test;

class GetAdminLanguageConfigQueryHandlerTest {
  @Test
  void readsTheCurrentRepositoryStateWithoutCaching() {
    var repository = mock(AdminLanguageConfigRepository.class);
    var state = mock(AdminLanguageConfig.class);
    when(repository.read()).thenReturn(state);
    assertThat(new GetAdminLanguageConfigQueryHandler(repository).read()).isSameAs(state);
    verify(repository).read();
  }
}

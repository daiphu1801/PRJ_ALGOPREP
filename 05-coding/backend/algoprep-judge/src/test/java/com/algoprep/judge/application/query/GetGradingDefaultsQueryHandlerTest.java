package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.model.GradingDefaults;
import com.algoprep.judge.domain.ports.out.GradingDefaultsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class GetGradingDefaultsQueryHandlerTest {
  private final GradingDefaultsRepository repository = mock(GradingDefaultsRepository.class);
  private final GetGradingDefaultsQueryHandler handler =
      new GetGradingDefaultsQueryHandler(repository);

  @Test
  void readsAgainOnEachCallAndKeepsUnits() {
    var initial = new GradingDefaults(1111, 222, 33, 14);
    var edited = new GradingDefaults(2222, 333, 44, 15);
    when(repository.read()).thenReturn(initial, edited);
    assertThat(handler.getGradingDefaults()).isEqualTo(initial);
    assertThat(handler.getGradingDefaults()).isEqualTo(edited);
    verify(repository, times(2)).read();
  }

  @Test
  void doesNotFabricateDefaultsOnFailure() {
    var failure = new DataAccessResourceFailureException("Unavailable");
    when(repository.read()).thenThrow(failure);
    assertThatThrownBy(handler::getGradingDefaults).isSameAs(failure);
  }
}

package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.ports.out.SolveStatePort;
import com.algoprep.problembank.domain.model.StudentCatalogModels.TopicProgress;
import com.algoprep.problembank.domain.ports.out.StudentCatalogReadRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StudentCatalogQueryHandlerTest {
  private final StudentCatalogReadRepository repository = mock(StudentCatalogReadRepository.class);
  private final SolveStatePort states = mock(SolveStatePort.class);
  private final StudentCatalogQueryHandler handler =
      new StudentCatalogQueryHandler(repository, states);
  private final UUID user = UUID.randomUUID();

  @Test
  void topicProgressStaysUpWithANullSolvedCountWhenIdentityCannotAnswer() {
    when(states.mine(user)).thenReturn(Optional.empty());
    when(repository.topicProgress(isNull()))
        .thenReturn(List.of(new TopicProgress(UUID.randomUUID(), "Arrays", 1, 5, null)));

    var result = handler.topicProgress(user);

    assertThat(result.solveStateAvailable()).isFalse();
    assertThat(result.items().get(0).solved()).isNull();
    assertThat(result.items().get(0).total()).isEqualTo(5);
  }

  @Test
  void levelProgressIs503RatherThanAZeroWhenIdentityCannotAnswer() {
    when(states.mine(user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.levelProgress(user))
        .isInstanceOf(ServiceUnavailableException.class);
    verify(repository, never()).levelProgress(isNull());
  }
}

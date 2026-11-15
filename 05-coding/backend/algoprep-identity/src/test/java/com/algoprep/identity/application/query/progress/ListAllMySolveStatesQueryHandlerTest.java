package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.model.SolveState;
import com.algoprep.identity.domain.ports.out.MyProgressReadRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListAllMySolveStatesQueryHandlerTest {
  @Test
  void returnsEveryStateOfTheCallerOnly() {
    var repository = mock(MyProgressReadRepository.class);
    var user = UUID.randomUUID();
    var problem = UUID.randomUUID();
    when(repository.allSolveStates(user)).thenReturn(Map.of(problem, SolveState.SOLVED));

    var result = new ListAllMySolveStatesQueryHandler(repository).listAll(user);

    assertThat(result).containsEntry(problem, SolveState.SOLVED).hasSize(1);
  }
}

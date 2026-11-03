package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.model.SubmissionClassScope;
import com.algoprep.identity.domain.ports.out.SubmissionClassScopeRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadSubmissionClassScopeQueryHandlerTest {
  @Test
  void preservesEmptyClassesAndHistoricalMemberships() {
    var repository = mock(SubmissionClassScopeRepository.class);
    var instructor = UUID.randomUUID();
    var empty = new SubmissionClassScope(UUID.randomUUID(), "Empty", null, null, null, null);
    when(repository.read(instructor)).thenReturn(List.of(empty));
    assertThat(new ReadSubmissionClassScopeQueryHandler(repository).read(instructor))
        .containsExactly(empty);
  }
}

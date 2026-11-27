package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.domain.ports.out.HistoricalTopicProblemRepository;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ReadHistoricalTopicProblemIdsQueryHandlerTest {
  @Test
  void validScopeIsPassedToTheRealRepositoryButOversizedScopeIsRejected() {
    var repository = mock(HistoricalTopicProblemRepository.class);
    var handler = new ReadHistoricalTopicProblemIdsQueryHandler(repository);
    var ids = Set.of(UUID.randomUUID());
    var expected = Map.of(ids.iterator().next(), Set.of(UUID.randomUUID()));
    when(repository.read(ids)).thenReturn(expected);
    assertThat(handler.read(ids)).isSameAs(expected);
    verify(repository).read(ids);
    var tooMany =
        IntStream.range(0, 51).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toSet());
    assertThatThrownBy(() -> handler.read(tooMany)).isInstanceOf(IllegalArgumentException.class);
    verifyNoMoreInteractions(repository);
  }
}

package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.algoprep.problembank.domain.ports.out.SubmissionMetadataRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadSubmissionMetadataQueryHandlerTest {
  @Test
  void emptyInputsReadNoContentAndSearchIsBounded() {
    var repository = mock(SubmissionMetadataRepository.class);
    var handler = new ReadSubmissionMetadataQueryHandler(repository);
    assertThat(handler.summaries(Set.of())).isEmpty();
    assertThat(handler.samples(UUID.randomUUID(), Set.of())).isEmpty();
    verifyNoInteractions(repository);
    UUID id = UUID.randomUUID();
    handler.summaries(Set.of(id));
    handler.samples(id, Set.of(id));
    handler.search("sum", 600);
    handler.search("sum", 0);
    verify(repository).summaries(Set.of(id));
    verify(repository).samples(id, Set.of(id));
    verify(repository).search("sum", 500);
    verify(repository).search("sum", 1);
  }
}

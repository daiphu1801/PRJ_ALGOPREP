package com.algoprep.interviewbank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.interviewbank.application.dto.CatalogQuery;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListCatalogQueryHandlersTest {
  private final CatalogRepository repository = mock(CatalogRepository.class);
  private final CatalogItem item = new CatalogItem(UUID.randomUUID(), "A", "A", 1, false, 4L);

  @Test
  void countsAreRequestedOnlyForAnAdministratorWhoAsked() {
    var topics = new ListQuestionTopicsQueryHandler(repository);
    var levels = new ListQuestionLevelsQueryHandler(repository);
    when(repository.list(CatalogKind.TOPIC, true)).thenReturn(List.of(item));
    when(repository.list(CatalogKind.LEVEL, true)).thenReturn(List.of(item));
    when(repository.list(CatalogKind.TOPIC, false)).thenReturn(List.of());
    when(repository.list(CatalogKind.LEVEL, false)).thenReturn(List.of());

    assertThat(topics.list(new CatalogQuery(true, true))).containsExactly(item);
    assertThat(levels.list(new CatalogQuery(true, true))).containsExactly(item);
    for (var query : List.of(new CatalogQuery(false, true), new CatalogQuery(true, false))) {
      assertThat(topics.list(query)).isEmpty();
      assertThat(levels.list(query)).isEmpty();
    }
    verify(repository, times(2)).list(CatalogKind.TOPIC, false);
    verify(repository, times(2)).list(CatalogKind.LEVEL, false);
  }
}

package com.algoprep.problembank.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.domain.model.PublishedProblemModels.Filter;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Page;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Paging;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.ports.out.PublishedProblemReadRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CachedPublishedProblemReadRepositoryTest {
  @Test
  void statusFilterBypassesSharedCacheAndKeepsUserState() {
    var origin = mock(PublishedProblemReadRepository.class);
    var cache = mock(CatalogReadCache.class);
    var adapter = new CachedPublishedProblemReadRepository(origin, cache);
    var filter = new Filter(null, "SOLVED", null, null, null);
    var states = new SolveStates(Set.of(UUID.randomUUID()), Set.of());
    var paging = new Paging("code", true, 1, 12);
    var page = new Page(List.of(), 0, 1);
    when(origin.list(filter, states, paging)).thenReturn(page);
    assertThat(adapter.list(filter, states, paging)).isSameAs(page);
    verifyNoInteractions(cache);
  }
}

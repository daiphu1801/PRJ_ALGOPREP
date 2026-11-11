package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.ports.out.UserBaseCategoriesRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadUserBaseCategoriesQueryHandlerTest {
  private final UserBaseCategoriesRepository repository = mock(UserBaseCategoriesRepository.class);
  private final ReadUserBaseCategoriesQueryHandler handler =
      new ReadUserBaseCategoriesQueryHandler(repository);
  private final UUID id = UUID.randomUUID();

  @Test
  void emptyAndNullBatchesAvoidDatabaseAccess() {
    assertThat(handler.getBaseCategories(null)).isEmpty();
    assertThat(handler.getBaseCategories(List.of())).isEmpty();
    verifyNoInteractions(repository);
  }

  @Test
  void deduplicatesOneBatchAndReturnsTheCategory() {
    when(repository.findBaseCategories(Set.of(id))).thenReturn(Map.of(id, BaseCategory.ADMIN));
    assertThat(handler.getBaseCategories(List.of(id, id))).containsEntry(id, BaseCategory.ADMIN);
    verify(repository).findBaseCategories(Set.of(id));
  }

  @Test
  void unknownIdsHaveNoEntry() {
    when(repository.findBaseCategories(Set.of(id))).thenReturn(Map.of());
    assertThat(handler.getBaseCategories(List.of(id))).doesNotContainKey(id);
  }

  @Test
  void returnsAnImmutableSnapshot() {
    var stored = new HashMap<>(Map.of(id, BaseCategory.STUDENT));
    when(repository.findBaseCategories(Set.of(id))).thenReturn(stored);
    var result = handler.getBaseCategories(List.of(id));
    stored.put(id, BaseCategory.ADMIN);
    assertThat(result).containsEntry(id, BaseCategory.STUDENT);
  }

  @Test
  void infrastructureFailuresStayVisibleSoTheCallerCanSkipItsRun() {
    var failure = new IllegalStateException("database unavailable");
    when(repository.findBaseCategories(Set.of(id))).thenThrow(failure);
    assertThatThrownBy(() -> handler.getBaseCategories(List.of(id))).isSameAs(failure);
  }
}

package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.ports.out.UserDisplayNamesRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadUserDisplayNamesQueryHandlerTest {
  private final UserDisplayNamesRepository repository = mock(UserDisplayNamesRepository.class);
  private final ReadUserDisplayNamesQueryHandler handler =
      new ReadUserDisplayNamesQueryHandler(repository);
  private final UUID id = UUID.randomUUID();

  @Test
  void emptyAndNullBatchesAvoidDatabaseAccess() {
    assertThat(handler.getDisplayNames(null)).isEmpty();
    assertThat(handler.getDisplayNames(List.of())).isEmpty();
    verifyNoInteractions(repository);
  }

  @Test
  void deduplicatesOneBatchAndPreservesUnicodeNames() {
    when(repository.findDisplayNames(Set.of(id))).thenReturn(Map.of(id, "Nguyễn Thị Ánh"));
    assertThat(handler.getDisplayNames(List.of(id, id))).containsEntry(id, "Nguyễn Thị Ánh");
    verify(repository).findDisplayNames(Set.of(id));
  }

  @Test
  void unknownIdsHaveNoEntryAndThereforeResolveToNull() {
    when(repository.findDisplayNames(Set.of(id))).thenReturn(Map.of());
    assertThat(handler.getDisplayNames(List.of(id)).get(id)).isNull();
  }

  @Test
  void returnsAnImmutableSnapshot() {
    var stored = new java.util.HashMap<>(Map.of(id, "Name"));
    when(repository.findDisplayNames(Set.of(id))).thenReturn(stored);
    var result = handler.getDisplayNames(List.of(id));
    stored.put(id, "Changed");
    assertThat(result).containsEntry(id, "Name");
    assertThatThrownBy(() -> result.put(id, "Overwrite"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void infrastructureFailuresRemainVisibleToTheOptionalBridge() {
    var failure = new IllegalStateException("database unavailable");
    when(repository.findDisplayNames(Set.of(id))).thenThrow(failure);
    assertThatThrownBy(() -> handler.getDisplayNames(List.of(id))).isSameAs(failure);
  }

  @Test
  void malformedNullIdentifiersDoNotReachTheDatabase() {
    var ids = new ArrayList<UUID>();
    ids.add(null);
    assertThatThrownBy(() -> handler.getDisplayNames(ids)).isInstanceOf(NullPointerException.class);
    verifyNoInteractions(repository);
  }
}

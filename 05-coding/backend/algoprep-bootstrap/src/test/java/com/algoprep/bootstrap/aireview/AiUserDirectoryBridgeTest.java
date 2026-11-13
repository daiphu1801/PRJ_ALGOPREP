package com.algoprep.bootstrap.aireview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.identity.application.ports.in.ReadUserBaseCategoriesUseCase;
import com.algoprep.identity.application.ports.in.ReadUserDisplayNamesUseCase;
import com.algoprep.identity.domain.model.BaseCategory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Type translation and failure mapping of the F5 to F1 user directory bridge. */
class AiUserDirectoryBridgeTest {
  private final ReadUserDisplayNamesUseCase names = mock(ReadUserDisplayNamesUseCase.class);
  private final ReadUserBaseCategoriesUseCase categories =
      mock(ReadUserBaseCategoriesUseCase.class);
  private final AiUserDirectoryBridge bridge = new AiUserDirectoryBridge(names, categories);
  private final UUID id = UUID.randomUUID();

  @Test
  void namesPassThrough() {
    when(names.getDisplayNames(Set.of(id))).thenReturn(Map.of(id, "An"));
    assertThat(bridge.getDisplayNames(List.of(id, id))).containsEntry(id, "An");
  }

  @Test
  void identityCategoriesAreTranslatedToTheF5Vocabulary() {
    when(categories.getBaseCategories(Set.of(id))).thenReturn(Map.of(id, BaseCategory.ADMIN));
    assertThat(bridge.getBaseCategories(List.of(id))).containsEntry(id, CallerCategory.ADMIN);
  }

  @Test
  void aFailureIsOneIllegalStateExceptionForBothReads() {
    when(names.getDisplayNames(Set.of(id))).thenThrow(new RuntimeException("db"));
    when(categories.getBaseCategories(Set.of(id))).thenThrow(new RuntimeException("db"));
    assertThatThrownBy(() -> bridge.getDisplayNames(List.of(id)))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> bridge.getBaseCategories(List.of(id)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void aSlowIdentityMissesTheDeadline() {
    var slow = new AiUserDirectoryBridge(names, categories, Duration.ofMillis(50));
    when(names.getDisplayNames(Set.of(id)))
        .thenAnswer(
            call -> {
              Thread.sleep(1000);
              return Map.of();
            });
    assertThatThrownBy(() -> slow.getDisplayNames(List.of(id)))
        .isInstanceOf(IllegalStateException.class);
  }
}

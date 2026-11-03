package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.application.dto.CatalogCommand;
import com.algoprep.problembank.domain.model.CatalogItem;
import com.algoprep.problembank.domain.model.CatalogKind;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogCommandHandlersTest {
  @Test
  void createProblemLevelDelegatesTheFixedCatalogueKind() {
    var service = mock(CatalogMutationService.class);
    var command = new CatalogCommand(UUID.randomUUID(), true, null, "Fresh", List.of());
    var expected = List.<CatalogItem>of();
    when(service.create(CatalogKind.LEVEL, command)).thenReturn(expected);
    assertThat(new CreateProblemLevelCommandHandler(service).execute(command)).isSameAs(expected);
  }

  @Test
  void updateProblemLevelDelegatesTheFixedCatalogueKind() {
    var service = mock(CatalogMutationService.class);
    var command = new CatalogCommand(UUID.randomUUID(), true, null, "Fresh", List.of());
    var expected = List.<CatalogItem>of();
    when(service.update(CatalogKind.LEVEL, command)).thenReturn(expected);
    assertThat(new UpdateProblemLevelCommandHandler(service).execute(command)).isSameAs(expected);
  }

  @Test
  void reorderProblemLevelsDelegatesTheFixedCatalogueKind() {
    var service = mock(CatalogMutationService.class);
    var command = new CatalogCommand(UUID.randomUUID(), true, null, "Fresh", List.of());
    var expected = List.<CatalogItem>of();
    when(service.reorder(CatalogKind.LEVEL, command)).thenReturn(expected);
    assertThat(new ReorderProblemLevelsCommandHandler(service).execute(command)).isSameAs(expected);
  }

  @Test
  void deleteProblemLevelDelegatesTheFixedCatalogueKind() {
    var service = mock(CatalogMutationService.class);
    var command = new CatalogCommand(UUID.randomUUID(), true, null, "Fresh", List.of());
    var expected = List.<CatalogItem>of();
    when(service.delete(CatalogKind.LEVEL, command)).thenReturn(expected);
    assertThat(new DeleteProblemLevelCommandHandler(service).execute(command)).isSameAs(expected);
  }

  @Test
  void createProblemTopicDelegatesTheFixedCatalogueKind() {
    var service = mock(CatalogMutationService.class);
    var command = new CatalogCommand(UUID.randomUUID(), true, null, "Fresh", List.of());
    var expected = List.<CatalogItem>of();
    when(service.create(CatalogKind.TOPIC, command)).thenReturn(expected);
    assertThat(new CreateProblemTopicCommandHandler(service).execute(command)).isSameAs(expected);
  }

  @Test
  void updateProblemTopicDelegatesTheFixedCatalogueKind() {
    var service = mock(CatalogMutationService.class);
    var command = new CatalogCommand(UUID.randomUUID(), true, null, "Fresh", List.of());
    var expected = List.<CatalogItem>of();
    when(service.update(CatalogKind.TOPIC, command)).thenReturn(expected);
    assertThat(new UpdateProblemTopicCommandHandler(service).execute(command)).isSameAs(expected);
  }

  @Test
  void reorderProblemTopicsDelegatesTheFixedCatalogueKind() {
    var service = mock(CatalogMutationService.class);
    var command = new CatalogCommand(UUID.randomUUID(), true, null, "Fresh", List.of());
    var expected = List.<CatalogItem>of();
    when(service.reorder(CatalogKind.TOPIC, command)).thenReturn(expected);
    assertThat(new ReorderProblemTopicsCommandHandler(service).execute(command)).isSameAs(expected);
  }

  @Test
  void deleteProblemTopicDelegatesTheFixedCatalogueKind() {
    var service = mock(CatalogMutationService.class);
    var command = new CatalogCommand(UUID.randomUUID(), true, null, "Fresh", List.of());
    var expected = List.<CatalogItem>of();
    when(service.delete(CatalogKind.TOPIC, command)).thenReturn(expected);
    assertThat(new DeleteProblemTopicCommandHandler(service).execute(command)).isSameAs(expected);
  }
}

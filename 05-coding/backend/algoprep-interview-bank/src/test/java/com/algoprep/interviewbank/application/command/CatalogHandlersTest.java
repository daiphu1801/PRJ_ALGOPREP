package com.algoprep.interviewbank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import com.algoprep.interviewbank.domain.ports.out.InterviewBankAuditPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Each handler must bind the right catalogue; a swapped kind would edit the wrong table. */
class CatalogHandlersTest {
  private final CatalogRepository repository = mock(CatalogRepository.class);
  private final QuestionCatalogMutationService service =
      new QuestionCatalogMutationService(repository, mock(InterviewBankAuditPort.class));
  private final UUID id = UUID.randomUUID();
  private final CatalogItem item = new CatalogItem(id, "ONE", "One", 1, false, null);
  private final CatalogCommand command =
      new CatalogCommand(UUID.randomUUID(), true, id, "New", null, List.of(id));

  @Test
  void topicHandlersWorkOnTopicsOnly() {
    when(repository.create(CatalogKind.TOPIC, "New")).thenReturn(item);
    when(repository.find(CatalogKind.TOPIC, id)).thenReturn(Optional.of(item));
    when(repository.list(CatalogKind.TOPIC, false)).thenReturn(List.of(item));
    assertThat(new CreateQuestionTopicCommandHandler(service).execute(command)).isSameAs(item);
    assertThat(new UpdateQuestionTopicCommandHandler(service).execute(command).displayName())
        .isEqualTo("New");
    assertThat(new ReorderQuestionTopicsCommandHandler(service).execute(command)).hasSize(1);
    new DeleteQuestionTopicCommandHandler(service).execute(command);
    verify(repository).delete(CatalogKind.TOPIC, id);
  }

  @Test
  void levelHandlersWorkOnLevelsOnly() {
    var second = new CatalogItem(UUID.randomUUID(), "TWO", "Two", 2, false, null);
    when(repository.create(CatalogKind.LEVEL, "New")).thenReturn(item);
    when(repository.find(CatalogKind.LEVEL, id)).thenReturn(Optional.of(item));
    when(repository.list(CatalogKind.LEVEL, false)).thenReturn(List.of(item, second));
    assertThat(new CreateQuestionLevelCommandHandler(service).execute(command)).isSameAs(item);
    assertThat(new UpdateQuestionLevelCommandHandler(service).execute(command).displayName())
        .isEqualTo("New");
    var swap =
        new CatalogCommand(command.actorId(), true, id, null, null, List.of(second.id(), id));
    assertThat(new ReorderQuestionLevelsCommandHandler(service).execute(swap)).hasSize(2);
    new DeleteQuestionLevelCommandHandler(service).execute(command);
    verify(repository).delete(CatalogKind.LEVEL, id);
  }
}

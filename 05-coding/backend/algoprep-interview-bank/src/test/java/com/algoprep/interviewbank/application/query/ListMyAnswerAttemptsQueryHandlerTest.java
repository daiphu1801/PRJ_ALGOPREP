package com.algoprep.interviewbank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.domain.model.AttemptPage;
import com.algoprep.interviewbank.domain.model.PracticeQuestion;
import com.algoprep.interviewbank.domain.ports.out.PracticeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListMyAnswerAttemptsQueryHandlerTest {
  private final PracticeRepository repository = mock(PracticeRepository.class);
  private final ListMyAnswerAttemptsQueryHandler handler =
      new ListMyAnswerAttemptsQueryHandler(repository);
  private final UUID user = UUID.randomUUID();
  private final UUID question = UUID.randomUUID();

  @Test
  void readsTheCallersOwnPageOfTenForAnActiveQuestion() {
    when(repository.findActiveQuestion(question))
        .thenReturn(Optional.of(new PracticeQuestion(question, "t", "c", List.of())));
    var page = new AttemptPage(List.of(), false, 0);
    when(repository.attempts(user, question, 5, 10)).thenReturn(page);

    assertThat(handler.list(user, question, 5)).isSameAs(page);
  }

  @Test
  void aMissingOrRetiredQuestionIsInb010AndReadsNoAttempts() {
    when(repository.findActiveQuestion(question)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.list(user, question, null))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).attempts(user, question, null, 10);
  }
}

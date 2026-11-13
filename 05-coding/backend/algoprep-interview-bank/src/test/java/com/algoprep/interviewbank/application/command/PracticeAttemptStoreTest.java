package com.algoprep.interviewbank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.domain.model.AnswerAttempt;
import com.algoprep.interviewbank.domain.model.FeedbackStatus;
import com.algoprep.interviewbank.domain.model.NewAttempt;
import com.algoprep.interviewbank.domain.model.QuestionLock;
import com.algoprep.interviewbank.domain.ports.out.PracticeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PracticeAttemptStoreTest {
  private final PracticeRepository repository = mock(PracticeRepository.class);
  private final PracticeAttemptStore store = new PracticeAttemptStore(repository);
  private final UUID user = UUID.randomUUID();
  private final UUID question = UUID.randomUUID();
  private final UUID topic = UUID.randomUUID();
  private final UUID answer = UUID.randomUUID();

  private void numbered(int attemptNo) {
    when(repository.nextAttemptNo(user, question)).thenReturn(attemptNo);
    when(repository.insertAttempt(any(), anyInt()))
        .thenReturn(
            new AnswerAttempt(answer, attemptNo, Instant.EPOCH, "a", FeedbackStatus.FAILED, null));
  }

  private AnswerAttempt save() {
    return store.save(
        new NewAttempt(answer, user, question, "a", FeedbackStatus.FAILED, null, List.of()));
  }

  @Test
  void theFirstAttemptOfAnActiveQuestionGrowsTheProgressCounter() {
    when(repository.lockQuestion(question)).thenReturn(Optional.of(new QuestionLock(topic, true)));
    numbered(1);

    assertThat(save().attemptNo()).isEqualTo(1);
    verify(repository).touchHistory(user, topic, true);
  }

  @Test
  void aLaterAttemptOnlyMovesTheLastPracticedTime() {
    when(repository.lockQuestion(question)).thenReturn(Optional.of(new QuestionLock(topic, true)));
    numbered(2);

    save();

    verify(repository).touchHistory(user, topic, false);
  }

  @Test
  void aQuestionRetiredWhileTheAiWasThinkingKeepsTheAttemptButNotTheProgress() {
    when(repository.lockQuestion(question)).thenReturn(Optional.of(new QuestionLock(topic, false)));
    numbered(1);

    assertThat(save().attemptNo()).isEqualTo(1);
    verify(repository, never()).touchHistory(any(), any(), anyBoolean());
  }

  @Test
  void aVanishedQuestionRowIsInb010AndWritesNothing() {
    when(repository.lockQuestion(question)).thenReturn(Optional.empty());

    assertThatThrownBy(this::save).isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).nextAttemptNo(any(), any());
  }
}

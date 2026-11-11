package com.algoprep.interviewbank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.domain.model.RecallRating;
import com.algoprep.interviewbank.domain.model.RecallState;
import com.algoprep.interviewbank.domain.ports.out.LearnerQuestionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InterviewLearnerCommandHandlersTest {
  private final LearnerQuestionRepository repository = mock(LearnerQuestionRepository.class);
  private final UUID user = UUID.randomUUID();
  private final UUID question = UUID.randomUUID();

  @Test
  void ratingReturnsTheStoredState() {
    var state = new RecallState(RecallRating.VAGUE, 3, Instant.now(), Instant.now());
    when(repository.rate(user, question, RecallRating.VAGUE)).thenReturn(Optional.of(state));
    assertThat(
            new RateQuestionRecallCommandHandler(repository)
                .rate(user, question, RecallRating.VAGUE))
        .isSameAs(state);
  }

  @Test
  void ratingAnInactiveQuestionIsInb010() {
    when(repository.rate(user, question, RecallRating.KNOWN)).thenReturn(Optional.empty());
    var handler = new RateQuestionRecallCommandHandler(repository);
    assertThatThrownBy(() -> handler.rate(user, question, RecallRating.KNOWN))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class, e -> assertThat(e.errorCode()).isEqualTo("INB-010"));
  }

  @Test
  void bookmarkingSetsTheWantedStateForTheCaller() {
    when(repository.isActive(question)).thenReturn(true);
    var handler = new SetQuestionBookmarkCommandHandler(repository);
    handler.set(user, question, true);
    handler.set(user, question, false);
    verify(repository).setBookmark(user, question, true);
    verify(repository).setBookmark(user, question, false);
  }

  @Test
  void bookmarkingAnInactiveQuestionWritesNothing() {
    when(repository.isActive(question)).thenReturn(false);
    var handler = new SetQuestionBookmarkCommandHandler(repository);
    assertThatThrownBy(() -> handler.set(user, question, true))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).setBookmark(user, question, true);
  }

  @Test
  void intervalsFollowTheFixedTable() {
    assertThat(RecallRating.FORGOTTEN.intervalDays()).isEqualTo(1);
    assertThat(RecallRating.VAGUE.intervalDays()).isEqualTo(3);
    assertThat(RecallRating.KNOWN.intervalDays()).isEqualTo(7);
  }
}

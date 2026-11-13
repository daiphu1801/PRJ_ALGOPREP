package com.algoprep.interviewbank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.interviewbank.domain.model.TopicProgress;
import com.algoprep.interviewbank.domain.ports.out.TopicProgressRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadMyInterviewTopicProgressQueryHandlerTest {
  private final TopicProgressRepository repository = mock(TopicProgressRepository.class);
  private final ReadMyInterviewTopicProgressQueryHandler handler =
      new ReadMyInterviewTopicProgressQueryHandler(repository);

  private static int row(int attempted, int total) {
    return TopicProgress.percent(attempted, total);
  }

  @Test
  void handlerReturnsExactlyWhatTheRepositoryGivesForThatUser() {
    UUID user = UUID.randomUUID();
    List<TopicProgress> rows =
        List.of(new TopicProgress(UUID.randomUUID(), "T", "Topic", 1, 3, 10, 30, null));
    when(repository.listFor(user)).thenReturn(rows);

    assertThat(handler.listFor(user)).isSameAs(rows);
    verify(repository).listFor(user);
  }

  @Test
  void percentIsRoundedHalfUpAndZeroWithoutQuestions() {
    assertThat(row(3, 10)).isEqualTo(30);
    assertThat(row(1, 3)).isEqualTo(33);
    assertThat(row(2, 3)).isEqualTo(67);
    assertThat(row(0, 0)).isZero();
    assertThat(row(5, 0)).isZero();
    assertThat(row(0, 7)).isZero();
  }

  @Test
  void percentIsClampedAtOneHundred() {
    assertThat(row(10, 10)).isEqualTo(100);
    assertThat(row(12, 10)).isEqualTo(100);
  }
}

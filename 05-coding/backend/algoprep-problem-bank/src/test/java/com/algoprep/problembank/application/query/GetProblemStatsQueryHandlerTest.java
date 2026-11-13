package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;
import com.algoprep.problembank.domain.model.ProblemStatsModels.ProblemStatsRow;
import com.algoprep.problembank.domain.ports.out.ProblemStatsRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetProblemStatsQueryHandlerTest {
  private final ProblemStatsRepository repository = mock(ProblemStatsRepository.class);
  private final GetProblemStatsQueryHandler handler = new GetProblemStatsQueryHandler(repository);
  private final UUID problem = UUID.randomUUID();
  private final UUID me = UUID.randomUUID();
  private final UUID other = UUID.randomUUID();

  private ProblemAuthoringQuery asInstructor() {
    return new ProblemAuthoringQuery(problem, me, false, false);
  }

  @Test
  void theAuthorReadsTheCountersOfTheirOwnDraft() {
    var at = Instant.parse("2026-11-06T00:00:00Z");
    when(repository.find(problem))
        .thenReturn(
            Optional.of(new ProblemStatsRow(me, "UNPUBLISHED", 100, new BigDecimal("40.00"), at)));

    var view = handler.get(asInstructor());

    assertThat(view.submissionCount()).isEqualTo(100);
    assertThat(view.acRate()).isEqualByComparingTo("40.00");
    assertThat(view.updatedAt()).isEqualTo(at);
  }

  @Test
  void aProblemWithoutCountersAnswersZeroAndNulls() {
    when(repository.find(problem))
        .thenReturn(Optional.of(new ProblemStatsRow(me, "PUBLISHED", 0, null, null)));

    var view = handler.get(asInstructor());

    assertThat(view.submissionCount()).isZero();
    assertThat(view.acRate()).isNull();
    assertThat(view.updatedAt()).isNull();
  }

  @Test
  void anInstructorReadsAPublishedProblemOfSomeoneElse() {
    when(repository.find(problem))
        .thenReturn(Optional.of(new ProblemStatsRow(other, "PUBLISHED", 3, null, null)));

    assertThat(handler.get(asInstructor()).submissionCount()).isEqualTo(3);
  }

  @Test
  void someoneElsesDraftAndAMissingProblemAreTheSame404ButAnAdminSeesTheDraft() {
    when(repository.find(problem))
        .thenReturn(Optional.of(new ProblemStatsRow(other, "UNPUBLISHED", 3, null, null)));
    assertThatThrownBy(() -> handler.get(asInstructor()))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-010"));
    assertThat(handler.get(new ProblemAuthoringQuery(problem, me, true, false)).submissionCount())
        .isEqualTo(3);

    when(repository.find(problem)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> handler.get(asInstructor()))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}

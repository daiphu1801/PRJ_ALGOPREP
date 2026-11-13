package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.domain.model.ProblemBankStatsModels.LevelRef;
import com.algoprep.problembank.domain.model.ProblemBankStatsModels.ProblemStat;
import com.algoprep.problembank.domain.ports.out.ProblemBankStatsRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProblemBankStatsQueryHandlerTest {
  private final ProblemBankStatsRepository repository = mock(ProblemBankStatsRepository.class);
  private final ProblemBankStatsQueryHandler handler = new ProblemBankStatsQueryHandler(repository);

  @Test
  void theLevelCatalogIsPassedThrough() {
    var level = new LevelRef(UUID.randomUUID(), "easy", "Easy", 1);
    when(repository.levels()).thenReturn(List.of(level));

    assertThat(handler.levelCatalog()).containsExactly(level);
  }

  @Test
  void summariesOfNoIdsDoNotTouchTheDatabase() {
    assertThat(handler.problemSummaries(Set.of())).isEmpty();

    verifyNoInteractions(repository);
  }

  @Test
  void thePublishedProblemsArePassedThrough() {
    var stat =
        new ProblemStat(UUID.randomUUID(), "Two Sum", UUID.randomUUID(), "easy", "Easy", true);
    when(repository.published()).thenReturn(List.of(stat));

    assertThat(handler.publishedProblems()).containsExactly(stat);
  }

  @Test
  void summariesAreReadByIdAndKeepTheWithdrawnFlag() {
    var id = UUID.randomUUID();
    var stat = new ProblemStat(id, "Two Sum", UUID.randomUUID(), "easy", "Easy", false);
    when(repository.problems(Set.of(id))).thenReturn(List.of(stat));

    assertThat(handler.problemSummaries(Set.of(id))).containsExactly(stat);
    verify(repository).problems(Set.of(id));
  }
}

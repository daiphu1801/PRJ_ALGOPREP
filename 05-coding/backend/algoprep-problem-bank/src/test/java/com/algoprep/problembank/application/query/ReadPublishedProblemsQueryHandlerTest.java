package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.ports.out.SolveStatePort;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Filter;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Page;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Paging;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Row;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Sample;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Samples;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.StoredGradingTestcase.Content;
import com.algoprep.problembank.domain.ports.out.PublishedProblemReadRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReadPublishedProblemsQueryHandlerTest {
  private final PublishedProblemReadRepository repository =
      mock(PublishedProblemReadRepository.class);
  private final SolveStatePort states = mock(SolveStatePort.class);
  private final TestcaseObjectStorePort store = mock(TestcaseObjectStorePort.class);
  private final ReadPublishedProblemsQueryHandler handler =
      new ReadPublishedProblemsQueryHandler(repository, states, store);
  private final UUID user = UUID.randomUUID();

  @Test
  void whenIdentityCannotAnswerTheStatusConditionIsDroppedAndTheColumnIsNull() {
    var row =
        new Row(UUID.randomUUID(), "A", "Title", List.of(), "easy", "Easy", BigDecimal.ONE, true);
    when(states.mine(user)).thenReturn(Optional.empty());
    when(repository.list(any(), isNull(), any())).thenReturn(new Page(List.of(row), 1, 1));

    var result = handler.list(user, Map.of("status", "SOLVED"));

    assertThat(result.solveStateAvailable()).isFalse();
    assertThat(result.items().get(0).solveState()).isNull();
    var filter = ArgumentCaptor.forClass(Filter.class);
    verify(repository).list(filter.capture(), isNull(), any());
    assertThat(filter.getValue().status()).isNull();
  }

  @Test
  void invalidParametersTakeTheirDefaults() {
    when(states.mine(user)).thenReturn(Optional.of(new SolveStates(Set.of(), Set.of())));
    when(repository.list(any(), any(), any())).thenReturn(new Page(List.of(), 0, 1));

    var result =
        handler.list(
            user,
            Map.of(
                "sort", "abc",
                "dir", "up",
                "page", "-4",
                "pageSize", "7",
                "status", "NOT_SOLVED",
                "topic", "x",
                "q", "y".repeat(150)));

    assertThat(result.page().size()).isEqualTo(12);
    assertThat(result.page().totalPages()).isEqualTo(1);
    var filter = ArgumentCaptor.forClass(Filter.class);
    var paging = ArgumentCaptor.forClass(Paging.class);
    verify(repository).list(filter.capture(), any(), paging.capture());
    assertThat(paging.getValue()).isEqualTo(new Paging("code", true, 1, 12));
    assertThat(filter.getValue().status()).isNull();
    assertThat(filter.getValue().topic()).isNull();
    assertThat(filter.getValue().q()).hasSize(100);
  }

  @Test
  void notSolvedIsAcceptedByRandomOnly() {
    when(states.mine(user)).thenReturn(Optional.of(new SolveStates(Set.of(), Set.of())));
    when(repository.random(any(), any())).thenReturn(Optional.empty());

    var result = handler.random(user, Map.of("status", "NOT_SOLVED"));

    assertThat(result.problemId()).isNull();
    var filter = ArgumentCaptor.forClass(Filter.class);
    verify(repository).random(filter.capture(), any());
    assertThat(filter.getValue().status()).isEqualTo("NOT_SOLVED");
  }

  @Test
  void aMinioSampleBecomesASignedLinkAndAnInlineOneStaysInline() {
    var problem = UUID.randomUUID();
    when(store.presignGet("k/in", ReadPublishedProblemsQueryHandler.DOWNLOAD_TTL))
        .thenReturn("https://signed/in");
    when(repository.samples(problem))
        .thenReturn(
            Optional.of(
                new Samples(
                    List.of("nums"),
                    List.of(
                        new Sample(
                            UUID.randomUUID(),
                            1,
                            new Content("MINIO", null, "k/in"),
                            new Content("INLINE", "out", null))))));

    var item = handler.samples(problem).items().get(0);

    assertThat(item.input()).isNull();
    assertThat(item.inputDownloadUrl()).isEqualTo("https://signed/in");
    assertThat(item.expectedOutput()).isEqualTo("out");
    assertThat(item.expectedOutputDownloadUrl()).isNull();
  }

  @Test
  void aProblemThatIsNotVisibleIsNotFound() {
    var problem = UUID.randomUUID();
    when(repository.workspace(problem, user)).thenReturn(Optional.empty());
    when(repository.overview(problem, user)).thenReturn(Optional.empty());
    when(repository.samples(problem)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.workspace(user, problem))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> handler.overview(user, problem))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> handler.samples(problem))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}

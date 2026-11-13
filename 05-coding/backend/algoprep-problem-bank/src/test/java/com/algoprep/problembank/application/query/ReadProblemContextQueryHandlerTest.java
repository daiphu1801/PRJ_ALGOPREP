package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.domain.model.ProblemContextView;
import com.algoprep.problembank.domain.ports.out.ProblemContextReadRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadProblemContextQueryHandlerTest {
  private final ProblemContextReadRepository repository = mock(ProblemContextReadRepository.class);
  private final ReadProblemContextQueryHandler handler =
      new ReadProblemContextQueryHandler(repository);
  private final UUID id = UUID.randomUUID();

  private static ProblemContextView view() {
    return new ProblemContextView(
        "Two Sum",
        "statement",
        "n <= 1e5",
        List.of("Array"),
        "Easy",
        null,
        null,
        "brief",
        false,
        true);
  }

  @Test
  void returnsTheStoredContextAndNormalisesTheLanguage() {
    when(repository.findVisible(id, "JAVA")).thenReturn(Optional.of(view()));

    assertThat(handler.getContext(id, " java ")).isEqualTo(view());
    verify(repository).findVisible(id, "JAVA");
  }

  @Test
  void aBlankOrNullLanguageMeansNoOverride() {
    when(repository.findVisible(id, null)).thenReturn(Optional.of(view()));

    assertThat(handler.getContext(id, "  ")).isEqualTo(view());
    assertThat(handler.getContext(id, null)).isEqualTo(view());
  }

  @Test
  void anInvisibleProblemIsPrb010() {
    when(repository.findVisible(id, null)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.getContext(id, null))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-010"));
  }

  @Test
  void aNullIdNeverQueries() {
    assertThatThrownBy(() -> handler.getContext(null, "JAVA"))
        .isInstanceOf(ResourceNotFoundException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void theTopicListIsDefensivelyCopied() {
    var mutable = new java.util.ArrayList<>(List.of("Array"));
    var copy = new ProblemContextView("t", "s", "c", mutable, "Easy", null, null, null, true, true);
    mutable.add("Graph");

    assertThat(copy.topics()).containsExactly("Array");
  }
}

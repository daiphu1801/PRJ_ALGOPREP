package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.problembank.domain.exception.UnreadableProblemSpecException;
import com.algoprep.problembank.domain.model.ProblemSpecData;
import com.algoprep.problembank.domain.model.ProblemSpecReadView;
import com.algoprep.problembank.domain.ports.out.HarnessProblemReadRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class HarnessProblemReadQueryHandlerTest {
  private final HarnessProblemReadRepository repository = mock(HarnessProblemReadRepository.class);
  private final HarnessProblemReadQueryHandler handler =
      new HarnessProblemReadQueryHandler(repository);
  private final UUID id = UUID.randomUUID();

  private ProblemSpecReadView stored(String type) {
    return new ProblemSpecReadView(
        id,
        130,
        new ProblemSpecData(
            "EPSILON",
            new BigDecimal("0.000000000001"),
            "input raw",
            "output raw",
            "echo",
            type,
            "[{\"name\":\"value\",\"type\":{\"kind\":\"INT\"}}]",
            "{\"JAVA\":\"echoJava\"}"),
        false);
  }

  @Test
  void decodesAllFieldsWithoutRoundingOrLosingTheVersionAndFlag() {
    when(repository.findSpec(id)).thenReturn(Optional.of(stored("{\"kind\":\"INT\"}")));
    var result = handler.findSpec(id).orElseThrow();
    assertThat(result.problemId()).isEqualTo(id);
    assertThat(result.specVersion()).isEqualTo(130);
    assertThat(result.functionWrapperSupported()).isFalse();
    assertThat(result.stdinFormatMd()).isEqualTo("input raw");
    assertThat(result.stdoutFormatMd()).isEqualTo("output raw");
    assertThat(result.signature().returnType().kind()).isEqualTo(TypeKind.INT);
    assertThat(result.signature().parameters().getFirst().name()).isEqualTo("value");
    assertThat(result.signature().nameOverrides()).containsEntry("JAVA", "echoJava");
    assertThat(result.signature().epsilonValue()).isEqualByComparingTo("0.000000000001");
  }

  @Test
  void mapsUnknownKindsOnlyInMemoryAndKeepsStoredJsonUnchanged() {
    var row = stored("{\"kind\":\"BIG_DECIMAL\"}");
    when(repository.findSpec(id)).thenReturn(Optional.of(row));
    assertThat(handler.findSpec(id).orElseThrow().signature().returnType().kind())
        .isEqualTo(TypeKind.OTHER);
    assertThat(row.data().returnTypeJson()).isEqualTo("{\"kind\":\"BIG_DECIMAL\"}");
  }

  @Test
  void malformedPersistedShapeIsSanitized() {
    when(repository.findSpec(id)).thenReturn(Optional.of(stored("{\"kind\":123}")));
    assertThatThrownBy(() -> handler.findSpec(id))
        .isInstanceOfSatisfying(
            UnreadableProblemSpecException.class, error -> assertThat(error.getCause()).isNull());
  }

  @Test
  void missingSpecIsEmptyAndNullIdsDoNotQuery() {
    assertThat(handler.findSpec(null)).isEmpty();
    verifyNoInteractions(repository);
    when(repository.findSpec(id)).thenReturn(Optional.empty());
    assertThat(handler.findSpec(id)).isEmpty();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "ROOT", "admin"})
  void invalidCategoryNeverReadsAccessMetadata(String category) {
    assertThat(handler.canRead(id, UUID.randomUUID(), category)).isFalse();
    verifyNoInteractions(repository);
  }

  @Test
  void anonymousAndMissingResourceNeverReadAccessMetadata() {
    assertThat(handler.canRead(id, null, "ADMIN")).isFalse();
    assertThat(handler.canRead(null, UUID.randomUUID(), "ADMIN")).isFalse();
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"STUDENT", "INSTRUCTOR", "ADMIN"})
  void delegatesLiveAccessForEveryValidCategory(String category) {
    UUID actor = UUID.randomUUID();
    when(repository.canRead(id, actor, category)).thenReturn(true);
    assertThat(handler.canRead(id, actor, category)).isTrue();
    verify(repository).canRead(id, actor, category);
  }
}

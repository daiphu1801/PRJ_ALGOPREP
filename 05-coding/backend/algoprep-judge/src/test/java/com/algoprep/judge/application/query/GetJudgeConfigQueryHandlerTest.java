package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import com.algoprep.judge.domain.model.GeneratorSandboxLimits;
import com.algoprep.judge.domain.model.LanguageMultipliers;
import com.algoprep.judge.domain.ports.out.JudgeConfigRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetJudgeConfigQueryHandlerTest {
  private final JudgeConfigRepository repository = mock(JudgeConfigRepository.class);
  private final GetJudgeConfigQueryHandler handler = new GetJudgeConfigQueryHandler(repository);

  @Test
  void generatorLimitsAreReadEveryTimeWithoutUnitConversionOrFallback() {
    when(repository.generatorLimits())
        .thenReturn(new GeneratorSandboxLimits(45, 200), new GeneratorSandboxLimits(60, 200));
    assertThat(handler.getGeneratorSandboxLimits()).isEqualTo(new GeneratorSandboxLimits(45, 200));
    assertThat(handler.getGeneratorSandboxLimits()).isEqualTo(new GeneratorSandboxLimits(60, 200));
  }

  @Test
  void enabledListIsAnImmutableCopyOfTheCurrentRead() {
    var languages = new ArrayList<>(List.of("CPP", "JAVA"));
    when(repository.enabledLanguages()).thenReturn(languages);
    var result = handler.getEnabledLanguages();
    languages.clear();
    assertThat(result).containsExactly("CPP", "JAVA");
    assertThatThrownBy(() -> result.add("PYTHON"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(handler.getEnabledLanguages()).isEmpty();
  }

  @Test
  void exactLanguageHasItsOwnMultipliersAndUnknownInputNeverQueriesDefaults() {
    for (String invalid : List.of("RUST", "java", "JAVA' OR '1'='1", "")) {
      assertThatThrownBy(() -> handler.getLanguageMultipliers(invalid))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThatThrownBy(() -> handler.getLanguageMultipliers(null))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(repository);
    var pair = new LanguageMultipliers(new BigDecimal("2.00"), new BigDecimal("1.50"));
    for (String language : List.of("JAVA", "CPP", "PYTHON")) {
      when(repository.multipliers(language)).thenReturn(Optional.of(pair));
      assertThat(handler.getLanguageMultipliers(language)).isEqualTo(pair);
    }
  }

  @Test
  void missingOrFailedReadsAreSanitizedInsteadOfInventingConfiguration() {
    when(repository.generatorLimits()).thenThrow(new IllegalStateException("private SQL password"));
    when(repository.enabledLanguages())
        .thenThrow(new IllegalStateException("private SQL password"));
    when(repository.multipliers("JAVA")).thenReturn(Optional.empty());
    assertThatThrownBy(handler::getGeneratorSandboxLimits)
        .isInstanceOf(JudgeReadUnavailableException.class)
        .hasNoCause();
    assertThatThrownBy(handler::getEnabledLanguages)
        .isInstanceOf(JudgeReadUnavailableException.class)
        .hasNoCause();
    assertThatThrownBy(() -> handler.getLanguageMultipliers("JAVA"))
        .isInstanceOf(JudgeReadUnavailableException.class)
        .hasMessage("Judge read unavailable")
        .hasNoCause();
  }
}

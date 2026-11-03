package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.judge.application.dto.GradingSpecification;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.domain.model.SampleSettings;
import com.algoprep.judge.domain.model.WorkerSettings;
import com.algoprep.judge.domain.ports.out.SampleSettingsRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetEffectiveExecutionLimitsQueryHandlerTest {
  private final ProblemGradingSpecPort problems = mock(ProblemGradingSpecPort.class);
  private final SampleSettingsRepository settings = mock(SampleSettingsRepository.class);
  private final GetEffectiveExecutionLimitsQueryHandler handler =
      new GetEffectiveExecutionLimitsQueryHandler(problems, settings);
  private final UUID problem = UUID.randomUUID();

  private SampleSettings language(String name, String factor, boolean enabled) {
    return new SampleSettings(
        name,
        enabled,
        new WorkerSettings(
            new BigDecimal(factor), new BigDecimal(factor), 1000, 64, 64, 10, false, true, false));
  }

  @Test
  void allLanguagesAndDisabledLanguageUseHalfUpWithoutClamping() {
    var spec =
        new GradingSpecification(
            new GradingSpecification.Problem(problem, true, true, false),
            new GradingSpecification.Limits(1005, 65, 64, null),
            null,
            null,
            List.of());
    when(problems.loadGradingSpec(problem)).thenReturn(Optional.of(spec));
    when(settings.read())
        .thenReturn(
            List.of(
                language("JAVA", "0.10", true),
                language("CPP", "9.99", true),
                language("PYTHON", "1.5", false)));
    var all = handler.get(problem, null).limits();
    assertThat(all).hasSize(3);
    assertThat(all.getFirst().timeLimitMs()).isEqualTo(101);
    assertThat(all.getFirst().memoryLimitMb()).isEqualTo(7);
    assertThat(all.get(1).timeLimitMs()).isEqualTo(10040);
    assertThat(handler.get(problem, "python").limits().getFirst().enabled()).isFalse();
  }

  @Test
  void invalidLanguageAndMissingOrUnpublishedProblemAreRejected() {
    assertThatThrownBy(() -> handler.get(problem, "RUST"))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-V03"));
    when(problems.loadGradingSpec(problem)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> handler.get(problem, null))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-101"));
    verifyNoInteractions(settings);
  }
}

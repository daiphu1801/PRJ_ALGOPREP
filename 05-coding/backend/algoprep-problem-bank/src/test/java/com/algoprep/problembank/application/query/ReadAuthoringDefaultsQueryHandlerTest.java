package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.algoprep.common.exception.ServiceUnavailableException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.application.dto.AuthoringDefaults;
import com.algoprep.problembank.application.dto.JudgeGradingDefaults;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import org.junit.jupiter.api.Test;

class ReadAuthoringDefaultsQueryHandlerTest {
  @Test
  void defaultsReflectCurrentJudgeAndAiConfigurationInsteadOfFrontendConstants() {
    var judge = mock(JudgeConfigReadPort.class);
    var repository = mock(ProblemAuthoringReadRepository.class);
    when(judge.getGradingDefaults()).thenReturn(new JudgeGradingDefaults(4321, 512, 128, 9));
    when(repository.aiGenerationLimit()).thenReturn(7);
    assertThat(new ReadAuthoringDefaultsQueryHandler(judge, repository).get())
        .isEqualTo(new AuthoringDefaults(4321, 512, 128, 7));
  }
  @Test void infrastructureFailureIsFixed503WithoutRawCause() {
    var judge=mock(JudgeConfigReadPort.class); var repository=mock(ProblemAuthoringReadRepository.class);
    when(judge.getGradingDefaults()).thenThrow(new IllegalStateException("sql secret"));
    assertThatThrownBy(()->new ReadAuthoringDefaultsQueryHandler(judge,repository).get())
      .isInstanceOf(ServiceUnavailableException.class).hasMessage("Authoring defaults unavailable").hasNoCause();
  }

}

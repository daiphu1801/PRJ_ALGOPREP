package com.algoprep.judge.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.judge.application.dto.LearnerListLimit;
import com.algoprep.judge.application.ports.in.GetEnabledJudgeLanguagesUseCase;
import com.algoprep.judge.application.ports.in.ListMyAcceptedSubmissionsUseCase;
import com.algoprep.judge.application.ports.in.ListMyInProgressProblemsUseCase;
import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import java.util.List;
import org.junit.jupiter.api.Test;

class LearnerListControllersTest {
  private final ListMyInProgressProblemsUseCase progress =
      mock(ListMyInProgressProblemsUseCase.class);
  private final ListMyAcceptedSubmissionsUseCase accepted =
      mock(ListMyAcceptedSubmissionsUseCase.class);
  private final LearnerSubmissionListsController controller =
      new LearnerSubmissionListsController(progress, accepted);

  @Test
  void defaultsAndBoundaryLimitsUseSeparateEndpointDefaults() {
    assertThat(controller.inProgress(null).items()).isEmpty();
    assertThat(controller.accepted(null).items()).isEmpty();
    controller.inProgress("1");
    controller.accepted("20");
    verify(progress).inProgress(new LearnerListLimit(3));
    verify(accepted).accepted(new LearnerListLimit(10));
    verify(progress).inProgress(new LearnerListLimit(1));
    verify(accepted).accepted(new LearnerListLimit(20));
  }

  @Test
  void invalidRawLimitsAreV14BeforeEitherUseCaseRuns() {
    for (String invalid : List.of("0", "21", "-1", "bad", "", "1.5", "2147483648")) {
      assertThatThrownBy(() -> controller.inProgress(invalid))
          .isInstanceOfSatisfying(
              InvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-V14"));
      assertThatThrownBy(() -> controller.accepted(invalid))
          .isInstanceOf(InvalidRequestException.class);
    }
    verifyNoInteractions(progress, accepted);
  }

  @Test
  void publicLanguageOrderIsIndependentOfTheInternalP3OrderAndCannotBeEmpty() {
    var port = mock(GetEnabledJudgeLanguagesUseCase.class);
    var api = new EnabledSubmissionLanguagesController(port);
    when(port.getEnabledLanguages())
        .thenReturn(List.of("CPP", "JAVA", "PYTHON"), List.of("CPP", "PYTHON"), List.of());
    assertThat(api.enabled().items()).containsExactly("PYTHON", "JAVA", "CPP");
    assertThat(api.enabled().items()).containsExactly("PYTHON", "CPP");
    assertThatThrownBy(api::enabled).isInstanceOf(JudgeReadUnavailableException.class);
  }
}

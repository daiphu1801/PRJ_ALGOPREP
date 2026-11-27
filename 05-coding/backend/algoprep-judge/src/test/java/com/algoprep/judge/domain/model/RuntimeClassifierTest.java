package com.algoprep.judge.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.functionspec.Language;
import com.algoprep.judge.application.classify.RuntimeClassifier;
import com.algoprep.judge.application.dto.ExecutionModels.Model;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.domain.exception.JudgeInfrastructureException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RuntimeClassifierTest {
  private final RuntimeClassifier classifier = new RuntimeClassifier();

  static Stream<Arguments> markers() {
    return Stream.of(
        Arguments.of(90, "@@APH-HARNESS-INPUT-ERROR@@\n", Status.INPUT_ERROR),
        Arguments.of(91, "@@APH-HARNESS-OUTPUT-LIMIT@@\n", Status.RE),
        Arguments.of(92, "@@APH-HARNESS-SIGNATURE-ERROR@@\n", Status.SIGNATURE_ERROR));
  }

  @ParameterizedTest
  @MethodSource("markers")
  void requiresExactMarkerModelAndEngineStatus(int exit, String marker, Status expected) {
    assertThat(
            classifier.classify(
                "Nonzero Exit Status", exit, marker, Model.FUNCTION_WRAPPER, Language.PYTHON))
        .isEqualTo(expected);
    assertThat(
            classifier.classify(
                "Nonzero Exit Status", exit, marker, Model.STANDARD_IO, Language.PYTHON))
        .isEqualTo(Status.RE);
    assertThat(
            classifier.classify(
                "Nonzero Exit Status",
                exit,
                marker + "secret",
                Model.FUNCTION_WRAPPER,
                Language.PYTHON))
        .isEqualTo(Status.RE);
    assertThat(
            classifier.classify(
                "Nonzero Exit Status", exit, "", Model.FUNCTION_WRAPPER, Language.PYTHON))
        .isEqualTo(Status.RE);
    assertThat(
            classifier.classify("Accepted", exit, marker, Model.FUNCTION_WRAPPER, Language.PYTHON))
        .isEqualTo(Status.RE);
    assertThat(
            classifier.classify(
                "Time Limit Exceeded", exit, marker, Model.FUNCTION_WRAPPER, Language.PYTHON))
        .isEqualTo(Status.TLE);
    assertThat(
            classifier.classify(
                "Memory Limit Exceeded", exit, marker, Model.FUNCTION_WRAPPER, Language.PYTHON))
        .isEqualTo(Status.MLE);
    assertThat(
            classifier.classify(
                "Output Limit Exceeded", exit, marker, Model.FUNCTION_WRAPPER, Language.PYTHON))
        .isEqualTo(Status.RE);
    assertThat(
            classifier.classify("Signalled", exit, marker, Model.FUNCTION_WRAPPER, Language.PYTHON))
        .isEqualTo(Status.RE);
    assertThatThrownBy(
            () ->
                classifier.classify(
                    "Internal Error", exit, marker, Model.FUNCTION_WRAPPER, Language.PYTHON))
        .isInstanceOf(JudgeInfrastructureException.class);
  }

  @Test
  void pythonGuardIsNotAJavaOrCppContract() {
    assertThat(
            classifier.classify(
                "Nonzero Exit Status",
                92,
                "@@APH-HARNESS-SIGNATURE-ERROR@@\n",
                Model.FUNCTION_WRAPPER,
                Language.JAVA))
        .isEqualTo(Status.RE);
    assertThat(classifier.classify("Accepted", 0, "", Model.STANDARD_IO, Language.CPP))
        .isEqualTo(Status.OK);
  }
}

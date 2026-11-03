package com.algoprep.judge.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CreateSubmissionErrorAdviceTest {
  private final CreateSubmissionErrorAdvice advice = new CreateSubmissionErrorAdvice();

  @ParameterizedTest
  @CsvSource({
    "JDG-101,404",
    "JDG-102,422",
    "JDG-103,422",
    "JDG-104,422",
    "JDG-106,429",
    "JDG-503,503",
    "JDG-504,503",
    "JDG-V01,400",
    "JDG-V02,400",
    "JDG-V03,400",
    "JDG-V04,400",
    "JDG-V05,400",
    "JDG-V06,400"
  })
  void exactContractStatus(String code, int status) {
    var result = advice.admission(new SubmissionAdmissionException(code));
    assertThat(result.getStatus()).isEqualTo(status);
    assertThat(result.getProperties())
        .containsEntry("errorCode", code)
        .doesNotContainKey("details");
  }

  @Test
  void metadataAndGenericInfrastructureResponseStaySeparate() {
    var result = advice.admission(new SubmissionAdmissionException("JDG-106", Map.of("limit", 60)));
    assertThat(result.getProperties()).containsEntry("details", Map.of("limit", 60));
    assertThat(advice.unavailable().getStatus()).isEqualTo(503);
    assertThat(advice.unavailable().getProperties())
        .containsEntry("errorCode", "JDG-505")
        .doesNotContainKey("details");
    assertThat(advice.unreadable().getStatus()).isEqualTo(400);
  }
}

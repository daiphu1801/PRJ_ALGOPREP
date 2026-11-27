package com.algoprep.harness.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.common.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StarterCodeErrorAdviceTest {
  private final StarterCodeErrorAdvice advice = new StarterCodeErrorAdvice();

  @ParameterizedTest
  @CsvSource({
    "HRN-001,404,HRN-001",
    "HRN-004,422,HRN-004",
    "HRN-005,422,HRN-005",
    "HRN-010,400,HRN-010",
    "HRN-003,503,HRN-003",
    "HRN-006,500,HRN-006",
    "HRN-030,500,HRN-030",
    "HRN-031,500,HRN-031",
    "OTHER,500,HRN-031"
  })
  void mapsEveryDocumentedErrorWithoutReturningUpstreamMessage(
      String input, int status, String output) {
    var result = advice.known(new DomainRuleViolationException(input, "private SQL and signature"));
    assertThat(result.getStatus()).isEqualTo(status);
    assertThat(result.getProperties()).containsEntry("errorCode", output);
    assertThat(result.getDetail()).isEqualTo("Starter code unavailable");
  }

  @Test
  void dependencyBindingForbiddenAndUnexpectedAreFixed() {
    assertThat(advice.dependency().getStatus()).isEqualTo(503);
    assertThat(advice.dependency().getProperties()).containsEntry("errorCode", "HRN-003");
    assertThat(advice.binding().getStatus()).isEqualTo(400);
    assertThat(advice.forbidden().getStatus()).isEqualTo(403);
    assertThat(advice.unexpected().getStatus()).isEqualTo(500);
    assertThat(advice.unexpected().getProperties()).containsEntry("errorCode", "HRN-031");
  }
}

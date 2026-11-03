package com.algoprep.judge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.application.dto.GradingSpecification;
import com.algoprep.judge.application.dto.GradingTestcase;
import com.algoprep.judge.application.dto.GradingTestcaseSnapshot;
import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GradingReadModelsTest {
  private final UUID id = UUID.randomUUID();

  @Test
  void trustedTestcaseOwnsBothArraysAndDoesNotPrintHiddenContent() {
    byte[] source = "hidden secret".getBytes(StandardCharsets.UTF_8);
    var row = new GradingTestcase(id, "HIDDEN", 1, source, source);
    source[0] = 0;
    row.input()[0] = 0;
    row.expectedOutput()[0] = 0;
    assertThat(row.input()).containsExactly("hidden secret".getBytes(StandardCharsets.UTF_8));
    assertThat(row.expectedOutput()).containsExactly(row.input());
    assertThat(row.toString()).doesNotContain("hidden secret", "input=", "expectedOutput=");
  }

  @Test
  void pinnedSnapshotOwnsAnImmutableList() {
    var rows = new ArrayList<GradingTestcase>();
    rows.add(new GradingTestcase(id, "SAMPLE", 1, new byte[0], new byte[0]));
    var result = new GradingTestcaseSnapshot(id, 7, rows);
    rows.clear();
    assertThat(result.testcases()).hasSize(1);
    assertThatThrownBy(() -> result.testcases().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void gradingMetadataDoesNotChangeWhenTheProvidersListIsModified() {
    var rows = new ArrayList<GradingSpecification.Testcase>();
    rows.add(new GradingSpecification.Testcase(id, "HIDDEN", 2));
    var result =
        new GradingSpecification(
            new GradingSpecification.Problem(id, false, false, false),
            new GradingSpecification.Limits(1000, 256, 64, null),
            new GradingSpecification.Versions(130, 7),
            new GradingSpecification.Matching("EXACT", null),
            rows);
    rows.clear();
    assertThat(result.testcases()).hasSize(1);
    assertThat(result.limits().maxSubmissionsPerHour()).isNull();
    assertThatThrownBy(() -> result.testcases().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void typedFailuresContainOnlyFixedTextAndAnInternalReason() {
    for (var reason : ProblemGradingReadException.Reason.values()) {
      var failure = new ProblemGradingReadException(reason);
      assertThat(failure.reason()).isEqualTo(reason);
      assertThat(failure.getMessage()).isEqualTo("Problem grading data is unavailable");
      assertThat(failure.getCause()).isNull();
    }
  }
}

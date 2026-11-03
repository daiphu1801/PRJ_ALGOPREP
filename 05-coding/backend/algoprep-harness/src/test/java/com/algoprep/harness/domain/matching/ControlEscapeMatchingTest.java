package com.algoprep.harness.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ControlEscapeMatchingTest {
  private final TypeNode type =
      new TypeNode(TypeKind.LIST, new TypeNode(TypeKind.STRING, null, null), null);

  @ParameterizedTest
  @ValueSource(strings = {"t", "r", "b", "f", "n"})
  void namedControlEscapesHaveTheSameValueAsTheirHexForm(String escape) {
    String named = "[\"a\\" + escape + "b\"]";
    String hex =
        switch (escape) {
          case "t" -> "0009";
          case "r" -> "000d";
          case "b" -> "0008";
          case "f" -> "000c";
          default -> "000a";
        };
    assertThat(
            OutputMatching.matches(
                named,
                "[\"a\\u" + hex + "b\"]",
                MatchingStrategy.EPSILON,
                type,
                new BigDecimal("0.001")))
        .isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"0000", "0001", "001f", "001F"})
  void acceptsAllControlEscapeBoundaries(String hex) {
    String value = "[\"\\u" + hex + "\"]";
    assertThat(OutputMatching.matches(value, value, MatchingStrategy.UNORDERED_SET, type, null))
        .isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"\\u", "\\u000", "\\u00zz", "\\u0020", "\\u1234", "\\q"})
  void malformedOrOutOfContractEscapesAreNoMatchForActualAndFailClosedForExpected(String escape) {
    String value = "[\"" + escape + "\"]";
    assertThat(OutputMatching.matches(value, "[]", MatchingStrategy.UNORDERED_SET, type, null))
        .isFalse();
    assertThatThrownBy(
            () -> OutputMatching.matches("[]", value, MatchingStrategy.UNORDERED_SET, type, null))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo("HRN-050"));
  }
}

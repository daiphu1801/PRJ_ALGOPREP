package com.algoprep.harness.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AsciiCharacterMatchingTest {
  private final TypeNode type = new TypeNode(TypeKind.CHAR, null, null);

  @ParameterizedTest
  @ValueSource(strings = {"!", "~", "a", "#"})
  void printableAsciiIsAccepted(String value) {
    assertThat(OutputMatching.matches(value, value, MatchingStrategy.UNORDERED_SET, type, null))
        .isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "é", "😀", "\t", "aa", "\u007f"})
  void invalidCharactersAreNoMatchAndInvalidExpectedFailsClosed(String value) {
    assertThat(OutputMatching.matches(value, "a", MatchingStrategy.UNORDERED_SET, type, null))
        .isFalse();
    assertThatThrownBy(
            () -> OutputMatching.matches("a", value, MatchingStrategy.UNORDERED_SET, type, null))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo("HRN-050"));
  }
}

package com.algoprep.problembank.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CatalogSlugTest {
  static Stream<Arguments> cases() {
    return Stream.of(
        Arguments.of(" Rất khó ", CatalogKind.LEVEL, 1, "RAT-KHO"),
        Arguments.of("Đồ thị", CatalogKind.TOPIC, 1, "DO-THI"),
        Arguments.of("aĐ  đ--b", CatalogKind.TOPIC, 2, "AD-D-B-2"),
        Arguments.of("!!!", CatalogKind.TOPIC, 1, "TOPIC"),
        Arguments.of("###", CatalogKind.LEVEL, 2, "LEVEL-2"),
        Arguments.of("a".repeat(64), CatalogKind.LEVEL, 20, "A".repeat(61) + "-20"),
        Arguments.of("a".repeat(62) + "--b", CatalogKind.LEVEL, 2, "A".repeat(62) + "-2"));
  }

  @ParameterizedTest
  @MethodSource("cases")
  void followsDdSlugContract(String input, CatalogKind kind, int attempt, String expected) {
    assertThat(CatalogSlug.create(input, kind, attempt)).isEqualTo(expected);
  }
}

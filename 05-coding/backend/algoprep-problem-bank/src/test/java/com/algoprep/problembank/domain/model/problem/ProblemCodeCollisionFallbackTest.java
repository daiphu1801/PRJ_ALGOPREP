package com.algoprep.problembank.domain.model.problem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProblemCodeCollisionFallbackTest {
  @ParameterizedTest
  @ValueSource(strings = {"Hello World", "Đếm số phần tử!", "! !", "---------------------------x"})
  void fallbackKeepsTheWholeIdentityWithinTheColumnLimit(String title) {
    UUID first = UUID.fromString("abcdefab-0000-4000-8000-000000000001");
    UUID second = UUID.fromString("abcdefab-0000-4000-8000-000000000002");
    String a = ProblemCode.withIdentitySuffix(title.repeat(40), first);
    String b = ProblemCode.withIdentitySuffix(title.repeat(40), second);
    assertThat(a).hasSizeLessThanOrEqualTo(64).endsWith("ABCDEFAB-0000-4000-8000-000000000001");
    assertThat(a).doesNotContain("--").isNotEqualTo(b);
  }
}

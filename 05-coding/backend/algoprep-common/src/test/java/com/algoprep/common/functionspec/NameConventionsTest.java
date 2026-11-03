package com.algoprep.common.functionspec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NameConventionsTest {
  @Test
  void splitsAcronymsCaseBoundariesUnderscoresAndDigits() {
    assertNames("HTTPServer", "httpServer", "http_server");
    assertNames("two_sum", "twoSum", "two_sum");
    assertNames("twoSum", "twoSum", "two_sum");
    assertNames("parse2HTTPServer", "parse2HttpServer", "parse2_http_server");
    assertNames("two__sum_", "twoSum", "two_sum");
  }

  @Test
  void overridesAreCaseInsensitiveAndUsedVerbatim() {
    assertThat(NameConventions.forLanguage("two_sum", Language.JAVA, Map.of("java", "SOLVE")))
        .isEqualTo("SOLVE");
    assertThat(NameConventions.forLanguage("two_sum", Language.PYTHON, null)).isEqualTo("two_sum");
    assertThat(NameConventions.RESERVED_WORDS.get(Language.PYTHON))
        .contains("False", "None", "True");
    assertThatThrownBy(() -> NameConventions.RESERVED_WORDS.get(Language.JAVA).add("solve"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static void assertNames(String raw, String camel, String snake) {
    assertThat(NameConventions.forLanguage(raw, Language.JAVA, Map.of())).isEqualTo(camel);
    assertThat(NameConventions.forLanguage(raw, Language.CPP, Map.of())).isEqualTo(camel);
    assertThat(NameConventions.forLanguage(raw, Language.PYTHON, Map.of())).isEqualTo(snake);
  }
}

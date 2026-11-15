package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.application.command.GeneratorOutputParser.GeneratedCase;
import com.algoprep.problembank.domain.exception.TestcaseGenerationRejectedException;
import org.junit.jupiter.api.Test;

class GeneratorOutputParserTest {
  @Test
  void splitsCasesAtTheirHeadersAndKeepsKnownLabelsOnly() {
    var cases =
        GeneratorOutputParser.parse(
            "###CASE BOUNDARY_LOW\n1\n2\n###CASE weird\n3\n###CASE\n4\n###CASE  extreme \n5\n");

    assertThat(cases)
        .containsExactly(
            new GeneratedCase("BOUNDARY_LOW", "1\n2\n"),
            new GeneratedCase(null, "3\n"),
            new GeneratedCase(null, "4\n"),
            new GeneratedCase("EXTREME", "5\n"));
  }

  @Test
  void outputWithNoHeaderIsOneUnlabelledCase() {
    assertThat(GeneratorOutputParser.parse("3\r\n1 2 3\r\n\r\n"))
        .containsExactly(new GeneratedCase(null, "3\n1 2 3\n"));
  }

  @Test
  void leadingBlankLinesBeforeTheFirstHeaderAreIgnored() {
    assertThat(GeneratorOutputParser.parse("\n\n###CASE TYPICAL\n7\n"))
        .containsExactly(new GeneratedCase("TYPICAL", "7\n"));
  }

  @Test
  void emptyTextBeforeAHeaderAndAnEmptyCaseAreInvalidFormat() {
    for (String bad :
        new String[] {null, "", "  \n", "oops\n###CASE TYPICAL\n1\n", "###CASE TYPICAL\n\n"}) {
      assertThatThrownBy(() -> GeneratorOutputParser.parse(bad))
          .isInstanceOf(TestcaseGenerationRejectedException.class)
          .satisfies(
              e ->
                  assertThat(((TestcaseGenerationRejectedException) e).clientErrorDetails())
                      .containsEntry("reason", "INVALID_FORMAT"));
    }
  }

  @Test
  void normalizeKeepsExactlyOneTrailingNewline() {
    assertThat(GeneratorOutputParser.normalize("a\n\n\n")).isEqualTo("a\n");
    assertThat(GeneratorOutputParser.normalize("a")).isEqualTo("a\n");
  }
}

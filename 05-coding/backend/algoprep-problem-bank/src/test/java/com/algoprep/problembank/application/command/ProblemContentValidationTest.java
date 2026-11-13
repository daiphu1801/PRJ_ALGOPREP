package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.domain.exception.ProblemContentInvalidException;
import com.algoprep.problembank.domain.model.ProblemContentData;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.env.MockEnvironment;

class ProblemContentValidationTest {
  private final ProblemContentValidation validation =
      new ProblemContentValidation(new MockEnvironment());

  static Stream<Arguments> invalid() {
    return Stream.concat(invalidTextAndLimits(), invalidRelations());
  }

  private static Stream<Arguments> invalidTextAndLimits() {
    return Stream.of(
        Arguments.of(null, "content", "required"),
        bad("title", " ", "title", "required"),
        bad("title", "a".repeat(256), "title", "maxLength"),
        bad("statementMd", null, "statementMd", "required"),
        bad("statementMd", "x".repeat(100001), "statementMd", "maxLength"),
        bad("statementMd", "x\0y", "statementMd", "text"),
        bad("constraintsMd", "x".repeat(20001), "constraintsMd", "maxLength"),
        bad("limits", null, "limits", "required"),
        bad(
            "limits",
            new ProblemContentData.Limits(0, 1, 1, null),
            "limits.timeLimitMs",
            "positiveInt"),
        bad(
            "limits",
            new ProblemContentData.Limits(1, 0, 1, null),
            "limits.memoryLimitMb",
            "positiveInt"),
        bad(
            "limits",
            new ProblemContentData.Limits(1, 1, 0, null),
            "limits.maxOutputSizeKb",
            "positiveInt"),
        bad(
            "limits",
            new ProblemContentData.Limits(1, 1, 1, 0),
            "limits.maxSubmissionsPerHour",
            "positiveInt"));
  }

  private static Stream<Arguments> invalidRelations() {
    return Stream.of(
        bad("topicIds", Arrays.asList((UUID) null), "topicIds", "exists"),
        bad("tags", List.of(" "), "tags", "required"),
        bad("tags", List.of("x".repeat(51)), "tags", "maxLength"),
        bad("tags", IntStream.range(0, 21).mapToObj(i -> "tag" + i).toList(), "tags", "maxItems"),
        bad(
            "workedExamples",
            Arrays.asList((ProblemContentData.Example) null),
            "workedExamples",
            "required"),
        example("", "2", null, "workedExamples.input", "required"),
        example("1", "", null, "workedExamples.output", "required"),
        example("é".repeat(4097), "2", null, "workedExamples.input", "maxBytes"),
        example("1", "é".repeat(4097), null, "workedExamples.output", "maxBytes"),
        example("1", "2", "x".repeat(4001), "workedExamples.explanationMd", "maxLength"),
        bad(
            "referenceSolution",
            new ProblemContentData.Solution("RUBY", "code"),
            "referenceSolution.language",
            "enum"),
        bad(
            "referenceSolution",
            new ProblemContentData.Solution(null, "code"),
            "referenceSolution.language",
            "enum"),
        bad(
            "referenceSolution",
            new ProblemContentData.Solution("JAVA", "é".repeat(32769)),
            "referenceSolution.sourceCode",
            "maxBytes"));
  }

  private static Arguments bad(String name, Object value, String field, String rule) {
    return Arguments.of(ContentTestData.replace(name, value), field, rule);
  }

  private static Arguments example(
      String input, String output, String explanation, String field, String rule) {
    return bad(
        "workedExamples",
        List.of(new ProblemContentData.Example(null, 0, input, output, explanation)),
        field,
        rule);
  }

  @ParameterizedTest
  @MethodSource("invalid")
  void errorsIdentifyFieldAndRuleWithoutEchoingSensitiveValues(
      ProblemContentData input, String field, String rule) {
    assertThatThrownBy(() -> validation.normalize(input))
        .isInstanceOfSatisfying(
            ProblemContentInvalidException.class,
            error -> {
              assertThat(error.errorCode()).isEqualTo("PRB-001");
              assertThat(error.clientErrorDetails())
                  .isEqualTo(
                      java.util.Map.of(
                          "fields", List.of(java.util.Map.of("field", field, "rule", rule))));
            });
  }

  @Test
  void boundariesPreserveMarkdownSourceAndNormalizeOnlyTitleTagsAndNullConstraints() {
    var source = new ProblemContentData.Solution("PYTHON", " " + "x".repeat(65534) + "\n");
    var data = ContentTestData.replace("title", " " + "é".repeat(255) + " ");
    data = ContentTestData.replace(data, "statementMd", "x".repeat(100000));
    data = ContentTestData.replace(data, "constraintsMd", "x".repeat(20000));
    data = ContentTestData.replace(data, "referenceSolution", source);
    data =
        ContentTestData.replace(
            data,
            "workedExamples",
            List.of(
                new ProblemContentData.Example(
                    null, 100, "é".repeat(4096), "x".repeat(8192), "x".repeat(4000))));
    var result = validation.normalize(data);
    assertThat(result.title()).hasSize(255);
    assertThat(result.referenceSolution()).isEqualTo(source);
    assertThat(result.workedExamples()).isEqualTo(data.workedExamples());
    UUID topic = UUID.randomUUID();
    var duplicates = ContentTestData.replace("topicIds", List.of(topic, topic));
    duplicates = ContentTestData.replace(duplicates, "tags", List.of(" Mảng ", "mẢNG", "Cây"));
    var normalized = validation.normalize(duplicates);
    assertThat(normalized.topicIds()).containsExactly(topic);
    assertThat(normalized.tags()).containsExactly("Mảng", "Cây");
    assertThat(normalized.constraintsMd()).isEmpty();
    assertThat(normalized.statementMd()).isEqualTo(ContentTestData.valid().statementMd());
  }

  @Test
  void duplicateExampleIdsAreRejectedAndEmptySolutionDeletesTheRow() {
    var example = new ProblemContentData.Example(UUID.randomUUID(), 1, "1", "2", null);
    assertThatThrownBy(
            () ->
                validation.normalize(
                    ContentTestData.replace("workedExamples", List.of(example, example))))
        .isInstanceOf(ProblemContentInvalidException.class);
    for (String text : Arrays.asList(null, "", "  \n")) {
      assertThat(
              validation
                  .normalize(
                      ContentTestData.replace(
                          "referenceSolution", new ProblemContentData.Solution("PYTHON", text)))
                  .referenceSolution())
          .isNull();
    }
  }

  @Test
  void configuredCeilingsAreUsedAndPositiveQuotaIsPreserved() {
    var configured =
        new ProblemContentValidation(
            new MockEnvironment().withProperty("algoprep.problem-bank.authoring.max-tags", "1"));
    assertThatThrownBy(
            () -> configured.normalize(ContentTestData.replace("tags", List.of("a", "b"))))
        .isInstanceOf(ProblemContentInvalidException.class);
    var limits = new ProblemContentData.Limits(1, 1, 1, 1);
    assertThat(validation.normalize(ContentTestData.replace("limits", limits)).limits())
        .isEqualTo(limits);
  }
}

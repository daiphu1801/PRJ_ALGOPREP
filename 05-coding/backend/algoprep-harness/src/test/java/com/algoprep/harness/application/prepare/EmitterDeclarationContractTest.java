package com.algoprep.harness.application.prepare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.domain.codegen.CppTypeCodeEmitter;
import com.algoprep.harness.domain.codegen.JavaTypeCodeEmitter;
import com.algoprep.harness.domain.codegen.PythonTypeCodeEmitter;
import com.algoprep.harness.domain.codegen.TypeCodeEmitter;
import com.algoprep.harness.domain.signature.FunctionSignatureDeclarations;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EmitterDeclarationContractTest {
  private static final Map<Language, TypeCodeEmitter> EMITTERS =
      Map.of(
          Language.JAVA,
          new JavaTypeCodeEmitter(),
          Language.CPP,
          new CppTypeCodeEmitter(),
          Language.PYTHON,
          new PythonTypeCodeEmitter());

  static Stream<Arguments> contracts() {
    return FunctionWrapperRendererTest.fixtures().stream()
        .map(FunctionWrapperRendererTest.Fixture::type)
        .distinct()
        .flatMap(
            type -> Stream.of(Language.values()).map(language -> Arguments.of(language, type)));
  }

  @ParameterizedTest
  @MethodSource("contracts")
  void readCallTypesAgreeWithP6AndStarterDeclarations(Language language, TypeNode type) {
    String emitted =
        EMITTERS
            .get(language)
            .type(type)
            .replace("java.util.List", "List")
            .replace("std::vector", "vector");
    String declaration =
        FunctionSignatureDeclarations.render(FunctionWrapperRendererTest.spec(type)).get(language);
    String expected =
        language == Language.PYTHON
            ? " -> " + emitted + ":"
            : (language == Language.JAVA ? "public " : "") + emitted + " echoValue(";
    assertThat(declaration).contains(expected);
  }

  @Test
  void pythonEmitterAlsoRejectsTypesOutsideTheClosedSchema() {
    assertThatThrownBy(
            () -> new PythonTypeCodeEmitter().type(new TypeNode(TypeKind.OTHER, null, null)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

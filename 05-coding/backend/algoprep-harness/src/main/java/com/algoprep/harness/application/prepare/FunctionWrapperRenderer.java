package com.algoprep.harness.application.prepare;

import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.NameConventions;
import com.algoprep.harness.domain.codegen.CppTypeCodeEmitter;
import com.algoprep.harness.domain.codegen.JavaTypeCodeEmitter;
import com.algoprep.harness.domain.codegen.PythonTypeCodeEmitter;
import com.algoprep.harness.domain.codegen.TypeCodeEmitter;
import com.algoprep.harness.domain.model.RenderedProgram;
import com.algoprep.harness.domain.model.WrapperSource;
import com.algoprep.harness.infrastructure.template.ClasspathPythonSignatureGuard;
import com.algoprep.harness.infrastructure.template.ClasspathWrapperRuntimes;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Pure P2 codegen helper; has no F2 reader, judge payload, HTTP or execution responsibility. */
public final class FunctionWrapperRenderer {
  private static final Map<Language, TypeCodeEmitter> EMITTERS =
      Map.of(
          Language.JAVA,
          new JavaTypeCodeEmitter(),
          Language.CPP,
          new CppTypeCodeEmitter(),
          Language.PYTHON,
          new PythonTypeCodeEmitter());
  private final Map<Language, String> runtimes = ClasspathWrapperRuntimes.load();
  private final String guard = ClasspathPythonSignatureGuard.load();

  public RenderedProgram render(Language language, FunctionSpecDraft draft, String userSource) {
    String nodes = new NodeSourceRenderer().render(language, draft);
    String arguments =
        IntStream.range(0, draft.parameters().size())
            .mapToObj(index -> "apH_p" + index)
            .collect(Collectors.joining(", "));
    String name =
        NameConventions.forLanguage(draft.functionName(), language, draft.nameOverrides());
    var fragments =
        Map.of(
            Language.JAVA, javaSource(draft, nodes, arguments, name),
            Language.CPP, cppSource(draft, nodes, arguments, name),
            Language.PYTHON, pythonSource(draft, nodes, arguments, name));
    return new ProgramSourceAssembler()
        .functionWrapper(language, userSource, fragments.get(language));
  }

  private String reads(Language language, FunctionSpecDraft draft, String indent) {
    TypeCodeEmitter emitter = EMITTERS.get(language);
    return IntStream.range(0, draft.parameters().size())
        .mapToObj(
            index -> {
              var type = draft.parameters().get(index).type();
              String declaration = language == Language.PYTHON ? "" : emitter.type(type) + " ";
              String end = language == Language.PYTHON ? "\n" : ";\n";
              return indent + declaration + "apH_p" + index + " = " + emitter.read(type) + end;
            })
        .collect(Collectors.joining());
  }

  private WrapperSource javaSource(
      FunctionSpecDraft draft, String nodes, String arguments, String name) {
    var emitter = EMITTERS.get(Language.JAVA);
    String before =
        nodes
            + runtimes.get(Language.JAVA)
            + "class ApHRunner {\n  public static void main(String[] apH_args) throws Exception {\n"
            + "    ApHInput apH_in = new ApHInput();\n"
            + reads(Language.JAVA, draft, "    ")
            + "    apH_in.finish();\n";
    String call =
        "    "
            + emitter.type(draft.returnType())
            + " apH_r = new Solution()."
            + name
            + "("
            + arguments
            + ");\n";
    String after =
        "    ApHOutput apH_out = new ApHOutput();\n    "
            + emitter.write(draft.returnType(), "apH_r")
            + "    apH_out.publish();\n  }\n}\n";
    return new WrapperSource("import java.util.*;\nimport java.io.*;\n", before, call, after);
  }

  private WrapperSource cppSource(
      FunctionSpecDraft draft, String nodes, String arguments, String name) {
    var emitter = EMITTERS.get(Language.CPP);
    String before =
        runtimes.get(Language.CPP)
            + "int main() {\n  ApHInput apH_in;\n"
            + reads(Language.CPP, draft, "  ")
            + "  apH_in.finish();\n";
    String call =
        "  "
            + emitter.type(draft.returnType())
            + " apH_r = Solution()."
            + name
            + "("
            + arguments
            + ");\n";
    String after =
        "  ApHOutput apH_out;\n  "
            + emitter.write(draft.returnType(), "apH_r")
            + "  apH_out.publish();\n}\n";
    return new WrapperSource(
        "#include <bits/stdc++.h>\nusing namespace std;\n" + nodes, before, call, after);
  }

  private WrapperSource pythonSource(
      FunctionSpecDraft draft, String nodes, String arguments, String name) {
    var emitter = EMITTERS.get(Language.PYTHON);
    String before =
        runtimes.get(Language.PYTHON)
            + guard
            + "\napH_in = ApHInput()\n"
            + reads(Language.PYTHON, draft, "")
            + "apH_in.finish()\napH_fn = apH_resolve_call(\""
            + name
            + "\", ["
            + arguments
            + "])\n";
    String after =
        "apH_out = ApHOutput()\n"
            + emitter.write(draft.returnType(), "apH_r")
            + "apH_out.publish()\n";
    return new WrapperSource(
        "from typing import *\n" + nodes, before, "apH_r = apH_fn(" + arguments + ")\n", after);
  }
}

package com.algoprep.harness.application.prepare;

import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.dto.ExecutionPlan;
import com.algoprep.harness.application.dto.ExecutionPlan.Command;
import com.algoprep.harness.application.dto.ExecutionPlan.SourceFile;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.domain.model.RenderedProgram;
import java.util.List;
import java.util.Map;

/** Fixed toolchains verified against the isolated judge image, DD F3 section 5.2. */
public final class ToolchainProfile {
  private static final List<String> ENV = List.of("PATH=/usr/bin:/bin", "LC_ALL=C");
  private static final List<String> PYTHON_ENV =
      List.of(
          "PATH=/usr/bin:/bin", "LC_ALL=C", "PYTHONIOENCODING=utf-8", "PYTHONDONTWRITEBYTECODE=1");

  public ExecutionPlan plan(Language language, SubmissionModel model, RenderedProgram source) {
    var profile = profiles(model, source.sourceFileName()).get(language);
    return new ExecutionPlan(
        List.of(new SourceFile(source.sourceFileName(), source.text())),
        profile.compile(),
        profile.run(),
        profile.artifact());
  }

  private Map<Language, ExecutionPlan> profiles(SubmissionModel model, String file) {
    String main = model == SubmissionModel.FUNCTION_WRAPPER ? "ApHRunner" : "Main";
    return Map.of(
        Language.JAVA,
            new ExecutionPlan(
                List.of(),
                new Command(List.of("/bin/sh", "/opt/algoprep/build-java.sh", file), ENV),
                new Command(
                    List.of("/usr/bin/java", "-XX:+UseSerialGC", "-cp", "/w/classes.jar", main),
                    ENV),
                "classes.jar"),
        Language.CPP,
            new ExecutionPlan(
                List.of(),
                new Command(
                    List.of(
                        "/usr/bin/g++",
                        "-std=c++20",
                        "-O2",
                        "-fno-diagnostics-show-caret",
                        "-fno-diagnostics-color",
                        "-fmax-errors=20",
                        "-o",
                        "main",
                        "main.cpp"),
                    ENV),
                new Command(List.of("/w/main"), ENV),
                "main"),
        Language.PYTHON,
            new ExecutionPlan(
                List.of(),
                new Command(List.of("/usr/bin/python3", "-m", "py_compile", "main.py"), PYTHON_ENV),
                new Command(List.of("/usr/bin/python3", "main.py"), PYTHON_ENV),
                null));
  }
}

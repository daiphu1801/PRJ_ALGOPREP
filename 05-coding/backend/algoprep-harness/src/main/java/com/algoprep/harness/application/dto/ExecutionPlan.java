package com.algoprep.harness.application.dto;

import java.util.List;

/** Engine-neutral source and toolchain; contains no testcase data or resource limits. */
public record ExecutionPlan(List<SourceFile> files, Command compile, Command run, String artifact) {
  public ExecutionPlan {
    files = List.copyOf(files);
  }

  public record SourceFile(String name, String content) {}

  public record Command(List<String> argv, List<String> env) {
    public Command {
      argv = List.copyOf(argv);
      env = List.copyOf(env);
    }
  }
}

package com.algoprep.harness.domain.compilation;

/** Parsed header only. Source snippets, carets and compiler context are deliberately absent. */
record CompilerDiagnostic(String file, int line, Integer column, String severity, String message) {
  boolean isError() {
    return severity.equals("error") || severity.equals("fatal error");
  }
}

package com.algoprep.harness.domain.model;

/** Source regions from PreparedProgram, DD F3 api section 2.3.2. */
public record LineMap(
    int prefixLineCount,
    int userLineCount,
    Integer callSiteStartLine,
    Integer callSiteEndLine,
    String sourceFileName) {
  public boolean isUserLine(int line) {
    return line > prefixLineCount && line <= (long) prefixLineCount + userLineCount;
  }

  public boolean isCallSite(int line) {
    return callSiteStartLine != null && line >= callSiteStartLine && line <= callSiteEndLine;
  }
}

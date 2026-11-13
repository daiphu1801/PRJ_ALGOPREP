package com.algoprep.judge.domain.model;

/** Raw UTF-8 debug budget, independent of the result and engine collector budgets. */
public final class WrapperDebugBudget {
  private static final int MAX_BYTES = 4096;

  private WrapperDebugBudget() {}

  public static boolean exceeded(String stdout, String sentinel) {
    if (sentinel == null || sentinel.isEmpty()) {
      return false;
    }
    int prefixEnd = lastPrefixEnd(stdout, sentinel);
    return prefixEnd >= 0 && exceedsUtf8(stdout, prefixEnd);
  }

  private static int lastPrefixEnd(String stdout, String sentinel) {
    int prefixEnd = -1;
    int start = 0;
    while (start <= stdout.length()) {
      int end = start;
      while (end < stdout.length() && stdout.charAt(end) != '\r' && stdout.charAt(end) != '\n') {
        end++;
      }
      if (end - start == sentinel.length()
          && stdout.regionMatches(start, sentinel, 0, sentinel.length())) {
        // Exclude only the LF starting the trusted frame, never a preceding learner CR.
        prefixEnd = start > 0 && stdout.charAt(start - 1) == '\n' ? start - 1 : start;
      }
      if (end == stdout.length()) {
        break;
      }
      start = end + 1;
      if (stdout.charAt(end) == '\r' && start < stdout.length() && stdout.charAt(start) == '\n') {
        start++;
      }
    }
    return prefixEnd;
  }

  private static boolean exceedsUtf8(String text, int end) {
    int bytes = 0;
    for (int index = 0; index < end; index++) {
      char current = text.charAt(index);
      if (current <= 0x7f) {
        bytes++;
      } else if (current <= 0x7ff) {
        bytes += 2;
      } else if (Character.isHighSurrogate(current)
          && index + 1 < end
          && Character.isLowSurrogate(text.charAt(index + 1))) {
        bytes += 4;
        index++;
      } else {
        bytes += Character.isSurrogate(current) ? 1 : 3;
      }
      if (bytes > MAX_BYTES) {
        return true;
      }
    }
    return false;
  }
}

package com.algoprep.common.text;

/**
 * Strips chosen characters from the ends of a string in one linear pass. Replaces regex idioms such
 * as {@code ^_+|_+$} whose backtracking is super-linear on long runs of the stripped character.
 */
public final class EdgeTrim {
  private EdgeTrim() {}

  /** Removes every leading and trailing character that appears in {@code chars}. */
  public static String edges(String value, String chars) {
    int start = 0;
    int end = value.length();
    while (start < end && chars.indexOf(value.charAt(start)) >= 0) {
      start++;
    }
    while (end > start && chars.indexOf(value.charAt(end - 1)) >= 0) {
      end--;
    }
    return value.substring(start, end);
  }

  /** Removes every trailing character that appears in {@code chars}. */
  public static String trailing(String value, String chars) {
    int end = value.length();
    while (end > 0 && chars.indexOf(value.charAt(end - 1)) >= 0) {
      end--;
    }
    return value.substring(0, end);
  }
}

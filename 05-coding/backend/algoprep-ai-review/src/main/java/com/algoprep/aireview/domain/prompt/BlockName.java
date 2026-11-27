package com.algoprep.aireview.domain.prompt;

/** The named data blocks of the user message (DD F5 section 3.1, layer 3). */
public enum BlockName {
  USER_CODE("user_code"),
  PROBLEM("problem"),
  SAMPLES("samples"),
  QUESTION("question"),
  TOPICS("topics"),
  TRANSCRIPT("transcript"),
  ANSWER("answer");

  private final String tag;

  BlockName(String tag) {
    this.tag = tag;
  }

  public String tag() {
    return tag;
  }
}

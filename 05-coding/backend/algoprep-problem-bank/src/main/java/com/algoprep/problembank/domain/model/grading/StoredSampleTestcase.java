package com.algoprep.problembank.domain.model;

import java.util.UUID;

/** Both storage branches are independent; these references never leave the F2 reader. */
public record StoredSampleTestcase(
    UUID testcaseId, int position, Content input, Content expectedOutput) {
  public record Content(String storage, String inline, String objectKey) {}
}

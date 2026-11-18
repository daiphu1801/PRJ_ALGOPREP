package com.algoprep.problembank.domain.model;

import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import java.util.UUID;

/** One testcase row as written by authoring; each side is exactly INLINE or MINIO. */
public record StoredTestcase(
    UUID id,
    String visibility,
    String origin,
    boolean approved,
    String category,
    int position,
    StoredGradingTestcase.Content input,
    StoredGradingTestcase.Content expectedOutput) {

  public AuthoringTestcase authoring() {
    return new AuthoringTestcase(
        id,
        position,
        visibility,
        origin,
        approved,
        category,
        input.storage(),
        input.inline(),
        input.objectKey(),
        expectedOutput.storage(),
        expectedOutput.inline(),
        expectedOutput.objectKey());
  }
}

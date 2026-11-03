package com.algoprep.problembank.domain.exception;

import com.algoprep.common.exception.ServiceUnavailableException;

/** Do not retain SDK causes: they can contain signed URLs, object keys or response bodies. */
public final class TestcaseStorageUnavailableException extends ServiceUnavailableException {
  private static final long serialVersionUID = 1L;
  private final boolean objectMissing;

  public TestcaseStorageUnavailableException() {
    this(false);
  }

  public TestcaseStorageUnavailableException(boolean objectMissing) {
    super("PRB-052", "Testcase storage is unavailable", null);
    this.objectMissing = objectMissing;
  }

  public boolean isObjectMissing() {
    return objectMissing;
  }
}

package com.algoprep.harness.application.preview;

import com.algoprep.common.exception.AlgoPrepException;

/** HRN-062: the P6 body is over 16,384 UTF-8 bytes; answered with HTTP 413 before P5 runs. */
public final class PreviewBodyTooLargeException extends AlgoPrepException {
  private static final long serialVersionUID = 1L;

  public PreviewBodyTooLargeException() {
    super("HRN-062", "Signature preview body is too large");
  }
}

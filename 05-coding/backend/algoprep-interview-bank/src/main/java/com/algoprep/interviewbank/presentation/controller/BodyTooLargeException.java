package com.algoprep.interviewbank.presentation.controller;

import com.algoprep.common.exception.AlgoPrepException;

/** INB-V17: the body exceeds the technical cap of the endpoint (HTTP 413). */
final class BodyTooLargeException extends AlgoPrepException {
  private static final long serialVersionUID = 1L;

  BodyTooLargeException() {
    super("INB-V17", "Request body exceeds the size limit");
  }
}

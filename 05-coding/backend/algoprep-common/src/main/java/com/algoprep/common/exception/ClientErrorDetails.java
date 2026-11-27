package com.algoprep.common.exception;

import java.util.Map;

/** Explicitly approved response metadata. Implementations must not put it into log messages. */
public interface ClientErrorDetails {
  Map<String, Object> clientErrorDetails();
}

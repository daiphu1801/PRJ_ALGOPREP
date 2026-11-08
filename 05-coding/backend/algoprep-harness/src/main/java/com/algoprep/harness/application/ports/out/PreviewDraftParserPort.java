package com.algoprep.harness.application.ports.out;

import com.algoprep.common.functionspec.FunctionSpecDraft;

/**
 * Turns the raw P6 request body into a typed draft. The raw JSON shape check stays with F2, so the
 * preview and the save of a specification read the same wire format.
 */
public interface PreviewDraftParserPort {
  /** Throws {@code PreviewSpecInvalidException} (HRN-060) when the body is not a valid draft. */
  FunctionSpecDraft parse(byte[] utf8Body);
}

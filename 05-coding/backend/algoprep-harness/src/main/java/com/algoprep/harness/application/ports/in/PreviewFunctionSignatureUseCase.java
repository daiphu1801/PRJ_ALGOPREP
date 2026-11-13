package com.algoprep.harness.application.ports.in;

import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.harness.application.dto.FunctionSignaturePreview;

/** P6 (DD F3 API 3.7): previews a typed draft without reading or saving a problem. */
public interface PreviewFunctionSignatureUseCase {
  FunctionSignaturePreview preview(FunctionSpecDraft draft);
}

package com.algoprep.harness.application.dto;

import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.SpecEvaluation.UnsupportedReason;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Immutable P6 result; unsupported drafts have three explicitly null declarations. */
public record FunctionSignaturePreview(
    boolean wrapperSupported,
    List<UnsupportedReason> unsupportedReasons,
    Map<Language, String> declarations) {
  public FunctionSignaturePreview {
    unsupportedReasons = List.copyOf(unsupportedReasons);
    EnumMap<Language, String> copy = new EnumMap<>(Language.class);
    copy.putAll(declarations);
    declarations = Collections.unmodifiableMap(copy);
  }
}

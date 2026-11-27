package com.algoprep.harness.domain.signature;

import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Shared declaration renderer for P6 and future P1. Call only after successful P5 evaluation. */
public final class FunctionSignatureDeclarations {
  private static final Map<Language, DeclarationFormatter> FORMATTERS =
      Map.of(
          Language.JAVA, new JavaDeclarationFormatter(),
          Language.CPP, new CppDeclarationFormatter(),
          Language.PYTHON, new PythonDeclarationFormatter());

  private FunctionSignatureDeclarations() {}

  public static Map<Language, String> render(FunctionSpecDraft validatedDraft) {
    EnumMap<Language, String> result = new EnumMap<>(Language.class);
    for (Language language : Language.values()) {
      result.put(language, FORMATTERS.get(language).declaration(validatedDraft));
    }
    return Collections.unmodifiableMap(result);
  }
}

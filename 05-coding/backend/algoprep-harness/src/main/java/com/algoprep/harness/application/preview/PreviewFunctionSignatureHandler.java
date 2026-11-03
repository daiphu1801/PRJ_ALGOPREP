package com.algoprep.harness.application.preview;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.FunctionSpecEvaluator;
import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.dto.FunctionSignaturePreview;
import com.algoprep.harness.application.ports.in.PreviewFunctionSignatureUseCase;
import com.algoprep.harness.domain.signature.FunctionSignatureDeclarations;
import java.util.EnumMap;
import org.springframework.stereotype.Service;

/** Pure P6 orchestration; HTTP decoding, authorization and throttling are separate boundaries. */
@Service
final class PreviewFunctionSignatureHandler implements PreviewFunctionSignatureUseCase {
  @Override
  public FunctionSignaturePreview preview(FunctionSpecDraft draft) {
    if (draft == null) {
      throw new DomainRuleViolationException("HRN-031", "Missing function signature draft");
    }
    var evaluation = FunctionSpecEvaluator.evaluate(draft);
    if (!evaluation.violations().isEmpty()) {
      throw new PreviewSpecInvalidException(evaluation.violations());
    }
    EnumMap<Language, String> declarations = new EnumMap<>(Language.class);
    for (Language language : Language.values()) {
      declarations.put(language, null);
    }
    if (evaluation.wrapperSupported()) {
      declarations.putAll(FunctionSignatureDeclarations.render(draft));
    }
    return new FunctionSignaturePreview(
        evaluation.wrapperSupported(), evaluation.unsupportedReasons(), declarations);
  }
}

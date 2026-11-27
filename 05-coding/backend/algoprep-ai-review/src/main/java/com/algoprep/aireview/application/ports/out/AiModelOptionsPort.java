package com.algoprep.aireview.application.ports.out;

import java.util.List;
import java.util.OptionalInt;

/** The dev-declared {@code ai.models.allowed} list the admin may pick from per prompt. */
public interface AiModelOptionsPort {
  List<String> allowed();

  /** Provider output ceiling, if configured for this model. */
  default OptionalInt maxOutputTokens(String modelId) {
    return OptionalInt.empty();
  }
}

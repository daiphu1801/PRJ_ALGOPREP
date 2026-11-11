package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import java.util.Optional;

/** Reads the ACTIVE template of a feature. Callers read once per LLM call and reuse the result. */
public interface AiPromptSnapshotRepository {
  Optional<PromptSnapshot> readActive(FeatureCode featureCode);
}

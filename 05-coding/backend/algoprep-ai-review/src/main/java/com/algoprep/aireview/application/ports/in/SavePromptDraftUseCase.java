package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.SavePromptDraftCommand;
import com.algoprep.aireview.domain.model.PromptTemplate;

public interface SavePromptDraftUseCase {
  PromptTemplate save(SavePromptDraftCommand command);
}

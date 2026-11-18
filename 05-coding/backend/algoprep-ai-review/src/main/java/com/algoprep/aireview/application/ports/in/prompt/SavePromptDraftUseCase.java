package com.algoprep.aireview.application.ports.in.prompt;

import com.algoprep.aireview.application.dto.prompt.SavePromptDraftCommand;
import com.algoprep.aireview.domain.model.prompt.PromptTemplate;

public interface SavePromptDraftUseCase {
  PromptTemplate save(SavePromptDraftCommand command);
}

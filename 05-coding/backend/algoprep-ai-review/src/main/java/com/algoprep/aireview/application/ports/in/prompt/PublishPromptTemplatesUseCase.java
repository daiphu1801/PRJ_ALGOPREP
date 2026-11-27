package com.algoprep.aireview.application.ports.in.prompt;

import com.algoprep.aireview.application.dto.prompt.PromptTemplateList;
import com.algoprep.aireview.application.dto.prompt.PublishChange;
import java.util.List;
import java.util.UUID;

public interface PublishPromptTemplatesUseCase {
  PromptTemplateList publish(UUID actor, List<PublishChange> changes);
}

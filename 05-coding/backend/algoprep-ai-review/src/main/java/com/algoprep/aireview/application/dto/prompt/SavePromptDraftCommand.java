package com.algoprep.aireview.application.dto.prompt;

import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.prompt.PromptContent;
import java.util.UUID;

/** Endpoint 16 input; the content has passed the shape checks of the presentation layer. */
public record SavePromptDraftCommand(UUID actor, FeatureCode feature, PromptContent content) {}

package com.algoprep.aireview.application.dto;

import com.algoprep.aireview.domain.model.PromptTemplate;
import java.util.List;

/** Endpoint 14 and 17 result: the live rows, the allowed models and the read-only token cap. */
public record PromptTemplateList(
    List<PromptTemplate> items, List<String> modelOptions, int maxTokens) {}

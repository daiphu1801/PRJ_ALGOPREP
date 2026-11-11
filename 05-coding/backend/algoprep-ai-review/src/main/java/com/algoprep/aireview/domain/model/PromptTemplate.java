package com.algoprep.aireview.domain.model;

import java.time.Instant;
import java.util.UUID;

/** One row of {@code ai.prompt_templates}. */
public record PromptTemplate(
    UUID id,
    FeatureCode featureCode,
    String version,
    PromptStatus status,
    PromptContent content,
    String responseSchemaVersion,
    Instant publishedAt,
    Instant createdAt) {}

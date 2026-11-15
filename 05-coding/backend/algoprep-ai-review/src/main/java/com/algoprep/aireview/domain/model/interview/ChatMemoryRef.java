package com.algoprep.aireview.domain.model.interview;

import java.util.UUID;

/** Points at the Redis ChatMemory of one interview session (BR-AI-10). */
public record ChatMemoryRef(UUID sessionId) {}

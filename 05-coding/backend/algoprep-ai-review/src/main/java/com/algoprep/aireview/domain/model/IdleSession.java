package com.algoprep.aireview.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A running session that nobody touched for the idle window (JOB-AI-04 candidate). */
public record IdleSession(UUID id, int turnCount, Instant updatedAt) {}

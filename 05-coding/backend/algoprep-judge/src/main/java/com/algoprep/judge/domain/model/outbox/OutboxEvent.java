package com.algoprep.judge.domain.model;

import java.util.UUID;

/** The committed wire envelope is replayed unchanged, including its event identity. */
public record OutboxEvent(UUID id, String type, String envelope, int attempts) {}

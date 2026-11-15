package com.algoprep.problembank.domain.model;

import java.util.UUID;

/** The committed envelope is replayed unchanged, so the event id survives every retry. */
public record OutboxEvent(
    UUID id, String eventType, String routingKey, String envelope, int attempts) {}

package com.algoprep.interviewbank.domain.model;

import java.util.UUID;

/** Receipt of one reserved hourly slot; the AI side refunds it exactly once. */
public record GateTicket(UUID userId, String requestId, long epochHour, boolean charged) {}

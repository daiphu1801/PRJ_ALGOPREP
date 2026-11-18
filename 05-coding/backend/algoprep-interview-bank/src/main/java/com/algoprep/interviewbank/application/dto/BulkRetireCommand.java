package com.algoprep.interviewbank.application.dto;

import java.util.List;
import java.util.UUID;

public record BulkRetireCommand(UUID actorId, List<UUID> ids) {}

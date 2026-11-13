package com.algoprep.interviewbank.application.dto;

import java.util.List;
import java.util.UUID;

public record ImportCommand(UUID actorId, List<ImportRow> rows) {}

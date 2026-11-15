package com.algoprep.identity.application.dto;

import java.util.UUID;

/** Name and base category of a new custom role, as sent. */
public record CreateRoleCommand(UUID actorId, String name, String baseCategory) {}

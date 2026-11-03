package com.algoprep.identity.application.dto;

import java.util.UUID;

/** Manual account creation of #50, as sent. */
public record CreateUserAccountCommand(
    UUID actorId, String displayName, String email, String roleId) {}

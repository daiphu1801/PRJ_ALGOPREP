package com.algoprep.identity.application.dto;

import java.util.UUID;

/** Request #11: the caller asks to move their login to {@code newEmail}. */
public record RequestEmailChangeCommand(UUID userId, String newEmail) {}

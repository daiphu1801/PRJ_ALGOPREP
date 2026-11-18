package com.algoprep.identity.application.dto;

import com.algoprep.identity.domain.model.BaseCategory;
import java.util.UUID;

/**
 * The user block of a login response.
 *
 * @param id user id
 * @param displayName name shown in the UI
 * @param baseCategory layer-1 category
 * @param mustChangePassword soft flag: the UI sends the user to the change-password screen
 */
public record UserSummary(
    UUID id, String displayName, BaseCategory baseCategory, boolean mustChangePassword) {}

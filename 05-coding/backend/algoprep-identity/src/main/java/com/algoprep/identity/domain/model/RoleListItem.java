package com.algoprep.identity.domain.model;

import java.util.UUID;

/** A role with the number of accounts holding it (#32, #33). */
public record RoleListItem(
    UUID id, String code, String name, String baseCategory, boolean isSystem, int userCount) {}

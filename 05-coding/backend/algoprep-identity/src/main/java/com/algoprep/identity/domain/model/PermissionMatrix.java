package com.algoprep.identity.domain.model;

import java.util.List;

/** The whole role by function by action matrix (#30), every triple present. */
public record PermissionMatrix(
    List<RbacRole> roles,
    List<CatalogEntry> functions,
    List<CatalogEntry> actions,
    List<PermissionCell> cells) {}

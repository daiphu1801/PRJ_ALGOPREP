package com.algoprep.identity.domain.model;

/**
 * What an {@code action_type} shows as: display group and the module that produced it.
 *
 * @param category one of the four display groups
 * @param service lower-case module name, for example {@code auth} or {@code ai-config}
 */
public record AuditActionInfo(AuditCategory category, String service) {}

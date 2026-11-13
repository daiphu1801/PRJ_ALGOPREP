package com.algoprep.problembank.presentation.controller;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.problembank.application.dto.CatalogCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.JsonNode;

final class CatalogRequests {
  private CatalogRequests() {}

  static boolean admin(JwtAuthenticationToken caller) {
    return caller.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }

  static CatalogCommand command(
      JwtAuthenticationToken caller, String id, JsonNode body, String field, boolean ordering) {
    // Check role before parsing administrator-only input. The method-security gate checks the cell.
    boolean isAdmin = admin(caller);
    if (!isAdmin) {
      throw new PermissionDeniedException("PRB-004", "Admin role required");
    }
    String name = body != null && body.path(field).isString() ? body.path(field).asString() : null;
    return new CatalogCommand(
        UUID.fromString(caller.getToken().getSubject()),
        isAdmin,
        id == null ? null : identifier(id),
        name,
        ordering ? order(body) : null);
  }

  private static UUID identifier(String text) {
    try {
      return UUID.fromString(text);
    } catch (IllegalArgumentException failure) {
      throw new InvalidRequestException("PRB-001", "Invalid catalogue id");
    }
  }

  private static List<UUID> order(JsonNode body) {
    if (body == null || !body.path("orderedIds").isArray()) {
      throw invalidOrder();
    }
    var result = new ArrayList<UUID>();
    for (JsonNode node : body.path("orderedIds")) {
      try {
        if (!node.isString()) {
          throw invalidOrder();
        }
        result.add(UUID.fromString(node.asString()));
      } catch (IllegalArgumentException failure) {
        throw invalidOrder();
      }
    }
    return result;
  }

  private static DomainRuleViolationException invalidOrder() {
    return new DomainRuleViolationException("PRB-049", "Invalid catalogue order");
  }
}

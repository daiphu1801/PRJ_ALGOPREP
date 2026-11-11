package com.algoprep.interviewbank.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.JsonNode;

/** Turns the trusted caller and the raw JSON body into a command; shape errors are INB-V*. */
final class CatalogRequests {
  private CatalogRequests() {}

  static boolean admin(JwtAuthenticationToken caller) {
    return caller.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }

  static UUID actor(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }

  /** The role is checked before any administrator-only input is parsed (INB-302). */
  private static void requireAdmin(JwtAuthenticationToken caller) {
    if (!admin(caller)) {
      throw new PermissionDeniedException("INB-302", "Admin role required");
    }
  }

  static CatalogCommand create(JwtAuthenticationToken caller, JsonNode body) {
    requireAdmin(caller);
    return new CatalogCommand(actor(caller), true, null, name(body), null, null);
  }

  static CatalogCommand update(
      JwtAuthenticationToken caller, CatalogKind kind, String id, JsonNode body) {
    requireAdmin(caller);
    Boolean star = null;
    if (kind == CatalogKind.TOPIC && !body.path("usesStarFramework").isMissingNode()) {
      JsonNode flag = body.path("usesStarFramework");
      if (!flag.isBoolean()) {
        throw new InvalidRequestException("INB-V13", "usesStarFramework must be a boolean");
      }
      star = flag.asBoolean();
    }
    String name = name(body);
    if (kind == CatalogKind.TOPIC && name == null && star == null) {
      throw new InvalidRequestException("INB-V13", "Request has no updatable field");
    }
    return new CatalogCommand(actor(caller), true, identifier(kind, id), name, star, null);
  }

  static CatalogCommand reorder(JwtAuthenticationToken caller, JsonNode body) {
    requireAdmin(caller);
    if (!body.path("orderedIds").isArray()) {
      throw invalidOrder();
    }
    var ids = new ArrayList<UUID>();
    for (JsonNode node : body.path("orderedIds")) {
      if (!node.isString()) {
        throw invalidOrder();
      }
      try {
        ids.add(UUID.fromString(node.asString()));
      } catch (IllegalArgumentException failure) {
        throw invalidOrder();
      }
    }
    return new CatalogCommand(actor(caller), true, null, null, null, List.copyOf(ids));
  }

  static CatalogCommand delete(JwtAuthenticationToken caller, CatalogKind kind, String id) {
    requireAdmin(caller);
    return new CatalogCommand(actor(caller), true, identifier(kind, id), null, null, null);
  }

  /** A present but non-text displayName is as invalid as an empty one (INB-V03). */
  private static String name(JsonNode body) {
    JsonNode node = body.path("displayName");
    if (node.isMissingNode()) {
      return null;
    }
    if (!node.isString()) {
      throw new InvalidRequestException("INB-V03", "displayName must be text");
    }
    return node.asString();
  }

  /** An id that is not a UUID cannot exist, so it reads as a missing item (404). */
  private static UUID identifier(CatalogKind kind, String text) {
    try {
      return UUID.fromString(text);
    } catch (IllegalArgumentException failure) {
      throw new ResourceNotFoundException(
          kind == CatalogKind.LEVEL ? "INB-012" : "INB-011", "Catalogue item not found");
    }
  }

  private static InvalidRequestException invalidOrder() {
    return new InvalidRequestException("INB-V14", "orderedIds must be an array of UUIDs");
  }
}

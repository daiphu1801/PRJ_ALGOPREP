package com.algoprep.identity.presentation.controller;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.dto.avatar.AvatarContent;
import com.algoprep.identity.application.ports.in.avatar.GetUserAvatarUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avatar bytes of any user for any signed-in caller, #76 (DD api F1 section 0.9.3). A malformed id
 * answers the same 404 IDT-451 as "no avatar". The query parameter {@code v} only separates cache
 * entries and is not read. The bytes are fetched here, after the use case's transaction ended.
 */
@RestController
@RequestMapping("/api/v1/identity/users")
class UserAvatarController {
  private static final String CACHE = "private, max-age=86400";

  private final GetUserAvatarUseCase avatars;

  UserAvatarController(GetUserAvatarUseCase avatars) {
    this.avatars = avatars;
  }

  @GetMapping("/{userId}/avatar")
  ResponseEntity<byte[]> get(
      @PathVariable String userId,
      @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
    AvatarContent content = avatars.get(id(userId), ifNoneMatch);
    if (content.notModified()) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
          .header(HttpHeaders.ETAG, content.etag())
          .header(HttpHeaders.CACHE_CONTROL, CACHE)
          .build();
    }
    byte[] body = content.body().get();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.contentType()))
        .header("X-Content-Type-Options", "nosniff")
        .header(HttpHeaders.ETAG, content.etag())
        .header(HttpHeaders.CACHE_CONTROL, CACHE)
        .body(body);
  }

  private static UUID id(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ResourceNotFoundException(IdentityErrorCodes.AVATAR_NOT_FOUND, "No avatar to show");
    }
  }
}

package com.algoprep.identity.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.ports.in.avatar.RemoveMyAvatarUseCase;
import com.algoprep.identity.application.ports.in.avatar.UploadMyAvatarUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

/**
 * Own avatar, #74 and #75 (DD api F1 section 0.9). The caller comes from the token; no id is
 * accepted. Any role may call. The body is read from the raw request so a wrong shape answers
 * {@code 400 IDT-V32} instead of Spring's own binding error.
 */
@RestController
@RequestMapping("/api/v1/identity/me/avatar")
class MyAvatarController {
  private final UploadMyAvatarUseCase upload;
  private final RemoveMyAvatarUseCase remove;

  MyAvatarController(UploadMyAvatarUseCase upload, RemoveMyAvatarUseCase remove) {
    this.upload = upload;
    this.remove = remove;
  }

  record AvatarVersionResponse(long avatarVersion) {}

  @PutMapping
  AvatarVersionResponse put(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
    byte[] bytes = onlyFilePart(request);
    return new AvatarVersionResponse(upload.upload(UUID.fromString(jwt.getSubject()), bytes));
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@AuthenticationPrincipal Jwt jwt) {
    remove.remove(UUID.fromString(jwt.getSubject()));
  }

  /** Exactly one file part named {@code file}; anything else is IDT-V32. */
  private static byte[] onlyFilePart(HttpServletRequest request) {
    // Security filters wrap the request, so unwrap to the multipart one Spring created.
    MultipartHttpServletRequest multipart =
        WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
    if (multipart == null) {
      throw invalid();
    }
    var files = multipart.getMultiFileMap();
    List<MultipartFile> parts = files.get("file");
    if (files.size() != 1 || parts == null || parts.size() != 1) {
      throw invalid();
    }
    try {
      return parts.get(0).getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static InvalidRequestException invalid() {
    return new InvalidRequestException(
        IdentityErrorCodes.AVATAR_REQUEST_INVALID,
        "Expected multipart/form-data with exactly one part named file");
  }
}

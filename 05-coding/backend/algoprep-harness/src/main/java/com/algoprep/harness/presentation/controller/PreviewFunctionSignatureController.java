package com.algoprep.harness.presentation.controller;

import com.algoprep.harness.application.dto.FunctionSignaturePreview;
import com.algoprep.harness.application.ports.in.PreviewFunctionSignatureUseCase;
import com.algoprep.harness.application.ports.out.PreviewDraftParserPort;
import com.algoprep.harness.application.ports.out.PreviewRateLimitPort;
import com.algoprep.harness.application.preview.PreviewBodyTooLargeException;
import com.algoprep.harness.application.preview.PreviewRateLimitedException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** P6 HTTP shell (DD F3 3.7): throttle, byte cap, parse, preview. Nothing is read or saved. */
@RestController
class PreviewFunctionSignatureController {
  static final int MAX_BODY_BYTES = 16_384;
  private final PreviewFunctionSignatureUseCase preview;
  private final PreviewDraftParserPort parser;
  private final PreviewRateLimitPort limiter;

  PreviewFunctionSignatureController(
      @Lazy PreviewFunctionSignatureUseCase preview,
      @Lazy PreviewDraftParserPort parser,
      PreviewRateLimitPort limiter) {
    this.preview = preview;
    this.parser = parser;
    this.limiter = limiter;
  }

  @PostMapping(
      value = "/api/v1/harness/function-signature/preview",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
  FunctionSignaturePreview preview(JwtAuthenticationToken caller, HttpServletRequest request)
      throws IOException {
    long wait = limiter.acquire(UUID.fromString(caller.getToken().getSubject()));
    if (wait > 0) {
      throw new PreviewRateLimitedException(wait);
    }
    // Read one byte past the cap: enough to refuse, and a huge body is never held in memory.
    byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
    if (body.length > MAX_BODY_BYTES) {
      throw new PreviewBodyTooLargeException();
    }
    return preview.preview(parser.parse(body));
  }
}

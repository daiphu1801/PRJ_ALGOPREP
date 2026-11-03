package com.algoprep.identity.presentation.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * #66: live audit rows as Server-Sent Events. Authenticated like every other endpoint, by the
 * {@code Authorization: Bearer} header (the repo convention for streams, see DD api section 7a):
 * the browser must read it with {@code fetch}, because the native {@code EventSource} cannot send
 * that header, and must send {@code Last-Event-ID} itself when it reconnects. No token travels in
 * the URL.
 */
@RestController
@RequestMapping("/api/v1/identity/admin/audit-logs")
class AuditLogStreamController {
  private final AuditLogStreamHub hub;

  AuditLogStreamController(AuditLogStreamHub hub) {
    this.hub = hub;
  }

  @GetMapping("/stream")
  SseEmitter stream(
      Authentication caller,
      @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
      HttpServletResponse response) {
    AdminOnly.require(caller);
    // no-transform: a proxy in front (the Next.js rewrite gzips for browsers) must not compress
    // the stream, or it holds the events in its buffer and the page never sees them.
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-transform");
    return hub.subscribe(lastEventId);
  }
}

package com.algoprep.bootstrap.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lets a stale token read the public problem pages as a guest (DD F2 section 0A, rule 3).
 *
 * <p>The resource-server filter answers 401 for any bad bearer token, even on a route that is open
 * to guests, so a browser that kept an expired token would see an error on a page that needs no
 * sign-in. On the guest-readable routes only, this filter validates the token with the same decoder
 * and converter. A good token becomes the authentication of the request (validated once, here); a
 * bad one, for any reason (expired, bad signature, revoked session, deactivated account, authority
 * data unavailable), is dropped and the request goes on as a guest. Every other route skips this
 * filter and keeps its 401.
 */
final class GuestReadBearerFilter extends OncePerRequestFilter {

  private final RequestMatcher guestReadable;
  private final JwtDecoder decoder;
  private final Converter<Jwt, AbstractAuthenticationToken> converter;
  private final BearerTokenResolver resolver = new DefaultBearerTokenResolver();

  GuestReadBearerFilter(
      RequestMatcher guestReadable,
      JwtDecoder decoder,
      Converter<Jwt, AbstractAuthenticationToken> converter) {
    this.guestReadable = guestReadable;
    this.decoder = decoder;
    this.converter = converter;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getHeader(HttpHeaders.AUTHORIZATION) == null || !guestReadable.matches(request);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    try {
      String token = resolver.resolve(request);
      if (token != null) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(converter.convert(decoder.decode(token)));
        SecurityContextHolder.setContext(context);
      }
    } catch (RuntimeException invalidOrUnavailable) {
      // A guest on purpose: see the class comment.
    }
    chain.doFilter(new WithoutAuthorization(request), response);
  }

  /** Hides the header so the resource-server filter does not validate the same token again. */
  private static final class WithoutAuthorization extends HttpServletRequestWrapper {
    WithoutAuthorization(HttpServletRequest request) {
      super(request);
    }

    @Override
    public String getHeader(String name) {
      return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name) ? null : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
          ? Collections.emptyEnumeration()
          : super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      return Collections.enumeration(
          Collections.list(super.getHeaderNames()).stream()
              .filter(h -> !HttpHeaders.AUTHORIZATION.equalsIgnoreCase(h))
              .toList());
    }
  }
}

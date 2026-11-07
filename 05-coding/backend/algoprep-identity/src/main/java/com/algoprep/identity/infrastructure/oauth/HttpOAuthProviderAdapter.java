package com.algoprep.identity.infrastructure.oauth;

import com.algoprep.identity.application.ports.out.OAuthProviderPort;
import com.algoprep.identity.domain.exception.OAuthCodeRejectedException;
import com.algoprep.identity.domain.model.OAuthProfile;
import com.algoprep.identity.domain.model.OAuthProvider;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * GitHub and Google over plain HTTPS. Keys live under {@code algoprep.oauth.<provider>.*}: {@code
 * client-id} and {@code client-secret} are the only ones a deployment sets; a provider with either
 * blank is "not configured" and the redirect answers 503. The endpoint URLs are properties only so
 * a test can point them at a local stub.
 */
@Component
class HttpOAuthProviderAdapter implements OAuthProviderPort {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final Environment environment;

  HttpOAuthProviderAdapter(Environment environment) {
    this.environment = environment;
  }

  @Override
  public Optional<String> authorizationUrl(OAuthProvider provider, String state) {
    if (!configured(provider)) {
      return Optional.empty();
    }
    String scope =
        provider == OAuthProvider.GITHUB ? "read:user user:email" : "openid email profile";
    var query = new LinkedHashMap<String, String>();
    query.put("client_id", prop(provider, "client-id", ""));
    query.put("redirect_uri", redirectUri(provider));
    query.put("response_type", "code");
    query.put("scope", scope);
    query.put("state", state);
    String base = prop(provider, "authorize-url", defaultAuthorize(provider));
    return Optional.of(base + (base.contains("?") ? "&" : "?") + form(query));
  }

  @Override
  public OAuthProfile fetchProfile(OAuthProvider provider, String code) {
    if (!configured(provider)) {
      throw new IllegalStateException("OAuth provider is not configured");
    }
    String token = exchange(provider, code);
    return provider == OAuthProvider.GITHUB ? github(token) : google(token);
  }

  private String exchange(OAuthProvider provider, String code) {
    var fields = new LinkedHashMap<String, String>();
    fields.put("grant_type", "authorization_code");
    fields.put("code", code);
    fields.put("client_id", prop(provider, "client-id", ""));
    fields.put("client_secret", prop(provider, "client-secret", ""));
    fields.put("redirect_uri", redirectUri(provider));
    JsonNode body =
        send(
            HttpRequest.newBuilder(URI.create(prop(provider, "token-url", defaultToken(provider))))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(fields)))
                .build(),
            true);
    JsonNode token = body.path("access_token");
    if (!token.isString() || token.asString().isBlank()) {
      throw new OAuthCodeRejectedException();
    }
    return token.asString();
  }

  private OAuthProfile github(String token) {
    String api = prop(OAuthProvider.GITHUB, "api-url", "https://api.github.com");
    JsonNode user = get(api + "/user", token);
    JsonNode emails = get(api + "/user/emails", token);
    String email = null;
    boolean verified = false;
    for (JsonNode candidate : emails) {
      boolean candidateVerified = candidate.path("verified").asBoolean(false);
      boolean primary = candidate.path("primary").asBoolean(false);
      if (candidateVerified && (primary || !verified)) {
        email = candidate.path("email").asString();
        verified = true;
      } else if (email == null && primary) {
        email = candidate.path("email").asString();
      }
    }
    String name =
        user.path("name").isString() ? user.path("name").asString() : user.path("login").asString();
    return new OAuthProfile(
        OAuthProvider.GITHUB,
        String.valueOf(user.path("id").asLong()),
        email == null ? null : email.toLowerCase(Locale.ROOT),
        verified,
        name);
  }

  private OAuthProfile google(String token) {
    String url =
        prop(
            OAuthProvider.GOOGLE,
            "userinfo-url",
            "https://openidconnect.googleapis.com/v1/userinfo");
    JsonNode user = get(url, token);
    String email = user.path("email").isString() ? user.path("email").asString() : null;
    return new OAuthProfile(
        OAuthProvider.GOOGLE,
        user.path("sub").asString(),
        email == null ? null : email.toLowerCase(Locale.ROOT),
        user.path("email_verified").asBoolean(false),
        user.path("name").isString() ? user.path("name").asString() : null);
  }

  private JsonNode get(String url, String token) {
    return send(
        HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + token)
            .header("User-Agent", "AlgoPrep")
            .GET()
            .build(),
        false);
  }

  /**
   * A token endpoint that answers 400 or 401 rejected the code; any other failure is "unavailable".
   */
  private JsonNode send(HttpRequest request, boolean tokenEndpoint) {
    try {
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (tokenEndpoint && (status == 400 || status == 401)) {
        throw new OAuthCodeRejectedException();
      }
      if (status / 100 != 2) {
        throw new IllegalStateException("OAuth provider answered " + status);
      }
      return JSON.readTree(response.body());
    } catch (IOException failure) {
      throw new IllegalStateException("OAuth provider unreachable");
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("OAuth call interrupted");
    }
  }

  private boolean configured(OAuthProvider provider) {
    return !prop(provider, "client-id", "").isBlank()
        && !prop(provider, "client-secret", "").isBlank();
  }

  private String prop(OAuthProvider provider, String key, String fallback) {
    return environment.getProperty("algoprep.oauth." + provider.path() + "." + key, fallback);
  }

  /** The callback is a route of the frontend, which then calls #6. */
  private String redirectUri(OAuthProvider provider) {
    String base =
        environment.getProperty("algoprep.oauth.redirect-base-url", "http://localhost:3000");
    return base.replaceAll("/+$", "") + "/auth/callback/" + provider.path();
  }

  private static String defaultAuthorize(OAuthProvider provider) {
    return provider == OAuthProvider.GITHUB
        ? "https://github.com/login/oauth/authorize"
        : "https://accounts.google.com/o/oauth2/v2/auth";
  }

  private static String defaultToken(OAuthProvider provider) {
    return provider == OAuthProvider.GITHUB
        ? "https://github.com/login/oauth/access_token"
        : "https://oauth2.googleapis.com/token";
  }

  private static String form(Map<String, String> values) {
    return values.entrySet().stream()
        .map(
            e ->
                URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }
}

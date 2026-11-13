package com.algoprep.identity.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.exception.OAuthCodeRejectedException;
import com.algoprep.identity.domain.model.OAuthProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** The two provider dialogues against a local stub, with the real HTTP client. */
class HttpOAuthProviderAdapterTest {
  private HttpServer server;
  private String base;
  private final Map<String, Reply> routes = new ConcurrentHashMap<>();
  private final List<String> seen = new ArrayList<>();
  private final MockEnvironment env = new MockEnvironment();

  private record Reply(int status, String body) {}

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String key = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath();
          String body =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          seen.add(
              key + " | " + exchange.getRequestHeaders().getFirst("Authorization") + " | " + body);
          reply(exchange, routes.getOrDefault(key, new Reply(404, "{}")));
        });
    server.start();
    base = "http://127.0.0.1:" + server.getAddress().getPort();
    env.setProperty("algoprep.oauth.github.client-id", "gh-id");
    env.setProperty("algoprep.oauth.github.client-secret", "gh-secret");
    env.setProperty("algoprep.oauth.github.token-url", base + "/gh/token");
    env.setProperty("algoprep.oauth.github.api-url", base + "/gh/api");
    env.setProperty("algoprep.oauth.google.client-id", "g-id");
    env.setProperty("algoprep.oauth.google.client-secret", "g-secret");
    env.setProperty("algoprep.oauth.google.token-url", base + "/g/token");
    env.setProperty("algoprep.oauth.google.userinfo-url", base + "/g/userinfo");
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private static void reply(HttpExchange exchange, Reply reply) throws IOException {
    byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(reply.status(), bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private HttpOAuthProviderAdapter adapter() {
    return new HttpOAuthProviderAdapter(env);
  }

  @Test
  void theAuthorizationUrlCarriesClientStateScopeAndTheFrontendCallback() {
    var url = adapter().authorizationUrl(OAuthProvider.GITHUB, "st4te").orElseThrow();

    assertThat(url).startsWith("https://github.com/login/oauth/authorize?");
    assertThat(url)
        .contains("client_id=gh-id", "state=st4te", "response_type=code")
        .contains("redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Fauth%2Fcallback%2Fgithub");
    assertThat(adapter().authorizationUrl(OAuthProvider.GOOGLE, "x").orElseThrow())
        .startsWith("https://accounts.google.com/o/oauth2/v2/auth?")
        .contains("scope=openid+email+profile");
  }

  @Test
  void aProviderWithoutClientIdOrSecretIsNotConfigured() {
    env.setProperty("algoprep.oauth.google.client-secret", " ");

    assertThat(adapter().authorizationUrl(OAuthProvider.GOOGLE, "s")).isEmpty();
    assertThatThrownBy(() -> adapter().fetchProfile(OAuthProvider.GOOGLE, "c"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void githubUsesThePrimaryVerifiedEmailAndSendsTheCodeAndSecret() {
    routes.put("POST /gh/token", new Reply(200, "{\"access_token\":\"tok\"}"));
    routes.put(
        "GET /gh/api/user", new Reply(200, "{\"id\":42,\"name\":\"An Nguyen\",\"login\":\"an\"}"));
    routes.put(
        "GET /gh/api/user/emails",
        new Reply(
            200,
            "[{\"email\":\"Old@X.test\",\"primary\":false,\"verified\":true},"
                + "{\"email\":\"An@X.test\",\"primary\":true,\"verified\":true}]"));

    var profile = adapter().fetchProfile(OAuthProvider.GITHUB, "the-code");

    assertThat(profile.providerUserId()).isEqualTo("42");
    assertThat(profile.email()).isEqualTo("an@x.test");
    assertThat(profile.emailVerified()).isTrue();
    assertThat(profile.displayName()).isEqualTo("An Nguyen");
    assertThat(seen.get(0))
        .contains("code=the-code", "client_secret=gh-secret", "grant_type=authorization_code");
    assertThat(seen.get(1)).contains("Bearer tok");
  }

  @Test
  void githubWithOnlyAnUnverifiedEmailIsNotVerifiedAndFallsBackToTheLoginName() {
    routes.put("POST /gh/token", new Reply(200, "{\"access_token\":\"tok\"}"));
    routes.put("GET /gh/api/user", new Reply(200, "{\"id\":7,\"name\":null,\"login\":\"binh\"}"));
    routes.put(
        "GET /gh/api/user/emails",
        new Reply(200, "[{\"email\":\"b@x.test\",\"primary\":true,\"verified\":false}]"));

    var profile = adapter().fetchProfile(OAuthProvider.GITHUB, "c");

    assertThat(profile.emailVerified()).isFalse();
    assertThat(profile.displayName()).isEqualTo("binh");
  }

  @Test
  void googleReadsSubEmailAndTheVerifiedFlag() {
    routes.put("POST /g/token", new Reply(200, "{\"access_token\":\"gtok\"}"));
    routes.put(
        "GET /g/userinfo",
        new Reply(
            200,
            "{\"sub\":\"1001\",\"email\":\"Chi@X.test\",\"email_verified\":true,\"name\":\"Chi\"}"));

    var profile = adapter().fetchProfile(OAuthProvider.GOOGLE, "c");

    assertThat(profile.providerUserId()).isEqualTo("1001");
    assertThat(profile.email()).isEqualTo("chi@x.test");
    assertThat(profile.emailVerified()).isTrue();

    routes.put(
        "GET /g/userinfo",
        new Reply(200, "{\"sub\":\"1\",\"email\":\"c@x.test\",\"email_verified\":false}"));
    assertThat(adapter().fetchProfile(OAuthProvider.GOOGLE, "c").emailVerified()).isFalse();
  }

  @Test
  void aRefusedCodeIsRejectedInBothDialects() {
    routes.put("POST /gh/token", new Reply(200, "{\"error\":\"bad_verification_code\"}"));
    assertThatThrownBy(() -> adapter().fetchProfile(OAuthProvider.GITHUB, "c"))
        .isInstanceOf(OAuthCodeRejectedException.class);

    routes.put("POST /g/token", new Reply(400, "{\"error\":\"invalid_grant\"}"));
    assertThatThrownBy(() -> adapter().fetchProfile(OAuthProvider.GOOGLE, "c"))
        .isInstanceOf(OAuthCodeRejectedException.class);
  }

  @Test
  void aProviderErrorIsAnOrdinaryFailureNotARejectedCode() {
    routes.put("POST /g/token", new Reply(200, "{\"access_token\":\"gtok\"}"));
    routes.put("GET /g/userinfo", new Reply(500, "{}"));
    assertThatThrownBy(() -> adapter().fetchProfile(OAuthProvider.GOOGLE, "c"))
        .isInstanceOf(IllegalStateException.class)
        .isNotInstanceOf(OAuthCodeRejectedException.class);

    routes.put("POST /g/token", new Reply(503, "{}"));
    assertThatThrownBy(() -> adapter().fetchProfile(OAuthProvider.GOOGLE, "c"))
        .isInstanceOf(IllegalStateException.class);
  }
}

package com.algoprep.bootstrap.config;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Security configuration: JWT resource server plus method security.
 *
 * <p>Access tokens are RS256 JWTs verified by the {@code JwtDecoder} that module identity exposes.
 * The {@link Converter} bean from identity re-reads account status and the permission matrix on
 * every request, and exposes {@code ROLE_<BASE_CATEGORY>} and {@code FUNCTION:ACTION} authorities.
 * Controllers in any module gate with {@code @PreAuthorize("hasAuthority('FUNCTION:ACTION')")}.
 *
 * <p>Decisions worth knowing:
 *
 * <ul>
 *   <li><strong>CSRF is ignored for {@code /api/**} only.</strong> Bearer-token APIs are not
 *       exposed to CSRF; every other path keeps Spring's default protection. The one cookie
 *       endpoint, {@code POST /auth/refresh}, must itself require the {@code X-Requested-With}
 *       header (BD security section 1); that check belongs to its controller.
 *   <li><strong>No CORS configuration.</strong> The Next.js frontend is a different origin, but the
 *       allowed origins per environment are a decision, not a guessable value.
 *   <li><strong>{@code /actuator/prometheus} is public</strong> on purpose, see the comment below.
 * </ul>
 *
 * <p>The security headers, including the {@code Content-Security-Policy} that {@code
 * 01-rd/req/nfr.md} section D requires, are applied here too.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final String actuatorBasePath;
  private final Converter<Jwt, AbstractAuthenticationToken> jwtConverter;

  public SecurityConfig(
      WebEndpointProperties webEndpointProperties,
      Converter<Jwt, AbstractAuthenticationToken> jwtConverter) {
    this.actuatorBasePath = webEndpointProperties.getBasePath();
    this.jwtConverter = jwtConverter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(actuatorBasePath + "/health", actuatorBasePath + "/health/**")
                    .permitAll()
                    // /actuator/prometheus mo cong khai, va day la mot danh doi co y thuc, khong
                    // phai bo sot. Prometheus trong 05-coding/docker-compose.yml scrape endpoint
                    // nay va KHONG co co che xac thuc nao duoc cau hinh, nen neu de yeu cau dang
                    // nhap thi giam sat chet trong im lang — 401 khong hien len o dau ngoai log cua
                    // Prometheus. Mot bao dong khong bao gio keu con te hon khong co bao dong.
                    //
                    // NO KY THUAT: endpoint nay lo ten metric va so lieu JVM (khong lo du lieu
                    // nguoi dung). Truoc khi trien khai that phai chuyen actuator sang mot cong
                    // rieng (`management.server.port`) khong publish ra ngoai. Xem
                    // 05-coding/backend/README.md muc no ky thuat.
                    .requestMatchers(actuatorBasePath + "/prometheus")
                    .permitAll()
                    // Public auth endpoints of module identity (DD api F1 section 0.1). Each one
                    // enforces its own rules (rate limit, OTP, cookie plus header).
                    .requestMatchers("/api/v1/identity/auth/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        // Stateless: không có session phía máy chủ. JWT là cơ chế đã chốt (F1-02), và một session
        // song song với JWT là hai nguồn sự thật về "ai đang gọi".
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Chưa xác thực thì trả 401, không chuyển hướng sang trang đăng nhập: đây là API, client là
        // Next.js, một redirect HTML ở đây chỉ làm client khó đọc lỗi.
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler(new ApiAccessDeniedHandler()))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .oauth2ResourceServer(
            oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)))
        .headers(this::hardenHeaders);
    return http.build();
  }

  /**
   * Security header cho phản hồi do BACKEND phát ra.
   *
   * <p>Phân định rõ để khỏi nhầm: CSP bảo vệ giao diện người dùng là việc của Next.js, vì trang
   * HTML do frontend phát. CSP ở đây chỉ áp cho nội dung backend tự trả — trang lỗi, actuator, và
   * bất kỳ nội dung tải về nào sau này. Với một API thuần thì chính sách đúng là chặn hết: {@code
   * default-src 'none'}.
   */
  private void hardenHeaders(
      org.springframework.security.config.annotation.web.configurers.HeadersConfigurer<HttpSecurity>
          headers) {
    headers
        .contentSecurityPolicy(
            csp ->
                csp.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
        .referrerPolicy(
            r ->
                r.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                        .ReferrerPolicy.NO_REFERRER))
        .frameOptions(frame -> frame.deny());
  }
}

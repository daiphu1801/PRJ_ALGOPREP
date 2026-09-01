package com.algoprep.bootstrap.config;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Cấu hình bảo mật TỐI THIỂU của khung base.
 *
 * <p><strong>Đây chưa phải cấu hình thật.</strong> JWT (access token ngắn hạn + refresh token trong
 * cookie HTTP-Only) và RBAC ba vai trò {@code STUDENT}/{@code INSTRUCTOR}/{@code ADMIN} ({@code
 * F1-02}, {@code F1-05}) cần {@code 03-dd/api/identity.md}, tài liệu chưa tồn tại. Ở base, quy tắc
 * là: mở đúng nhóm endpoint quan sát, chặn mọi thứ còn lại.
 *
 * <p>Phần <strong>đã là thật</strong> và không phải chờ DD: bộ security header, trong đó có {@code
 * Content-Security-Policy} mà {@code 01-rd/req/nfr.md} mục D yêu cầu.
 *
 * <p>Hai điều cố tình để nguyên mặc định của Spring Security, vì kiểu hỏng của chúng là kiểu hỏng
 * ỒN ÀO chứ không âm thầm:
 *
 * <ul>
 *   <li><strong>CSRF vẫn BẬT.</strong> Cùng với {@code STATELESS} bên dưới, endpoint {@code POST}
 *       đầu tiên ai đó thêm vào sẽ trả {@code 403} ngay — buộc người đó phải quyết định CSRF một
 *       cách có ý thức, đúng như {@code nfr.md} mục D đòi cho luồng refresh token bằng cookie. Tắt
 *       sẵn ở đây sẽ tạo ra một lỗ bảo mật không ai nhìn thấy trong diff.
 *   <li><strong>Không cấu hình CORS.</strong> Frontend Next.js chạy khác origin nên sẽ cần CORS,
 *       nhưng danh sách origin hợp lệ theo từng môi trường là một quyết định, không phải một giá
 *       trị đoán được.
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final String actuatorBasePath;

  public SecurityConfig(WebEndpointProperties webEndpointProperties) {
    this.actuatorBasePath = webEndpointProperties.getBasePath();
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
                    .anyRequest()
                    .authenticated())
        // Stateless: không có session phía máy chủ. JWT là cơ chế đã chốt (F1-02), và một session
        // song song với JWT là hai nguồn sự thật về "ai đang gọi".
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Chưa xác thực thì trả 401, không chuyển hướng sang trang đăng nhập: đây là API, client là
        // Next.js, một redirect HTML ở đây chỉ làm client khó đọc lỗi.
        .exceptionHandling(
            e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
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

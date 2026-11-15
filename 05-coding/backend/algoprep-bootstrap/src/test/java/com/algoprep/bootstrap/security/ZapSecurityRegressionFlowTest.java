package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** HTTP error contracts and transaction rollback with real PostgreSQL and Redis-backed adapters. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=3"
    })
@ActiveProfiles("test")
@Testcontainers
@SuppressWarnings({"rawtypes", "unchecked"})
class ZapSecurityRegressionFlowTest {
  private static final String PASSWORD = "ValidPass123";

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;
  @MockitoSpyBean StringRedisTemplate redis;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void nulAtAnyPositionReturns400AndValidUnicodeSearchStillWorks() throws Exception {
    UUID admin = account("ADMIN");
    String token = tokens.issue(admin, BaseCategory.ADMIN, "ADMIN", UUID.randomUUID()).value();
    for (String keyword : List.of("a\0b", "\0abc", "abc\0", "\0", " \0 ")) {
      var response =
          mvc.perform(
                  get("/api/v1/identity/admin/users")
                      .param("q", keyword)
                      .header("Authorization", "Bearer " + token))
              .andReturn()
              .getResponse();
      assertThat(response.getStatus()).isEqualTo(400);
      assertThat(response.getContentAsString())
          .contains("IDT-V17")
          .doesNotContain("SQLException", "0x00");
    }
    assertThat(
            mvc.perform(
                    get("/api/v1/identity/admin/users")
                        .param("q", "Nguyễn Văn Tú")
                        .header("Authorization", "Bearer " + token))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(200);
  }

  @Test
  void failedCounterWriteReturns503WithoutIssuingLoginSession() throws Exception {
    UUID id = account("STUDENT");
    doThrow(new RedisConnectionFailureException("private Redis address"))
        .when(redis)
        .execute(any(RedisScript.class), anyList());
    unavailable(login(id, "wrong"));
    assertThat(refreshCount(id)).isZero();
  }

  @Test
  void failedCounterClearRollsBackTheNewLoginSession() throws Exception {
    UUID id = account("STUDENT");
    doThrow(new RedisConnectionFailureException("private Redis address"))
        .when(redis)
        .delete("identity:ratelimit:login:" + id + "@example.test:127.0.0.1");
    unavailable(login(id, PASSWORD));
    assertThat(refreshCount(id)).isZero();
  }

  @Test
  void passwordChangeCannotProceedWithoutCompletingTheSharedLimiter() throws Exception {
    UUID id = account("STUDENT");
    String before =
        jdbc.queryForObject(
            "SELECT password_hash FROM identity.users WHERE id=?", String.class, id);
    String token = tokens.issue(id, BaseCategory.STUDENT, "STUDENT", UUID.randomUUID()).value();
    doThrow(new RedisConnectionFailureException("private Redis address"))
        .when(redis)
        .delete("identity:ratelimit:login:pwchange:" + id + ":-");
    var response =
        mvc.perform(
                post("/api/v1/identity/me/password")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"currentPassword\":\"ValidPass123\",\"newPassword\":\"AnotherPass456\"}"))
            .andReturn()
            .getResponse();
    unavailable(response);
    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash FROM identity.users WHERE id=?", String.class, id))
        .isEqualTo(before);
  }

  private UUID account(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,password_hash,display_name,role_id,status) SELECT ?,?,?,?,id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        new BCryptPasswordEncoder().encode(PASSWORD),
        "QA",
        role);
    return id;
  }

  private int refreshCount(UUID id) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.refresh_tokens WHERE user_id=?", Integer.class, id);
  }

  private MockHttpServletResponse login(UUID id, String password) throws Exception {
    return mvc.perform(
            post("/api/v1/identity/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"" + id + "@example.test\",\"password\":\"" + password + "\"}"))
        .andReturn()
        .getResponse();
  }

  private static void unavailable(MockHttpServletResponse response) throws Exception {
    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(response.getContentAsString())
        .contains("IDT-510")
        .doesNotContain("accessToken", "private Redis address");
    assertThat(response.getHeader("Set-Cookie")).isNull();
  }
}

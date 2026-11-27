package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.jayway.jsonpath.JsonPath;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * F1-35 avatar endpoints #74 to #77 and the {@code avatarVersion} of #9 against real PostgreSQL,
 * Redis and MinIO (AC-93 to AC-102). Images are real files made with ImageIO or hand-built headers;
 * MockMvc is used for the contract, one real HTTP call proves the container's multipart limit.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=4"
    })
@ActiveProfiles("test")
@Testcontainers
class UserAvatarFlowTest {
  private static final String ME = "/api/v1/identity/me/avatar";
  private static final String PASSWORD = "ValidPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
  private static final String MINIO_USER = "avatar-test-user";
  private static final String MINIO_PASSWORD = "avatar-test-password";
  private static final String BUCKET = "algoprep-avatars";

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

  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>("minio/minio:RELEASE.2025-09-07T16-13-09Z")
          .withEnv("MINIO_ROOT_USER", MINIO_USER)
          .withEnv("MINIO_ROOT_PASSWORD", MINIO_PASSWORD)
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("algoprep.identity.avatar.storage.endpoint", UserAvatarFlowTest::minioUrl);
    properties.add("algoprep.identity.avatar.storage.access-key", () -> MINIO_USER);
    properties.add("algoprep.identity.avatar.storage.secret-key", () -> MINIO_PASSWORD);
  }

  private static String minioUrl() {
    return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
  }

  private static MinioClient store;

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AccessTokenPort tokens;
  @Autowired private StringRedisTemplate redis;
  @LocalServerPort private int port;
  private MockMvc mvc;

  /** A signed-in account: STUDENT, INSTRUCTOR or ADMIN. */
  private record Account(UUID id, String email, String token) {}

  @BeforeAll
  static void createBucket() throws Exception {
    store =
        MinioClient.builder().endpoint(minioUrl()).credentials(MINIO_USER, MINIO_PASSWORD).build();
    store.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
  }

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // ---- fixtures ---------------------------------------------------------------------------

  private Account account(String role) {
    UUID id = UUID.randomUUID();
    String email = id + "@example.test";
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status)"
            + " SELECT ?,?,?,'Avatar Tester',r.id,'ACTIVE' FROM identity.roles r WHERE r.code=?",
        id,
        email,
        HASH,
        role);
    BaseCategory category = BaseCategory.valueOf(role);
    return new Account(id, email, tokens.issue(id, category, role, UUID.randomUUID()).value());
  }

  private static byte[] image(String format, int side) throws Exception {
    var out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB), format, out);
    return out.toByteArray();
  }

  /** Header-only PNG that claims the given size; the server never decodes pixels. */
  private static byte[] pngHeader(int width, int height) {
    byte[] d = new byte[40];
    byte[] magic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(magic, 0, d, 0, 8);
    System.arraycopy("IHDR".getBytes(StandardCharsets.US_ASCII), 0, d, 12, 4);
    for (int i = 0; i < 4; i++) {
      d[16 + i] = (byte) (width >>> (24 - 8 * i));
      d[20 + i] = (byte) (height >>> (24 - 8 * i));
    }
    return d;
  }

  /** Header-only lossless WebP of the given size. */
  private static byte[] webp(int width, int height) {
    byte[] d = new byte[40];
    System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, d, 0, 4);
    System.arraycopy("WEBPVP8L".getBytes(StandardCharsets.US_ASCII), 0, d, 8, 8);
    d[20] = 0x2F;
    int bits = (width - 1) | ((height - 1) << 14);
    for (int i = 0; i < 4; i++) {
      d[21 + i] = (byte) (bits >>> (8 * i));
    }
    return d;
  }

  private MockHttpServletResponse put(Account who, byte[] bytes, String contentType)
      throws Exception {
    return put(who, new MockMultipartFile("file", "avatar.png", contentType, bytes));
  }

  private MockHttpServletResponse put(Account who, MockMultipartFile... files) throws Exception {
    MockMultipartHttpServletRequestBuilder request = multipart(HttpMethod.PUT, ME);
    Arrays.stream(files).forEach(request::file);
    return mvc.perform(request.header("Authorization", "Bearer " + who.token()))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse deleteOwn(Account who) throws Exception {
    return mvc.perform(delete(ME).header("Authorization", "Bearer " + who.token()))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse read(Account viewer, String userId, String ifNoneMatch)
      throws Exception {
    var request =
        get("/api/v1/identity/users/" + userId + "/avatar")
            .header("Authorization", "Bearer " + viewer.token());
    if (ifNoneMatch != null) {
      request.header("If-None-Match", ifNoneMatch);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private MockHttpServletResponse adminRemove(Account admin, String userId) throws Exception {
    return mvc.perform(
            delete("/api/v1/identity/admin/users/" + userId + "/avatar")
                .header("Authorization", "Bearer " + admin.token()))
        .andReturn()
        .getResponse();
  }

  private Long profileVersion(Account who) throws Exception {
    MockHttpServletResponse profile =
        mvc.perform(
                get("/api/v1/identity/me/profile").header("Authorization", "Bearer " + who.token()))
            .andReturn()
            .getResponse();
    assertThat(profile.getStatus()).isEqualTo(200);
    Number version = JsonPath.read(profile.getContentAsString(), "$.avatarVersion");
    return version == null ? null : version.longValue();
  }

  private int rows(UUID userId) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.user_avatars WHERE user_id = ?", Integer.class, userId);
  }

  private List<String> objects(UUID userId) throws Exception {
    List<String> keys = new ArrayList<>();
    var found =
        store.listObjects(
            ListObjectsArgs.builder().bucket(BUCKET).prefix("users/" + userId + "/").build());
    for (Result<Item> item : found) {
      keys.add(item.get().objectName());
    }
    return keys;
  }

  private int auditCount(UUID target) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.system_audit_logs WHERE action_type = 'USER_AVATAR_REMOVED'"
            + " AND target_id = ?",
        Integer.class,
        target.toString());
  }

  private static void assertError(MockHttpServletResponse r, int status, String code)
      throws Exception {
    assertThat(r.getStatus()).isEqualTo(status);
    assertThat(JsonPath.<String>read(r.getContentAsString(), "$.errorCode")).isEqualTo(code);
  }

  // ---- AC-93, AC-94: upload, read, replace ------------------------------------------------

  @Test
  void validPngJpegAndWebpAreStoredReadByOthersAndShownInTheProfile() throws Exception {
    Account owner = account("STUDENT");
    Account other = account("INSTRUCTOR");
    assertThat(profileVersion(owner)).isNull();
    byte[][] files = {image("png", 16), image("jpg", 16), webp(32, 32)};
    String[] types = {"image/png", "image/jpeg", "image/webp"};

    for (int i = 0; i < files.length; i++) {
      MockHttpServletResponse stored = put(owner, files[i], types[i]);
      assertThat(stored.getStatus()).isEqualTo(200);
      long version =
          JsonPath.<Number>read(stored.getContentAsString(), "$.avatarVersion").longValue();
      assertThat(profileVersion(owner)).isEqualTo(version);
      MockHttpServletResponse seen = read(other, owner.id().toString(), null);
      assertThat(seen.getStatus()).isEqualTo(200);
      assertThat(seen.getContentAsByteArray()).isEqualTo(files[i]);
      assertThat(seen.getContentType()).isEqualTo(types[i]);
      assertThat(seen.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
      assertThat(seen.getHeader("Cache-Control")).isEqualTo("private, max-age=86400");
    }
    assertThat(rows(owner.id())).isEqualTo(1);
  }

  @Test
  void anEtagMatchIs304AndOwnerCanReadOwnAvatar() throws Exception {
    Account owner = account("STUDENT");
    assertThat(put(owner, image("png", 8), "image/png").getStatus()).isEqualTo(200);

    MockHttpServletResponse first = read(owner, owner.id().toString(), null);
    String etag = first.getHeader("ETag");
    assertThat(etag).startsWith("\"").endsWith("\"");
    MockHttpServletResponse cached = read(owner, owner.id().toString(), etag);
    assertThat(cached.getStatus()).isEqualTo(304);
    assertThat(cached.getContentAsByteArray()).isEmpty();
    assertThat(cached.getHeader("ETag")).isEqualTo(etag);
    assertThat(read(owner, owner.id().toString(), "W/" + etag).getStatus()).isEqualTo(304);
    assertThat(read(owner, owner.id().toString(), "\"other\"").getStatus()).isEqualTo(200);
  }

  @Test
  void replacingKeepsOneObjectAndRaisesTheVersionStrictly() throws Exception {
    Account owner = account("STUDENT");
    long previous = -1;
    String firstEtag = null;
    for (int i = 0; i < 3; i++) {
      MockHttpServletResponse stored = put(owner, image("png", 8 + i), "image/png");
      long version =
          JsonPath.<Number>read(stored.getContentAsString(), "$.avatarVersion").longValue();
      assertThat(version).isGreaterThan(previous);
      previous = version;
      String etag = read(owner, owner.id().toString(), null).getHeader("ETag");
      assertThat(etag).isNotEqualTo(firstEtag);
      firstEtag = etag;
      assertThat(objects(owner.id())).hasSize(1);
    }
    assertThat(rows(owner.id())).isEqualTo(1);
    assertThat(profileVersion(owner)).isEqualTo(previous);
  }

  // ---- AC-95, AC-96: validation -----------------------------------------------------------

  @Test
  void notAnImageIsRejectedByItsBytesWhateverTheNameOrContentTypeSays() throws Exception {
    Account owner = account("STUDENT");
    byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8);
    byte[] text = "this is plain text".getBytes(StandardCharsets.UTF_8);

    assertError(put(owner, svg, "image/svg+xml"), 422, "IDT-V29");
    assertError(put(owner, svg, "image/png"), 422, "IDT-V29");
    assertError(put(owner, text, "image/png"), 422, "IDT-V29");
    assertError(put(owner, new byte[0], "image/png"), 422, "IDT-V29");

    assertThat(rows(owner.id())).isZero();
    assertThat(objects(owner.id())).isEmpty();
  }

  @Test
  void aRealPngWithAWrongContentTypeIsAccepted() throws Exception {
    Account owner = account("STUDENT");

    assertThat(put(owner, image("png", 8), "text/plain").getStatus()).isEqualTo(200);

    assertThat(read(owner, owner.id().toString(), null).getContentType()).isEqualTo("image/png");
  }

  @Test
  void sizeAndPixelLimitsAreEnforcedAndOldAvatarSurvivesEveryRefusal() throws Exception {
    Account owner = account("STUDENT");
    assertThat(put(owner, image("png", 8), "image/png").getStatus()).isEqualTo(200);
    long version = profileVersion(owner);
    byte[] tooBig = Arrays.copyOf(pngHeader(10, 10), 600 * 1024);

    assertError(put(owner, tooBig, "image/png"), 413, "IDT-V30");
    assertError(put(owner, pngHeader(4000, 4000), "image/png"), 422, "IDT-V31");
    assertError(put(owner, pngHeader(1025, 10), "image/png"), 422, "IDT-V31");
    assertError(put(owner, pngHeader(0, 10), "image/png"), 422, "IDT-V31");

    assertThat(profileVersion(owner)).isEqualTo(version);
    assertThat(objects(owner.id())).hasSize(1);
    assertThat(put(owner, pngHeader(1024, 1024), "image/png").getStatus()).isEqualTo(200);
  }

  @Test
  void aMissingOrExtraPartIs400IdtV32() throws Exception {
    Account owner = account("STUDENT");
    var file = new MockMultipartFile("file", "a.png", "image/png", image("png", 8));
    var second = new MockMultipartFile("file", "b.png", "image/png", image("png", 8));
    var wrongName = new MockMultipartFile("avatar", "a.png", "image/png", image("png", 8));

    assertError(put(owner), 400, "IDT-V32");
    assertError(put(owner, file, second), 400, "IDT-V32");
    assertError(put(owner, wrongName), 400, "IDT-V32");
    MockHttpServletResponse notMultipart =
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(ME)
                    .header("Authorization", "Bearer " + owner.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andReturn()
            .getResponse();
    assertError(notMultipart, 400, "IDT-V32");

    assertThat(rows(owner.id())).isZero();
  }

  @Test
  void aBodyOverTheContainerLimitIs413IdtV30OnARealServer() throws Exception {
    Account owner = account("STUDENT");
    String boundary = "----avatarBoundary";
    byte[] head =
        ("--"
                + boundary
                + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n")
            .getBytes(StandardCharsets.US_ASCII);
    byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII);
    var body = new ByteArrayOutputStream();
    body.write(head);
    body.write(new byte[650 * 1024]);
    body.write(tail);

    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + ME))
                    .header("Authorization", "Bearer " + owner.token())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build(),
                HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(413);
    assertThat(JsonPath.<String>read(response.body(), "$.errorCode")).isEqualTo("IDT-V30");
    assertThat(rows(owner.id())).isZero();
  }

  // ---- AC-97, AC-98: own removal and read privacy -----------------------------------------

  @Test
  void ownDeletionIsIdempotentLeavesNothingAndWritesNoAudit() throws Exception {
    Account owner = account("STUDENT");
    assertThat(put(owner, image("png", 8), "image/png").getStatus()).isEqualTo(200);

    assertThat(deleteOwn(owner).getStatus()).isEqualTo(204);
    assertThat(deleteOwn(owner).getStatus()).isEqualTo(204);

    assertThat(rows(owner.id())).isZero();
    assertThat(objects(owner.id())).isEmpty();
    assertThat(profileVersion(owner)).isNull();
    assertError(read(owner, owner.id().toString(), null), 404, "IDT-451");
    assertThat(auditCount(owner.id())).isZero();
  }

  @Test
  void everyReasonForNothingToShowIsTheSame404AndNoTokenIs401() throws Exception {
    Account owner = account("STUDENT");
    Account viewer = account("STUDENT");
    Account gone = account("STUDENT");
    Account anonymised = account("STUDENT");
    assertThat(put(gone, image("png", 8), "image/png").getStatus()).isEqualTo(200);
    assertThat(put(anonymised, image("png", 8), "image/png").getStatus()).isEqualTo(200);
    jdbc.update(
        "UPDATE identity.users SET status='DEACTIVATED', deactivation_source='SELF',"
            + " deactivated_at=clock_timestamp() WHERE id=?",
        gone.id());
    jdbc.update(
        "UPDATE identity.users SET status='DEACTIVATED', deactivation_source='SELF',"
            + " deactivated_at=clock_timestamp() - interval '40 days',"
            + " anonymized_at=clock_timestamp() WHERE id=?",
        anonymised.id());

    List<MockHttpServletResponse> answers =
        List.of(
            read(viewer, owner.id().toString(), null),
            read(viewer, UUID.randomUUID().toString(), null),
            read(viewer, gone.id().toString(), null),
            read(viewer, anonymised.id().toString(), null),
            read(viewer, "not-a-uuid", null));

    for (MockHttpServletResponse answer : answers) {
      assertError(answer, 404, "IDT-451");
      assertThat(JsonPath.<String>read(answer.getContentAsString(), "$.title"))
          .isEqualTo(JsonPath.<String>read(answers.get(0).getContentAsString(), "$.title"));
      assertThat(JsonPath.<String>read(answer.getContentAsString(), "$.detail"))
          .isEqualTo(JsonPath.<String>read(answers.get(0).getContentAsString(), "$.detail"));
    }
    assertThat(
            mvc.perform(get("/api/v1/identity/users/" + owner.id() + "/avatar"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
  }

  @Test
  void uploadAndDeleteNeedAToken() throws Exception {
    var file = new MockMultipartFile("file", "a.png", "image/png", image("png", 8));

    assertThat(
            mvc.perform(multipart(HttpMethod.PUT, ME).file(file))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
    assertThat(mvc.perform(delete(ME)).andReturn().getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void aCallerOnlyEverChangesTheirOwnAvatar() throws Exception {
    Account a = account("STUDENT");
    Account b = account("STUDENT");
    assertThat(put(a, image("png", 8), "image/png").getStatus()).isEqualTo(200);

    assertThat(put(b, image("png", 9), "image/png").getStatus()).isEqualTo(200);
    assertThat(deleteOwn(b).getStatus()).isEqualTo(204);

    assertThat(rows(a.id())).isEqualTo(1);
    assertThat(rows(b.id())).isZero();
  }

  // ---- AC-99: administrator removal -------------------------------------------------------

  @Test
  void adminRemovalIsAuditedOnceAndTheSecondCallWritesNothing() throws Exception {
    Account admin = account("ADMIN");
    Account target = account("STUDENT");
    assertThat(put(target, image("png", 8), "image/png").getStatus()).isEqualTo(200);

    assertThat(adminRemove(admin, target.id().toString()).getStatus()).isEqualTo(204);
    assertThat(adminRemove(admin, target.id().toString()).getStatus()).isEqualTo(204);

    assertThat(rows(target.id())).isZero();
    assertThat(objects(target.id())).isEmpty();
    assertThat(auditCount(target.id())).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT before_json ->> 'hadAvatar' FROM identity.system_audit_logs WHERE"
                    + " action_type = 'USER_AVATAR_REMOVED' AND target_id = ? AND actor_user_id = ?",
                String.class,
                target.id().toString(),
                admin.id()))
        .isEqualTo("true");
  }

  @Test
  void adminRemovalOfAUserWithoutAvatarIs204WithoutAuditAndUnknownUserIs404() throws Exception {
    Account admin = account("ADMIN");
    Account target = account("STUDENT");

    assertThat(adminRemove(admin, target.id().toString()).getStatus()).isEqualTo(204);
    assertThat(auditCount(target.id())).isZero();
    assertError(adminRemove(admin, UUID.randomUUID().toString()), 404, "IDT-450");
    assertError(adminRemove(admin, "not-a-uuid"), 404, "IDT-450");
  }

  @Test
  void onlyAnAdminWithTheUpdateCellMayRemoveSomeoneElsesAvatar() throws Exception {
    Account target = account("STUDENT");
    assertThat(put(target, image("png", 8), "image/png").getStatus()).isEqualTo(200);

    assertError(adminRemove(account("STUDENT"), target.id().toString()), 403, "IDT-445");
    assertError(adminRemove(account("INSTRUCTOR"), target.id().toString()), 403, "IDT-445");
    assertThat(
            mvc.perform(delete("/api/v1/identity/admin/users/" + target.id() + "/avatar"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(401);
    assertThat(rows(target.id())).isEqualTo(1);

    Account admin = account("ADMIN");
    setUpdateCell(false);
    try {
      assertError(adminRemove(admin, target.id().toString()), 403, "IDT-445");
      assertThat(rows(target.id())).isEqualTo(1);
    } finally {
      setUpdateCell(true);
    }
    assertThat(adminRemove(admin, target.id().toString()).getStatus()).isEqualTo(204);
  }

  private void setUpdateCell(boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted = ? FROM identity.roles r, identity.functions f,"
            + " identity.actions a WHERE r.id = p.role_id AND f.id = p.function_id AND a.id ="
            + " p.action_id AND r.code = 'ADMIN' AND f.code = 'USER_MANAGEMENT' AND a.code ="
            + " 'UPDATE'",
        granted);
    jdbc.update(
        "UPDATE identity.roles SET permission_version = permission_version + 1 WHERE code ="
            + " 'ADMIN'");
  }

  // ---- AC-100: rate limit -----------------------------------------------------------------

  @Test
  void theEleventhChangeInAnHourIs429AndAdminRemovalIsNotCounted() throws Exception {
    Account owner = account("STUDENT");
    Account admin = account("ADMIN");
    assertThat(put(owner, image("png", 8), "image/png").getStatus()).isEqualTo(200);
    for (int i = 0; i < 9; i++) {
      assertThat(deleteOwn(owner).getStatus()).isEqualTo(204);
    }

    assertError(put(owner, image("png", 8), "image/png"), 429, "IDT-452");
    assertError(deleteOwn(owner), 429, "IDT-452");
    assertThat(rows(owner.id())).isZero();

    String ownerKey = "identity:avatar:rate:" + owner.id();
    String counted = redis.opsForValue().get(ownerKey);
    for (int i = 0; i < 12; i++) {
      assertThat(adminRemove(admin, owner.id().toString()).getStatus()).isEqualTo(204);
    }
    assertThat(redis.opsForValue().get(ownerKey)).isEqualTo(counted);
    assertThat(redis.hasKey("identity:avatar:rate:" + admin.id())).isFalse();
  }

  // ---- AC-101: storage outage -------------------------------------------------------------

  @Test
  void whenStorageIsDownUploadAndReadAre503WhileLoginAndProfileStayUp() throws Exception {
    Account owner = account("STUDENT");
    assertThat(put(owner, image("png", 8), "image/png").getStatus()).isEqualTo(200);
    long version = profileVersion(owner);
    String key =
        jdbc.queryForObject(
            "SELECT object_key FROM identity.user_avatars WHERE user_id = ?",
            String.class,
            owner.id());

    MINIO.getDockerClient().pauseContainerCmd(MINIO.getContainerId()).exec();
    try {
      assertError(put(owner, image("png", 9), "image/png"), 503, "IDT-511");
      assertError(read(account("STUDENT"), owner.id().toString(), null), 503, "IDT-511");
      assertThat(
              mvc.perform(
                      post("/api/v1/identity/auth/login")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(
                              "{\"email\":\""
                                  + owner.email()
                                  + "\",\"password\":\""
                                  + PASSWORD
                                  + "\"}"))
                  .andReturn()
                  .getResponse()
                  .getStatus())
          .isEqualTo(200);
      assertThat(profileVersion(owner)).isEqualTo(version);
    } finally {
      MINIO.getDockerClient().unpauseContainerCmd(MINIO.getContainerId()).exec();
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT object_key FROM identity.user_avatars WHERE user_id = ?",
                String.class,
                owner.id()))
        .isEqualTo(key);
    assertThat(read(owner, owner.id().toString(), null).getStatus()).isEqualTo(200);
  }

  /**
   * [SoT: Suy luận] AC-101 expects 503 for #75 and #77 too, but BR-26 step 3 and BD storage section
   * 4 delete the object after commit and only log a failure. The code follows BR-26, so the row is
   * gone and the object is orphaned; this test pins that behaviour until the owner decides.
   */
  @Test
  void whenStorageIsDownRemovalsStillSucceedByBr26AndLeaveAnOrphanObject() throws Exception {
    Account owner = account("STUDENT");
    Account admin = account("ADMIN");
    Account other = account("STUDENT");
    assertThat(put(owner, image("png", 8), "image/png").getStatus()).isEqualTo(200);
    assertThat(put(other, image("png", 8), "image/png").getStatus()).isEqualTo(200);

    MINIO.getDockerClient().pauseContainerCmd(MINIO.getContainerId()).exec();
    try {
      assertThat(deleteOwn(owner).getStatus()).isEqualTo(204);
      assertThat(adminRemove(admin, other.id().toString()).getStatus()).isEqualTo(204);
    } finally {
      MINIO.getDockerClient().unpauseContainerCmd(MINIO.getContainerId()).exec();
    }

    assertThat(rows(owner.id())).isZero();
    assertThat(rows(other.id())).isZero();
    assertThat(objects(owner.id())).hasSize(1);
    assertThat(auditCount(other.id())).isEqualTo(1);
  }

  // ---- AC-102: account deletion -----------------------------------------------------------

  @Test
  void selfDeletionRemovesTheAvatarAtOnceAndRestoreDoesNotBringItBack() throws Exception {
    Account owner = account("STUDENT");
    Account viewer = account("STUDENT");
    assertThat(put(owner, image("png", 8), "image/png").getStatus()).isEqualTo(200);
    assertThat(objects(owner.id())).hasSize(1);

    MockHttpServletResponse deleted =
        mvc.perform(
                post("/api/v1/identity/me/account/delete")
                    .header("Authorization", "Bearer " + owner.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"emailConfirmation\":\"" + owner.email() + "\"}"))
            .andReturn()
            .getResponse();
    assertThat(deleted.getStatus()).isEqualTo(200);

    assertThat(rows(owner.id())).isZero();
    assertThat(objects(owner.id())).isEmpty();
    assertError(read(viewer, owner.id().toString(), null), 404, "IDT-451");

    String ticket = restoreTicket(owner);
    MockHttpServletResponse restored =
        mvc.perform(
                post("/api/v1/identity/auth/deactivation/restore")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"restoreTicket\":\"" + ticket + "\"}"))
            .andReturn()
            .getResponse();
    assertThat(restored.getStatus()).isEqualTo(200);
    MockHttpServletResponse login = login(owner);
    assertThat(login.getStatus()).isEqualTo(200);
    Account back =
        new Account(
            owner.id(), owner.email(), JsonPath.read(login.getContentAsString(), "$.accessToken"));
    assertThat(profileVersion(back)).isNull();
    assertError(read(viewer, owner.id().toString(), null), 404, "IDT-451");
  }

  private MockHttpServletResponse login(Account who) throws Exception {
    return mvc.perform(
            post("/api/v1/identity/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + who.email() + "\",\"password\":\"" + PASSWORD + "\"}"))
        .andReturn()
        .getResponse();
  }

  private String restoreTicket(Account who) throws Exception {
    MockHttpServletResponse denied = login(who);
    assertError(denied, 403, "IDT-104");
    return JsonPath.read(denied.getContentAsString(), "$.details.restoreTicket");
  }
}

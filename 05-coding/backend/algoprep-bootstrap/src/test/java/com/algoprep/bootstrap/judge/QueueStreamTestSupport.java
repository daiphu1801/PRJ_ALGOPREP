package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.application.ports.in.BroadcastQueueMonitorUseCase;
import com.algoprep.judge.application.ports.in.GetQueueMonitorBatchUseCase;
import com.algoprep.judge.application.ports.out.QueueMonitorAudiencePort;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

abstract class QueueStreamTestSupport {
  @LocalServerPort protected int port;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected AccessTokenPort tokens;
  @Autowired protected BroadcastQueueMonitorUseCase ticks;
  @Autowired protected GetQueueMonitorBatchUseCase batches;
  @Autowired protected QueueMonitorAudiencePort audience;
  @Autowired protected PlatformTransactionManager transactions;
  protected final JsonMapper json = JsonMapper.builder().build();
  protected UUID operator;
  protected String admin;

  @BeforeEach
  void setupQueueStream() throws Exception {
    await(() -> !audience.hasSubscribers());
    jdbc.update("DELETE FROM judge.outbox_events");
    jdbc.update("DELETE FROM judge.submissions");
    jdbc.update(
        "UPDATE judge.cluster_settings SET queue_paused=false,autoscale_enabled=true,autoscale_job_threshold=20,jobs_per_page=20,error_retention_hours=24,latency_cap_seconds=10");
    setRead(true);
    operator = UUID.randomUUID();
    admin = account(operator, "ADMIN");
  }

  protected String account(UUID id, String role) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Queue stream user',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return tokens.issue(id, BaseCategory.valueOf(role), role, UUID.randomUUID()).value();
  }

  protected void setRead(boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted=? FROM identity.roles r,identity.functions f,identity.actions a WHERE p.role_id=r.id AND p.function_id=f.id AND p.action_id=a.id AND r.code='ADMIN' AND f.code='JUDGE_QUEUE_MONITOR' AND a.code='READ'",
        granted);
    jdbc.update(
        "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code='ADMIN'");
  }

  protected void seed() {
    for (String status :
        new String[] {"PENDING", "PENDING", "COMPILING", "JUDGING", "SYSTEM_ERROR", "ACCEPTED"}) {
      jdbc.update(
          "INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,source_input_method,status,failure_reason,testcase_set_version_at_grading,submitted_at,started_grading_at,finished_at) VALUES(?,?,?,'JAVA','STANDARD_IO','STREAM_PRIVATE_SOURCE','EDITOR',?,?,1,transaction_timestamp()-interval '6 seconds',CASE WHEN ?='PENDING' THEN NULL ELSE transaction_timestamp()-interval '4 seconds' END,CASE WHEN ? IN ('SYSTEM_ERROR','ACCEPTED') THEN transaction_timestamp()-interval '1 second' ELSE NULL END)",
          UUID.randomUUID(),
          operator,
          UUID.randomUUID(),
          status,
          status.equals("SYSTEM_ERROR") ? "SANDBOX_FAULT" : null,
          status,
          status);
    }
    for (int attempts : new int[] {3, 5}) {
      jdbc.update(
          "INSERT INTO judge.outbox_events(id,aggregate_type,aggregate_id,event_type,payload,publish_attempts) VALUES(?,'Submission',?,'SubmissionGraded','{\"secret\":\"STREAM_PRIVATE_PAYLOAD\"}'::jsonb,?)",
          UUID.randomUUID(),
          UUID.randomUUID(),
          attempts);
    }
  }

  protected JsonNode body(String frame) {
    assertThat(frame).startsWith("MESSAGE\n").contains("destination:/topic/admin/queue");
    return json.readTree(frame.substring(frame.indexOf("\n\n") + 2));
  }

  protected static void await(BooleanSupplier condition) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
    while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
    assertThat(condition.getAsBoolean()).isTrue();
  }

  protected static Instant generated(JsonNode batch) {
    return Instant.parse(batch.path("generatedAt").asText());
  }
}

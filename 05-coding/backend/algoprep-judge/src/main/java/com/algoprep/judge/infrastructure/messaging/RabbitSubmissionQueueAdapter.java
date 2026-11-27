package com.algoprep.judge.infrastructure.messaging;

import com.algoprep.judge.application.ports.out.SubmissionQueuePort;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Dedicated per-attempt channels isolate late confirms from retries and other module publishers.
 */
@Component
class RabbitSubmissionQueueAdapter implements SubmissionQueuePort {
  static final String QUEUE = "judge.submissions";
  private final ConnectionFactory factory;

  @Autowired
  RabbitSubmissionQueueAdapter(Environment environment) {
    this(configured(environment));
  }

  RabbitSubmissionQueueAdapter(ConnectionFactory factory) {
    this.factory = factory;
  }

  @Override
  public boolean enqueue(UUID submissionId) {
    Objects.requireNonNull(submissionId);
    for (int attempt = 0; attempt < 3; attempt++) {
      if (Thread.currentThread().isInterrupted()) {
        return false;
      }
      if (attempt(submissionId)) {
        return true;
      }
      if (attempt < 2 && !pause(attempt == 0 ? 200 : 500)) {
        return false;
      }
    }
    return false;
  }

  private boolean attempt(UUID id) {
    var task = new FutureTask<>(() -> publishOnce(id));
    Thread.startVirtualThread(task);
    try {
      return task.get(3, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return false;
    } catch (java.util.concurrent.ExecutionException
        | java.util.concurrent.TimeoutException failure) {
      return false;
    } finally {
      task.cancel(true);
    }
  }

  private boolean publishOnce(UUID id) throws Exception {
    var connection = factory.newConnection();
    try {
      var channel = connection.createChannel();
      channel.queueDeclare(QUEUE, true, false, false, null);
      channel.confirmSelect();
      var returned = new AtomicBoolean();
      channel.addReturnListener(message -> returned.set(true));
      if (Thread.currentThread().isInterrupted()) {
        return false;
      }
      var properties =
          new AMQP.BasicProperties.Builder()
              .contentType("application/json")
              .contentEncoding("UTF-8")
              .deliveryMode(2)
              .messageId(id.toString())
              .build();
      byte[] body = ("{\"submissionId\":\"" + id + "\"}").getBytes(StandardCharsets.UTF_8);
      channel.basicPublish("", QUEUE, true, properties, body);
      return channel.waitForConfirms(3000) && !returned.get();
    } finally {
      connection.abort(100);
    }
  }

  private static boolean pause(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
      return true;
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static ConnectionFactory configured(Environment environment) {
    var factory = new ConnectionFactory();
    factory.setHost(environment.getProperty("spring.rabbitmq.host", "localhost"));
    factory.setPort(environment.getProperty("spring.rabbitmq.port", Integer.class, 5672));
    factory.setUsername(environment.getProperty("spring.rabbitmq.username", "guest"));
    factory.setPassword(environment.getProperty("spring.rabbitmq.password", "guest"));
    factory.setVirtualHost(environment.getProperty("spring.rabbitmq.virtual-host", "/"));
    factory.setConnectionTimeout(3000);
    factory.setHandshakeTimeout(3000);
    factory.setChannelRpcTimeout(3000);
    factory.setAutomaticRecoveryEnabled(false);
    return factory;
  }
}

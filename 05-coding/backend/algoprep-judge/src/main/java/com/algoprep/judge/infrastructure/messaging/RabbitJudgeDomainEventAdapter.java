package com.algoprep.judge.infrastructure.messaging;

import com.algoprep.judge.application.ports.out.JudgeDomainEventPort;
import com.algoprep.judge.domain.model.OutboxEvent;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Dedicated channels isolate late confirms and returns from other publishers. */
@Component
class RabbitJudgeDomainEventAdapter implements JudgeDomainEventPort {
  private final ConnectionFactory factory;

  @Autowired
  RabbitJudgeDomainEventAdapter(Environment environment) {
    this(configured(environment));
  }

  RabbitJudgeDomainEventAdapter(ConnectionFactory factory) {
    this.factory = factory;
  }

  @Override
  public boolean publish(OutboxEvent event) {
    String key =
        switch (event.type()) {
          case "SubmissionGraded" -> "judge.submission.graded";
          case "SubmissionAccepted" -> "judge.submission.accepted";
          default -> "";
        };
    if (key.isEmpty() || Thread.currentThread().isInterrupted()) {
      return false;
    }
    var task = new FutureTask<>(() -> send(event, key));
    Thread.startVirtualThread(task);
    try {
      return task.get(10, TimeUnit.SECONDS);
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

  private boolean send(OutboxEvent event, String key) throws Exception {
    var connection = factory.newConnection();
    try {
      var channel = connection.createChannel();
      channel.exchangeDeclare("algoprep.domain-events", "topic", true);
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
              .messageId(event.id().toString())
              .type(event.type())
              .build();
      channel.basicPublish(
          "algoprep.domain-events",
          key,
          true,
          properties,
          event.envelope().getBytes(StandardCharsets.UTF_8));
      return channel.waitForConfirms(3000) && !returned.get();
    } finally {
      connection.abort(100);
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

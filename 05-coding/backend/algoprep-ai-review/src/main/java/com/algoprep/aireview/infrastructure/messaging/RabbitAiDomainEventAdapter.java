package com.algoprep.aireview.infrastructure.messaging;

import com.algoprep.aireview.application.ports.out.AiDomainEventPort;
import com.algoprep.aireview.domain.model.AiOutboxEvent;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** A dedicated connection per message keeps late confirms and returns off other publishers. */
@Component
class RabbitAiDomainEventAdapter implements AiDomainEventPort {
  private static final String EXCHANGE = "algoprep.domain-events";
  private final ConnectionFactory factory;

  @Autowired
  RabbitAiDomainEventAdapter(Environment environment) {
    this(configured(environment));
  }

  RabbitAiDomainEventAdapter(ConnectionFactory factory) {
    this.factory = factory;
  }

  @Override
  public boolean publish(AiOutboxEvent event, String envelope) {
    if (Thread.currentThread().isInterrupted()) {
      return false;
    }
    var task = new FutureTask<>(() -> send(event, envelope));
    Thread.startVirtualThread(task);
    try {
      return task.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return false;
    } catch (ExecutionException | TimeoutException failure) {
      return false;
    } finally {
      task.cancel(true);
    }
  }

  private boolean send(AiOutboxEvent event, String envelope) throws Exception {
    var connection = factory.newConnection();
    try {
      var channel = connection.createChannel();
      channel.exchangeDeclare(EXCHANGE, "topic", true);
      channel.confirmSelect();
      var returned = new AtomicBoolean();
      channel.addReturnListener(message -> returned.set(true));
      var properties =
          new AMQP.BasicProperties.Builder()
              .contentType("application/json")
              .contentEncoding("UTF-8")
              .deliveryMode(2)
              .messageId(event.id().toString())
              .type(event.eventType())
              .build();
      // mandatory = true: a key with no bound queue is returned, so the row stays pending.
      channel.basicPublish(
          EXCHANGE,
          event.routingKey(),
          true,
          properties,
          envelope.getBytes(StandardCharsets.UTF_8));
      // The publisher confirm waits at most 5 seconds (same bound as JOB-PB-01).
      return channel.waitForConfirms(5000) && !returned.get();
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

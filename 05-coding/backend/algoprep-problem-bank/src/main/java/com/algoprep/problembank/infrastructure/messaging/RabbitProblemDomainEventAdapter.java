package com.algoprep.problembank.infrastructure.messaging;

import com.algoprep.problembank.application.ports.out.ProblemDomainEventPort;
import com.algoprep.problembank.domain.model.OutboxEvent;
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
class RabbitProblemDomainEventAdapter implements ProblemDomainEventPort {
  private static final String EXCHANGE = "algoprep.domain-events";
  private final ConnectionFactory factory;

  @Autowired
  RabbitProblemDomainEventAdapter(Environment environment) {
    this(configured(environment));
  }

  RabbitProblemDomainEventAdapter(ConnectionFactory factory) {
    this.factory = factory;
  }

  @Override
  public boolean publish(OutboxEvent event) {
    if (Thread.currentThread().isInterrupted()) {
      return false;
    }
    var task = new FutureTask<>(() -> send(event));
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

  private boolean send(OutboxEvent event) throws Exception {
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
      channel.basicPublish(
          EXCHANGE,
          event.routingKey(),
          true,
          properties,
          event.envelope().getBytes(StandardCharsets.UTF_8));
      // DD JOB-PB-01: publisher confirm timeout is at most 5 seconds.
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
